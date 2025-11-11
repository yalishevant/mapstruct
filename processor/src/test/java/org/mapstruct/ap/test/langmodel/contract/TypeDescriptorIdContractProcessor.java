/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.mapstruct.ap.langmodel.javax.DefaultVersionInformation;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.descriptor.FieldDescriptor;
import org.mapstruct.ap.langmodel.api.LangElements;
import org.mapstruct.ap.langmodel.LangModelContext;
import org.mapstruct.ap.langmodel.LangModelContextFactory;
import org.mapstruct.ap.langmodel.LangModelElementQuery;
import org.mapstruct.ap.langmodel.LangModelTypeSystem;
import org.mapstruct.ap.langmodel.api.LangTypes;
import org.mapstruct.ap.langmodel.MapperEntryPoint;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.langmodel.javax.JavaxLangModelContextFactory;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class TypeDescriptorIdContractProcessor extends AbstractProcessor {

    private boolean executed;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if ( executed || roundEnv.processingOver() ) {
            return false;
        }

        executed = true;

        List<String> errors = new ArrayList<>();

        try {
            LangModelContextFactory factory = new JavaxLangModelContextFactory();
            VersionInformation versionInformation = resolveVersionInformation();
            TypeElement mapperElement = processingEnv.getElementUtils().getTypeElement(
                "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.MapperStub"
            );
            if ( mapperElement == null ) {
                errors.add( "MapperStub test type not found" );
            }
            else {
                MapperEntryPoint entryPoint = MapperEntryPoint.of(
                    versionInformation,
                    processingEnv,
                    mapperElement
                );
                try ( LangModelContext<?, ?, ?, ?> context = factory.create( entryPoint ) ) {
                    verifyTypeDescriptorIds( context, errors );
                }
            }
        }
        catch ( Exception ex ) {
            errors.add( "Unexpected exception during type descriptor id verification: " + ex );
        }

        if ( !errors.isEmpty() ) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                String.join( System.lineSeparator(), errors )
            );
        }

        return false;
    }

    private void verifyTypeDescriptorIds(LangModelContext<?, ?, ?, ?> context, List<String> errors) {
        LangModelTypeSystem<?, ?, ?, ?> typeSystem = context.typeSystem();
        LangModelElementQuery elementQuery = context.elementQuery();
        LangElements elements = elementQuery.elements();
        LangTypes langTypes = typeSystem.types();

        expectId( "primitive int", langTypes.primitive( "int" ), "primitive:int", errors );
        expectId( "void type", langTypes.voidType(), "void", errors );

        TypeElementDescriptor stringElement = elements.typeElement( "java.lang.String" );
        if ( stringElement == null ) {
            errors.add( "java.lang.String type element not found" );
            return;
        }
        TypeDescriptor stringType = stringElement.asType();
        expectId( "java.lang.String", stringType, "declared:java.lang.String", errors );

        TypeElementDescriptor listElement = elements.typeElement( "java.util.List" );
        if ( listElement == null ) {
            errors.add( "java.util.List type element not found" );
            return;
        }
        TypeDescriptor listOfString = langTypes.declaredType( listElement, List.of( stringType ) );
        expectId(
            "java.util.List<java.lang.String>",
            listOfString,
            "declared:java.util.List<declared:java.lang.String>",
            errors
        );

        verifyWildcardHolderIds( elements, errors );
        verifyBoxTypeIds( elements, errors );
        verifyMethodTypeIds( elements, errors );
    }

    private void verifyWildcardHolderIds(LangElements elements, List<String> errors) {
        TypeElementDescriptor holder = elements.typeElement(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.WildcardHolder"
        );
        if ( holder == null ) {
            errors.add( "WildcardHolder test type not found" );
            return;
        }

        FieldDescriptor sourceField = findField( elements, holder, "source" );
        if ( sourceField != null ) {
            TypeDescriptor sourceType = sourceField.fieldType();
            expectId(
                "List<? extends Number>",
                sourceType,
                "declared:java.util.List<wildcard:extends=declared:java.lang.Number>",
                errors
            );
            if ( !sourceType.typeArguments().isEmpty() ) {
                expectId(
                    "Wildcard extends Number",
                    sourceType.typeArguments().get( 0 ),
                    "wildcard:extends=declared:java.lang.Number",
                    errors
                );
            }
        }
        else {
            errors.add( "Field 'source' not found on WildcardHolder" );
        }

        FieldDescriptor sinkField = findField( elements, holder, "sink" );
        if ( sinkField != null ) {
            TypeDescriptor sinkType = sinkField.fieldType();
            expectId(
                "List<? super Integer>",
                sinkType,
                "declared:java.util.List<wildcard:super=declared:java.lang.Integer>",
                errors
            );
            if ( !sinkType.typeArguments().isEmpty() ) {
                expectId(
                    "Wildcard super Integer",
                    sinkType.typeArguments().get( 0 ),
                    "wildcard:super=declared:java.lang.Integer",
                    errors
                );
            }
        }
        else {
            errors.add( "Field 'sink' not found on WildcardHolder" );
        }

        FieldDescriptor numberArray = findField( elements, holder, "numberArray" );
        if ( numberArray != null ) {
            expectId(
                "Number[]",
                numberArray.fieldType(),
                "array:declared:java.lang.Number",
                errors
            );
        }
        else {
            errors.add( "Field 'numberArray' not found on WildcardHolder" );
        }
    }

    private void verifyBoxTypeIds(LangElements elements, List<String> errors) {
        TypeElementDescriptor box = elements.typeElement(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.Box"
        );
        if ( box == null ) {
            errors.add( "Box<T> test type not found" );
            return;
        }

        TypeDescriptor boxType = box.asType();
        if ( boxType.typeArguments().isEmpty() ) {
            errors.add( "Box<T> expected to expose a type parameter descriptor" );
            return;
        }

        TypeDescriptor typeVariable = boxType.typeArguments().get( 0 );
        String expectedTypeVarId =
            "typevar:type:org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.Box:0:T";
        expectId( "Type parameter T", typeVariable, expectedTypeVarId, errors );

        TypeDescriptor upperBound = typeVariable.typeVariableUpperBound().orElse( null );
        if ( upperBound != null ) {
            String expectedIntersection =
                "intersection:declared:java.lang.Number&declared:java.lang.Comparable<" + expectedTypeVarId + ">";
            expectId( "Intersection bound of T", upperBound, expectedIntersection, errors );
        }
        else {
            errors.add( "Upper bound for type variable T expected to be present" );
        }
    }

    private void verifyMethodTypeIds(LangElements elements, List<String> errors) {
        TypeElementDescriptor methodHolder = elements.typeElement(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.MethodHolder"
        );
        if ( methodHolder == null ) {
            errors.add( "MethodHolder test type not found" );
            return;
        }

        ExecutableDescriptor identity = findExecutable( elements, methodHolder, "identity" );
        if ( identity == null ) {
            errors.add( "Method 'identity' not found on MethodHolder" );
            return;
        }

        if ( identity.typeParameters().isEmpty() ) {
            errors.add( "Method identity expected to expose a type parameter" );
            return;
        }

        TypeDescriptor methodTypeVar = identity.typeParameters().get( 0 );
        String owner =
            "method:org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.MethodHolder#identity(R)";
        String expectedId = "typevar:" + owner + ":0:R";
        expectId( "Method type parameter R", methodTypeVar, expectedId, errors );

        TypeDescriptor methodUpper = methodTypeVar.typeVariableUpperBound().orElse( null );
        if ( methodUpper != null ) {
            String expectedIntersection =
                "intersection:declared:java.lang.Comparable<" + expectedId + ">&declared:java.io.Serializable";
            expectId( "Intersection bound of R", methodUpper, expectedIntersection, errors );
        }
        else {
            errors.add( "Method type parameter R expected to expose an upper bound" );
        }
    }

    private FieldDescriptor findField(LangElements elements, TypeElementDescriptor type, String name) {
        for ( FieldDescriptor field : elements.enclosedFields( type ) ) {
            if ( field.simpleName().content().equals( name ) ) {
                return field;
            }
        }
        return null;
    }

    private ExecutableDescriptor findExecutable(LangElements elements, TypeElementDescriptor type, String name) {
        for ( ExecutableDescriptor executable : elements.enclosedExecutables( type ) ) {
            if ( executable.simpleName().content().equals( name ) ) {
                return executable;
            }
        }
        return null;
    }

    private void expectId(String scenario, TypeDescriptor descriptor, String expected, List<String> errors) {
        if ( descriptor == null ) {
            errors.add( scenario + " descriptor is null" );
            return;
        }
        String actual = descriptor.id();
        if ( !expected.equals( actual ) ) {
            errors.add( scenario + " expected id '" + expected + "' but was '" + actual + "'" );
        }
    }

    private VersionInformation resolveVersionInformation()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method factory = DefaultVersionInformation.class.getDeclaredMethod(
            "fromProcessingEnvironment",
            javax.annotation.processing.ProcessingEnvironment.class
        );
        factory.setAccessible( true );
        return (VersionInformation) factory.invoke( null, processingEnv );
    }
}
