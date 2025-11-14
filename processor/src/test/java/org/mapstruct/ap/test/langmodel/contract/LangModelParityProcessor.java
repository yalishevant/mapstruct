/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ParameterDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.javax.DefaultVersionInformation;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.internal.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.LangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.LangModelElementQuery;
import org.mapstruct.ap.internal.langmodel.MapperEntryPoint;
import org.mapstruct.ap.internal.langmodel.TypeIntrospector;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.javax.JavaxLangModelContextFactory;

/**
 * Processor validating that the thin language model reproduces the behaviour of {@code javax.lang.model} for selected
 * scenarios (overrides, constructors, records, enum metadata).
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class LangModelParityProcessor extends AbstractProcessor {

    private static final String CONTRACT_TYPES_FQN =
        "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes";
    private static final String OVERRIDE_BASE_FQN = CONTRACT_TYPES_FQN + ".OverrideBase";
    private static final String OVERRIDE_CHILD_FQN = CONTRACT_TYPES_FQN + ".OverrideChild";
    private static final String CONSTRUCTOR_FQN = CONTRACT_TYPES_FQN + ".MultipleConstructors";
    private static final String RECORD_FQN = CONTRACT_TYPES_FQN + ".RecordType";
    private static final String ENUM_FQN = CONTRACT_TYPES_FQN + ".SampleEnum";
    private static final String MAPPER_STUB_FQN = CONTRACT_TYPES_FQN + ".MapperStub";

    private boolean executed;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if ( executed || roundEnv.processingOver() ) {
            return false;
        }

        executed = true;

        List<String> errors = new ArrayList<>();
        try {
            Set<String> focus = determineFocus();
            LangModelContextFactory factory = new JavaxLangModelContextFactory();
            VersionInformation versionInformation = resolveVersionInformation( processingEnv );
            TypeElement mapperElement = processingEnv.getElementUtils().getTypeElement( MAPPER_STUB_FQN );
            if ( mapperElement == null ) {
                errors.add( "MapperStub test type not found" );
            }
            else {
                MapperEntryPoint entryPoint = MapperEntryPoint.of( versionInformation, processingEnv, mapperElement );
                try ( LangModelContext context =
                    cast( factory.create( entryPoint ) ) ) {
                    if ( focus.contains( "overrides" ) ) {
                        verifyOverridesParity( factory, context, errors );
                    }
                    if ( focus.contains( "constructors" ) ) {
                        verifyConstructorsParity( factory, context, errors );
                    }
                    if ( focus.contains( "record" ) ) {
                        verifyRecordComponentsParity( factory, context, errors );
                    }
                    if ( focus.contains( "enum" ) ) {
                        verifyEnumMetadataParity( factory, context, errors );
                    }
                }
            }
        }
        catch ( Exception ex ) {
            errors.add( "Unexpected exception during parity verification: " + ex );
        }

        if ( !errors.isEmpty() ) {
            for ( String error : errors ) {
                processingEnv.getMessager().printMessage( Diagnostic.Kind.ERROR, error );
            }
        }

        return false;
    }

    private Set<String> determineFocus() {
        String focusOption = processingEnv.getOptions().get( "mapstruct.test.langModelParity" );
        if ( focusOption == null || focusOption.trim().isEmpty() ) {
            return Set.of( "overrides", "constructors", "record", "enum" );
        }
        String[] tokens = focusOption.split( "," );
        Set<String> focus = new HashSet<>();
        for ( String token : tokens ) {
            String normalized = token.trim().toLowerCase( Locale.ROOT );
            if ( !normalized.isEmpty() ) {
                focus.add( normalized );
            }
        }
        return focus.isEmpty()
            ? Set.of( "overrides", "constructors", "record", "enum" )
            : focus;
    }

    private void verifyOverridesParity(LangModelContextFactory factory,
                                       LangModelContext context,
                                       List<String> errors) {

        LangModelElementQuery elementQuery = context.elementQuery();
        LangElements langElements = elementQuery.elements();
        TypeElementDescriptor baseDescriptor = langElements.typeElement( OVERRIDE_BASE_FQN );
        TypeElementDescriptor childDescriptor = langElements.typeElement( OVERRIDE_CHILD_FQN );

        if ( baseDescriptor == null || childDescriptor == null ) {
            errors.add( "Override fixtures not found: " + OVERRIDE_BASE_FQN + ", " + OVERRIDE_CHILD_FQN );
            return;
        }

        ExecutableDescriptor baseValue = findDeclaredMethod( langElements, baseDescriptor, "value", 1 );
        ExecutableDescriptor childValue = findDeclaredMethod( langElements, childDescriptor, "value", 1 );
        ExecutableDescriptor baseLabel = findDeclaredMethod( langElements, baseDescriptor, "label", 0 );
        ExecutableDescriptor childLabel = findDeclaredMethod( langElements, childDescriptor, "label", 0 );
        ExecutableDescriptor childOnly = findDeclaredMethod( langElements, childDescriptor, "childOnly", 0 );

        if ( baseValue == null || childValue == null || baseLabel == null || childLabel == null || childOnly == null ) {
            errors.add( "Failed to locate override candidate methods on fixture types" );
            return;
        }

        TypeElement childElement = processingEnv.getElementUtils().getTypeElement( OVERRIDE_CHILD_FQN );
        TypeElement baseElement = processingEnv.getElementUtils().getTypeElement( OVERRIDE_BASE_FQN );

        assertOverrideParity( langElements, childDescriptor, childValue, baseValue, childElement, baseElement, errors );
        assertOverrideParity( langElements, childDescriptor, childLabel, baseLabel, childElement, baseElement, errors );
        assertOverrideParity( langElements, childDescriptor, childOnly, baseValue, childElement, baseElement, errors );
    }

    private void verifyConstructorsParity(LangModelContextFactory factory,
                                          LangModelContext context,
                                          List<String> errors) {

        LangModelElementQuery elementQuery = context.elementQuery();
        LangElements langElements = elementQuery.elements();

        TypeElementDescriptor constructorDescriptor = langElements.typeElement( CONSTRUCTOR_FQN );
        if ( constructorDescriptor == null ) {
            errors.add( "Constructor parity fixture not found: " + CONSTRUCTOR_FQN );
            return;
        }

        List<ExecutableDescriptor> langConstructors = langElements.constructors( constructorDescriptor );
        Set<String> langSignatures = langConstructors.stream()
            .map( LangModelParityProcessor::constructorSignature )
            .collect( Collectors.toCollection( HashSet::new ) );

        TypeElement element = processingEnv.getElementUtils().getTypeElement( CONSTRUCTOR_FQN );
        if ( element == null ) {
            errors.add( "Constructor parity element not found: " + CONSTRUCTOR_FQN );
            return;
        }
        Set<String> javaxSignatures = element.getEnclosedElements().stream()
            .filter( e -> e.getKind() == ElementKind.CONSTRUCTOR )
            .map( ExecutableElement.class::cast )
            .map( LangModelParityProcessor::constructorSignature )
            .collect( Collectors.toCollection( HashSet::new ) );

        if ( !langSignatures.equals( javaxSignatures ) ) {
            errors.add( "Constructor parity mismatch for " + CONSTRUCTOR_FQN + ": thin=" + langSignatures
                + ", javax=" + javaxSignatures );
        }
    }

    private void verifyRecordComponentsParity(LangModelContextFactory factory,
                                              LangModelContext context,
                                              List<String> errors) {

        LangModelElementQuery elementQuery = context.elementQuery();
        LangElements langElements = elementQuery.elements();

        TypeElementDescriptor recordDescriptor = langElements.typeElement( RECORD_FQN );
        if ( recordDescriptor == null ) {
            errors.add( "Record parity fixture not found: " + RECORD_FQN );
            return;
        }

        Set<String> langComponents = langElements.recordComponents( recordDescriptor ).stream()
            .map( component -> component.simpleName().content() )
            .collect( Collectors.toCollection( HashSet::new ) );

        TypeElement recordElement = processingEnv.getElementUtils().getTypeElement( RECORD_FQN );
        if ( recordElement == null ) {
            errors.add( "Record parity element not found: " + RECORD_FQN );
            return;
        }
        Set<String> javaxComponents = recordElement.getRecordComponents().stream()
            .map( component -> component.getSimpleName().toString() )
            .collect( Collectors.toCollection( HashSet::new ) );

        if ( !langComponents.equals( javaxComponents ) ) {
            errors.add( "Record component parity mismatch for " + RECORD_FQN + ": thin=" + langComponents
                + ", javax=" + javaxComponents );
        }
    }

    private void verifyEnumMetadataParity(LangModelContextFactory factory,
                                          LangModelContext context,
                                          List<String> errors) {

        LangDescriptorFactory descriptors = context.descriptors();
        TypeIntrospector typeIntrospector = context.typeIntrospector();

        TypeElement enumElement = processingEnv.getElementUtils().getTypeElement( ENUM_FQN );
        if ( enumElement == null ) {
            errors.add( "Enum parity element not found: " + ENUM_FQN );
            return;
        }

        TypeDescriptor enumDescriptor = descriptors.typeDescriptor( enumElement.asType() );
        TypeIntrospector.Metadata metadata = typeIntrospector.describe( enumDescriptor );

        if ( !metadata.isEnumType() ) {
            errors.add( "TypeIntrospector metadata did not mark " + ENUM_FQN + " as enum type" );
        }

        List<String> langConstants = metadata.enumConstants();
        List<String> javaxConstants = enumElement.getEnclosedElements().stream()
            .filter( e -> e.getKind() == ElementKind.ENUM_CONSTANT )
            .map( Element::getSimpleName )
            .map( Name::toString )
            .collect( Collectors.toList() );

        if ( !langConstants.equals( javaxConstants ) ) {
            errors.add( "Enum constant parity mismatch for " + ENUM_FQN + ": thin=" + langConstants
                + ", javax=" + javaxConstants );
        }
    }

    private void assertOverrideParity(LangElements langElements,
                                      TypeElementDescriptor childDescriptor,
                                      ExecutableDescriptor childMethod,
                                      ExecutableDescriptor baseMethod,
                                      TypeElement childElement,
                                      TypeElement baseElement,
                                      List<String> errors) {

        boolean langOverrides = langElements.overrides( childMethod, baseMethod, childDescriptor );
        boolean javaxOverrides = processingEnv.getElementUtils().overrides(
            (ExecutableElement) childMethod.unwrap(),
            (ExecutableElement) baseMethod.unwrap(),
            childElement
        );

        if ( langOverrides != javaxOverrides ) {
            errors.add( "Override parity mismatch for "
                + childDescriptor.simpleName().content() + "#" + childMethod.simpleName().content()
                + " vs " + baseElement.getQualifiedName() + "#" + baseMethod.simpleName().content()
                + ": thin=" + langOverrides + ", javax=" + javaxOverrides );
        }
    }

    private static ExecutableDescriptor findDeclaredMethod(LangElements langElements,
                                                           TypeElementDescriptor owner,
                                                           String name,
                                                           int parameterCount) {
        return langElements.enclosedExecutables( owner ).stream()
            .filter( executable -> name.equals( executable.simpleName().content() ) )
            .filter( executable -> executable.parameters().size() == parameterCount )
            .filter( executable ->
                executable.enclosingElement()
                    .map( enclosing -> enclosing.id().equals( owner.id() ) )
                    .orElse( false )
            )
            .findFirst()
            .orElse( null );
    }

    private static String constructorSignature(ExecutableDescriptor descriptor) {
        return descriptor.parameters().stream()
            .map( ParameterDescriptor::type )
            .map( TypeDescriptor::displayName )
            .collect( Collectors.joining( ",", descriptor.simpleName().content() + "(", ")" ) );
    }

    private static String constructorSignature(ExecutableElement element) {
        return element.getParameters().stream()
            .map( variable -> variable.asType().toString() )
            .collect( Collectors.joining( ",", element.getSimpleName().toString() + "(", ")" ) );
    }

    @SuppressWarnings("unchecked")
    private LangModelContext cast(
        LangModelContext context) {
        return (LangModelContext) context;
    }

    private static VersionInformation resolveVersionInformation(ProcessingEnvironment processingEnvironment)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method factory = DefaultVersionInformation.class.getDeclaredMethod(
            "fromProcessingEnvironment",
            ProcessingEnvironment.class
        );
        factory.setAccessible( true );
        return (VersionInformation) factory.invoke( null, processingEnvironment );
    }
}
