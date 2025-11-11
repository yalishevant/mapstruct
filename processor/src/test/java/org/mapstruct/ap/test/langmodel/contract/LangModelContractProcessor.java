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
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.mapstruct.ap.langmodel.javax.DefaultVersionInformation;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.langmodel.AccessorNamingAdapter;
import org.mapstruct.ap.langmodel.AccessorNamingAdapterFactory;
import org.mapstruct.ap.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.descriptor.ElementDescriptor;
import org.mapstruct.ap.langmodel.api.EnumMappingSupport;
import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.langmodel.ExecutableSignature;
import org.mapstruct.ap.descriptor.FieldDescriptor;
import org.mapstruct.ap.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.spi.lang.EnumMappingCapability;
import org.mapstruct.ap.langmodel.api.LangElements;
import org.mapstruct.ap.langmodel.LangModelContext;
import org.mapstruct.ap.langmodel.LangModelContextFactory;
import org.mapstruct.ap.langmodel.LangModelElementQuery;
import org.mapstruct.ap.langmodel.LangModelTypeSystem;
import org.mapstruct.ap.descriptor.LangTypeKind;
import org.mapstruct.ap.langmodel.api.LangTypes;
import org.mapstruct.ap.spi.lang.MappingExclusionCapability;
import org.mapstruct.ap.langmodel.api.MappingExclusionSupport;
import org.mapstruct.ap.langmodel.OptionalCapability;
import org.mapstruct.ap.langmodel.MapperEntryPoint;
import org.mapstruct.ap.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.langmodel.TypeIntrospector;
import org.mapstruct.ap.langmodel.TypeIntrospector.Metadata;
import org.mapstruct.ap.langmodel.javax.JavaxLangModelContextFactory;
import org.mapstruct.ap.spi.AccessorNamingStrategy;
import org.mapstruct.ap.spi.EnumMappingStrategy;
import org.mapstruct.ap.spi.MapStructProcessingEnvironment;
import org.mapstruct.ap.spi.MappingExclusionProvider;
import org.mapstruct.ap.spi.MethodType;

/**
 * Processor performing contract checks against the descriptor-first language model abstractions.
 * It emits a compilation error when an invariant is violated so that the corresponding unit test fails.
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class LangModelContractProcessor extends AbstractProcessor {

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
                    verifyDescriptorContract( context, factory, errors );
                }
            }
        }
        catch ( Exception ex ) {
            errors.add( "Unexpected exception during descriptor contract verification: " + ex );
        }

        if ( !errors.isEmpty() ) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                String.join( System.lineSeparator(), errors )
            );
        }

        return false;
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

    private void verifyDescriptorContract(LangModelContext<?, ?, ?, ?> context,
                                          LangModelContextFactory factory,
                                          List<String> errors) {
        LangModelTypeSystem<?, ?, ?, ?> typeSystem = context.typeSystem();
        LangModelElementQuery elementQuery = context.elementQuery();
        @SuppressWarnings("unchecked")
        LangDescriptorFactory descriptors = (LangDescriptorFactory) typeSystem.descriptors();
        LangElements langElements = elementQuery.elements();
        LangTypes langTypes = typeSystem.types();
        TypeIntrospector typeIntrospector = typeSystem.typeIntrospector();

        ElementsLookup lookup = new ElementsLookup( processingEnv.getElementUtils(), descriptors, errors );

        runCheck( "Type variable bounds", () -> verifyTypeVariableBounds( lookup, langTypes, errors ), errors );
        runCheck( "Wildcard descriptors", () -> verifyWildcardDescriptors( lookup, langElements, errors ), errors );
        runCheck( "Record components", () -> verifyRecordComponents( lookup, langElements, errors ), errors );
        runCheck( "Type introspector metadata",
            () -> verifyTypeIntrospectorMetadata( lookup, typeIntrospector, errors ), errors );
        runCheck(
            "Stream return type",
            () -> verifyStreamReturnType( lookup, langElements, langTypes, errors ),
            errors
        );
        runCheck(
            "`asMemberOf` resolution",
            () -> verifyAsMemberOf( lookup, langTypes, typeIntrospector, errors ),
            errors
        );
        runCheck(
            "Accessor naming adapter",
            () -> verifyAccessorNamingAdapter( lookup, factory, context, errors ),
            errors
        );
        runCheck( "Enum mapping support", () -> verifyEnumMappingSupport( lookup, context, errors ), errors );
        runCheck(
            "Mapping exclusion support",
            () -> verifyMappingExclusionSupport( lookup, context, errors ),
            errors
        );
    }

    private void verifyTypeVariableBounds(ElementsLookup lookup,
                                          LangTypes langTypes,
                                          List<String> errors) {
        TypeElementDescriptor box = lookup.typeElement(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.Box"
        );
        if ( box == null ) {
            return;
        }

        TypeDescriptor boxType;
        try {
            boxType = box.asType();
        }
        catch ( RuntimeException ex ) {
            errors.add( "Failed to acquire descriptor for Box<T>: " + ex.getMessage() );
            return;
        }
        if ( boxType.typeArguments().isEmpty() ) {
            errors.add( "Box<T> expected to expose one type argument descriptor" );
            return;
        }

        TypeDescriptor typeVariable = boxType.typeArguments().get( 0 );
        expect( typeVariable.kind() == LangTypeKind.TYPE_PARAMETER,
            "Box<T> type argument expected to be TYPE_PARAMETER", errors );

        TypeDescriptor upperBound;
        try {
            upperBound = typeVariable.typeVariableUpperBound().orElse( null );
        }
        catch ( RuntimeException ex ) {
            errors.add( "Failed to resolve type variable upper bound: " + ex.getMessage() );
            return;
        }
        expect( upperBound != null, "Type variable bound expected to be present", errors );
        if ( upperBound == null ) {
            return;
        }

        expect( upperBound.kind() == LangTypeKind.INTERSECTION,
            "Upper bound should be reported as INTERSECTION type", errors );

        List<TypeDescriptor> intersectionBounds;
        try {
            intersectionBounds = upperBound.typeVariableBounds();
        }
        catch ( RuntimeException ex ) {
            errors.add( "Failed to expand intersection bounds: " + ex.getMessage() );
            return;
        }
        expect( intersectionBounds.size() == 2,
            "Intersection bound should expose two descriptors (Number & Comparable)", errors );
        if ( intersectionBounds.size() == 2 ) {
            expectQualifiedName( intersectionBounds.get( 0 ), "java.lang.Number",
                "First intersection bound should be java.lang.Number", errors );
            expectQualifiedName( intersectionBounds.get( 1 ), "java.lang.Comparable",
                "Second intersection bound should be java.lang.Comparable", errors );
        }

        List<TypeDescriptor> arguments = new ArrayList<>();
        arguments.add( lookup.typeDescriptor( "java.lang.Integer" ) );
        TypeDescriptor boxWithInteger;
        try {
            boxWithInteger = langTypes.declaredType( box, arguments );
        }
        catch ( RuntimeException ex ) {
            errors.add( "Failed to create declared type for Box<Integer>: " + ex.getMessage() );
            return;
        }

        FieldDescriptor valueField = lookup.field( box, "value" );
        if ( valueField != null ) {
            TypeDescriptor resolvedField;
            try {
                resolvedField = langTypes.asMemberOf( boxWithInteger, valueField );
            }
            catch ( RuntimeException ex ) {
                errors.add( "`asMemberOf` failed for Box<Integer>.value: " + ex.getMessage() );
                return;
            }
            expectQualifiedName( resolvedField, "java.lang.Integer",
                "`asMemberOf` should resolve field type to the supplied type argument", errors );
        }
    }

    private void verifyWildcardDescriptors(ElementsLookup lookup,
                                           LangElements langElements,
                                           List<String> errors) {
        TypeElementDescriptor holder = lookup.typeElement(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.WildcardHolder"
        );
        if ( holder == null ) {
            return;
        }

        FieldDescriptor source = lookup.field( holder, "source" );
        if ( source != null ) {
            TypeDescriptor argument = firstTypeArgument( source.fieldType() );
            if ( argument != null ) {
                expect( argument.kind() == LangTypeKind.WILDCARD,
                    "List<? extends Number> should expose a WILDCARD type argument", errors );
                expectQualifiedName(
                    argument.wildcardExtendsBound().orElse( null ),
                    "java.lang.Number",
                    "Wildcard extends bound expected to resolve to java.lang.Number",
                    errors
                );
            }
        }

        FieldDescriptor sink = lookup.field( holder, "sink" );
        if ( sink != null ) {
            TypeDescriptor argument = firstTypeArgument( sink.fieldType() );
            if ( argument != null ) {
                expect( argument.kind() == LangTypeKind.WILDCARD,
                    "List<? super Integer> should expose a WILDCARD type argument", errors );
                expectQualifiedName(
                    argument.wildcardSuperBound().orElse( null ),
                    "java.lang.Integer",
                    "Wildcard super bound expected to resolve to java.lang.Integer",
                    errors
                );
            }
        }

        FieldDescriptor arrayField = lookup.field( holder, "numberArray" );
        if ( arrayField != null ) {
            TypeDescriptor component = arrayField.fieldType().componentType().orElse( null );
            expectQualifiedName( component, "java.lang.Number",
                "Number[] component type expected to resolve to java.lang.Number", errors );
        }

        // Verify that Object methods are filtered from enclosed executables
        List<ExecutableDescriptor> executables = langElements.enclosedExecutables( holder );
        long objectMethods = executables.stream()
            .map( executable -> executable.simpleName().content() )
            .filter( name -> "wait".equals( name ) || "notify".equals( name ) )
            .count();
        expect( objectMethods == 0,
            "enclosedExecutables should filter Object.class methods", errors );
    }

    private void verifyRecordComponents(ElementsLookup lookup,
                                        LangElements langElements,
                                        List<String> errors) {
        TypeElementDescriptor record = lookup.typeElement(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.RecordType"
        );
        if ( record == null ) {
            return;
        }

        List<RecordComponentDescriptor> components = langElements.recordComponents( record );
        expect( components.size() == 2, "RecordType should expose two record components", errors );
        if ( components.size() == 2 ) {
            expectSimpleName( components.get( 0 ), "name", "First record component should be name", errors );
            expectSimpleName( components.get( 1 ), "age", "Second record component should be age", errors );
        }
    }

    private void verifyTypeIntrospectorMetadata(ElementsLookup lookup,
                                                TypeIntrospector typeIntrospector,
                                                List<String> errors) {
        TypeDescriptor recordType = lookup.typeDescriptor(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.RecordType"
        );
        if ( recordType != null ) {
            Metadata metadata = typeIntrospector.describe( recordType );
            expect( metadata.isRecordType(), "RecordType metadata should flag recordType", errors );
            expect( !metadata.isEnumType(), "RecordType metadata should not flag enumType", errors );
            expect( metadata.componentType().isEmpty(), "RecordType metadata should not expose componentType", errors );
            expectSimpleName( metadata.typeElement().orElse( null ), "RecordType",
                "RecordType metadata should expose simple name", errors );
            expectQualifiedName( metadata.topLevelType().orElse( null ),
                "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes",
                "Top level type should resolve to enclosing class", errors );
        }

        TypeDescriptor shapeType = lookup.typeDescriptor(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.Shape"
        );
        if ( shapeType != null ) {
            Metadata metadata = typeIntrospector.describe( shapeType );
            expect( metadata.isSealedType(), "Shape metadata should flag sealedType", errors );
            List<TypeDescriptor> permitted = metadata.permittedSubclasses();
            expect( permitted.size() == 2, "Shape should have two permitted subclasses", errors );
            if ( permitted.size() == 2 ) {
                expectQualifiedName( permitted.get( 0 ),
                    "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.Circle",
                    "First permitted subclass should be Circle", errors );
                expectQualifiedName( permitted.get( 1 ),
                    "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.Rectangle",
                    "Second permitted subclass should be Rectangle", errors );
            }
        }
    }

    private void verifyStreamReturnType(ElementsLookup lookup,
                                        LangElements langElements,
                                        LangTypes langTypes,
                                        List<String> errors) {
        TypeElementDescriptor source = lookup.typeElement(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.StreamSource"
        );
        if ( source == null ) {
            return;
        }

        ExecutableDescriptor values = lookup.method( source, "values" );
        if ( values != null ) {
            TypeElementDescriptor streamElement = langElements.typeElement( "java.util.stream.Stream" );
            TypeDescriptor streamDescriptor = streamElement != null
                ? langTypes.erasure( streamElement.asType() )
                : null;
            boolean recognised = streamDescriptor != null
                && langTypes.isSubtypeErased( values.returnType(), streamDescriptor );
            expect( recognised, "values() return type should be recognised as Stream", errors );
        }
    }

    private void verifyAsMemberOf(ElementsLookup lookup,
                                  LangTypes langTypes,
                                  TypeIntrospector typeIntrospector,
                                  List<String> errors) {
        TypeElementDescriptor box = lookup.typeElement(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.Box"
        );
        if ( box == null ) {
            return;
        }

        ExecutableDescriptor getter = lookup.method( box, "getValue" );
        if ( getter == null ) {
            return;
        }

        List<TypeDescriptor> typeArguments = new ArrayList<>();
        typeArguments.add( lookup.typeDescriptor( "java.lang.Integer" ) );
        TypeDescriptor boxWithInteger = langTypes.declaredType( box, typeArguments );

        ExecutableSignature signature = typeIntrospector.resolveExecutable( boxWithInteger, getter );
        expect( signature != null, "resolveExecutable should provide signature for getValue()", errors );
        if ( signature != null ) {
            expectQualifiedName( signature.returnType(), "java.lang.Integer",
                "Resolved signature should expose Integer return type", errors );
        }
    }

    private void verifyAccessorNamingAdapter(ElementsLookup lookup,
                                             LangModelContextFactory factory,
                                             LangModelContext<?, ?, ?, ?> context,
                                             List<String> errors) {
        TypeElementDescriptor box = lookup.typeElement(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.Box"
        );
        TypeElementDescriptor adderHolder = lookup.typeElement(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.AdderHolder"
        );
        if ( box == null || adderHolder == null ) {
            return;
        }

        ExecutableDescriptor getter = lookup.method( box, "getValue" );
        ExecutableDescriptor adder = lookup.method( adderHolder, "addItem" );
        if ( getter == null || adder == null ) {
            return;
        }

        CapturingAccessorNamingStrategy strategy = new CapturingAccessorNamingStrategy();
        DescriptorUnwrapper unwrapper = factory.descriptorUnwrapper();
        AccessorNamingAdapterFactory adapterFactory = factory.accessorNamingAdapterFactory();
        AccessorNamingAdapter adapter = adapterFactory.create( strategy, unwrapper, context );

        expect( adapter.methodType( getter ) == MethodType.GETTER,
            "Adapter should delegate methodType to AccessorNamingStrategy", errors );
        expect( strategy.lastMethodType != null
                && strategy.lastMethodType.id().equals( getter.id() ),
            "Adapter must forward underlying descriptor for getter", errors );

        expect( "value".equals( adapter.propertyName( getter ) ),
            "Adapter should return property name resolved by strategy", errors );
        expect( strategy.lastPropertyName != null
                && strategy.lastPropertyName.id().equals( getter.id() ),
            "Adapter must forward descriptor for propertyName", errors );

        expect( "item".equalsIgnoreCase( adapter.elementName( adder ) ),
            "Adapter should resolve element name for adder method", errors );
        expect( strategy.lastElementName != null
                && strategy.lastElementName.id().equals( adder.id() ),
            "Adapter must forward descriptor for elementName", errors );
    }

    private void verifyEnumMappingSupport(ElementsLookup lookup,
                                          LangModelContext<?, ?, ?, ?> context,
                                          List<String> errors) {
        TypeDescriptor enumDescriptor = lookup.typeDescriptor(
            "org.mapstruct.ap.test.langmodel.contract.LangModelContractTypes.SampleEnum"
        );
        if ( enumDescriptor == null ) {
            return;
        }

        CapturingEnumMappingStrategy strategy = new CapturingEnumMappingStrategy();
        OptionalCapability<EnumMappingCapability> capability = context.optional( EnumMappingCapability.class );
        EnumMappingCapability enumCapability = capability.orElse( null );
        if ( enumCapability == null ) {
            errors.add( "Enum mapping capability is required" );
            return;
        }
        EnumMappingSupport support = enumCapability.enumMappingSupport( strategy );

        String nullConstant = support.defaultNullEnumConstant( enumDescriptor );
        expect( "DEFAULT_NULL".equals( nullConstant ),
            "EnumMappingSupport should delegate defaultNullEnumConstant to strategy", errors );
        expect( strategy.lastNullRequest != null
                && strategy.lastNullRequest.id().equals( enumDescriptor.id() ),
            "EnumMappingStrategy should receive original descriptor for defaultNullEnumConstant", errors );

        String mappedConstant = support.enumConstant( enumDescriptor, "FIRST" );
        expect( "FIRST_MAPPED".equals( mappedConstant ),
            "EnumMappingSupport should delegate enumConstant mapping", errors );
        expect( strategy.lastEnumConstantRequest != null
                && strategy.lastEnumConstantRequest.id().equals( enumDescriptor.id() ),
            "EnumMappingStrategy should receive original descriptor for enumConstant", errors );

        TypeDescriptor unexpected = support.unexpectedValueMappingExceptionType();
        expectQualifiedName( unexpected, "java.lang.IllegalStateException",
            "UnexpectedValueMappingException type should be provided by strategy", errors );
    }

    private void verifyMappingExclusionSupport(ElementsLookup lookup,
                                               LangModelContext<?, ?, ?, ?> context,
                                               List<String> errors) {
        TypeDescriptor stringDescriptor = lookup.typeDescriptor( "java.lang.String" );
        TypeDescriptor listDescriptor = lookup.typeDescriptor( "java.util.List" );
        if ( stringDescriptor == null || listDescriptor == null ) {
            return;
        }

        CapturingMappingExclusionProvider provider = new CapturingMappingExclusionProvider();
        OptionalCapability<MappingExclusionCapability> capability =
            context.optional( MappingExclusionCapability.class );
        MappingExclusionCapability exclusionCapability = capability.orElse( null );
        if ( exclusionCapability == null ) {
            errors.add( "Mapping exclusion capability is required" );
            return;
        }
        MappingExclusionSupport support = exclusionCapability.mappingExclusionSupport( provider );

        expect( support.isExcluded( stringDescriptor ),
            "DefaultMappingExclusionProvider should exclude java.lang.String", errors );
        expect( provider.lastChecked != null
                && provider.lastChecked.qualifiedName().orElse( "" ).equals( "java.lang.String" ),
            "MappingExclusionProvider should receive original descriptor", errors );

        expect( !support.isExcluded( listDescriptor ),
            "DefaultMappingExclusionProvider should not exclude java.util.List", errors );
    }

    private void runCheck(String label, Runnable task, List<String> errors) {
        try {
            task.run();
        }
        catch ( RuntimeException ex ) {
            errors.add( label + " failed: " + ex.getMessage() );
        }
    }

    private TypeDescriptor firstTypeArgument(TypeDescriptor descriptor) {
        List<TypeDescriptor> arguments = descriptor.typeArguments();
        return arguments.isEmpty() ? null : arguments.get( 0 );
    }

    private void expect(boolean condition, String message, List<String> errors) {
        if ( !condition ) {
            errors.add( message );
        }
    }

    private void expectQualifiedName(TypeDescriptor descriptor, String expected, String message,
                                     List<String> errors) {
        if ( descriptor == null ) {
            errors.add( message + " (descriptor was null)" );
            return;
        }
        String actual = descriptor.qualifiedName().orElse( null );
        if ( !Objects.equals( actual, expected ) ) {
            errors.add( message + " (expected " + expected + " but was " + actual + ")" );
        }
    }

    private void expectSimpleName(ElementDescriptor descriptor, String expected, String message,
                                  List<String> errors) {
        if ( descriptor == null ) {
            errors.add( message + " (descriptor was null)" );
            return;
        }
        String actual = descriptor.simpleName().content();
        if ( !Objects.equals( actual, expected ) ) {
            errors.add( message + " (expected " + expected + " but was " + actual + ")" );
        }
    }

    private static final class ElementsLookup {

        private final javax.lang.model.util.Elements elements;
        private final LangDescriptorFactory descriptors;
        private final List<String> errors;

        private ElementsLookup(javax.lang.model.util.Elements elements,
                               LangDescriptorFactory descriptors,
                               List<String> errors) {
            this.elements = elements;
            this.descriptors = descriptors;
            this.errors = errors;
        }

        TypeElementDescriptor typeElement(String binaryName) {
            TypeElement element = elements.getTypeElement( binaryName );
            if ( element == null ) {
                errors.add( "Failed to resolve type element " + binaryName );
                return null;
            }
            ElementDescriptor descriptor = descriptors.elementDescriptor( element );
            if ( !( descriptor instanceof TypeElementDescriptor ) ) {
                errors.add( "Descriptor for " + binaryName + " expected to be TypeElementDescriptor" );
                return null;
            }
            return (TypeElementDescriptor) descriptor;
        }

        TypeDescriptor typeDescriptor(String binaryName) {
            TypeElement element = elements.getTypeElement( binaryName );
            if ( element == null ) {
                errors.add( "Failed to resolve type descriptor for " + binaryName );
                return null;
            }
            return descriptors.typeDescriptor( element.asType() );
        }

        FieldDescriptor field(TypeElementDescriptor owner, String name) {
            Object unwrapped = owner.unwrap();
            if ( !( unwrapped instanceof TypeElement ) ) {
                errors.add( "Descriptor " + owner + " does not unwrap to TypeElement" );
                return null;
            }
            for ( Element enclosed : ( (TypeElement) unwrapped ).getEnclosedElements() ) {
                if ( enclosed.getKind() == ElementKind.FIELD && enclosed.getSimpleName().contentEquals( name ) ) {
                    ElementDescriptor descriptor = descriptors.elementDescriptor( enclosed );
                    if ( descriptor instanceof FieldDescriptor ) {
                        return (FieldDescriptor) descriptor;
                    }
                }
            }
            errors.add( "Failed to resolve field " + name + " on " + owner.qualifiedName() );
            return null;
        }

        ExecutableDescriptor method(TypeElementDescriptor owner, String name) {
            Object unwrapped = owner.unwrap();
            if ( !( unwrapped instanceof TypeElement ) ) {
                errors.add( "Descriptor " + owner + " does not unwrap to TypeElement" );
                return null;
            }
            for ( Element enclosed : ( (TypeElement) unwrapped ).getEnclosedElements() ) {
                if ( enclosed.getKind() == ElementKind.METHOD && enclosed.getSimpleName().contentEquals( name ) ) {
                    ElementDescriptor descriptor = descriptors.elementDescriptor( enclosed );
                    if ( descriptor instanceof ExecutableDescriptor ) {
                        return (ExecutableDescriptor) descriptor;
                    }
                }
            }
            errors.add( "Failed to resolve method " + name + " on " + owner.qualifiedName() );
            return null;
        }
    }

    private static final class CapturingAccessorNamingStrategy implements AccessorNamingStrategy {

        private ExecutableDescriptor lastMethodType;
        private ExecutableDescriptor lastPropertyName;
        private ExecutableDescriptor lastElementName;

        @Override
        public MethodType getMethodType(ExecutableDescriptor method) {
            lastMethodType = method;
            String name = method.simpleName().content();
            if ( "getValue".equals( name ) ) {
                return MethodType.GETTER;
            }
            if ( "addItem".equals( name ) ) {
                return MethodType.ADDER;
            }
            return MethodType.OTHER;
        }

        @Override
        public String getPropertyName(ExecutableDescriptor getterOrSetterMethod) {
            lastPropertyName = getterOrSetterMethod;
            String name = getterOrSetterMethod.simpleName().content();
            if ( name.startsWith( "get" ) && name.length() > 3 ) {
                String base = name.substring( 3 );
                return Character.toLowerCase( base.charAt( 0 ) ) + base.substring( 1 );
            }
            return name;
        }

        @Override
        public String getElementName(ExecutableDescriptor adderMethod) {
            lastElementName = adderMethod;
            return "item";
        }

        @Override
        @Deprecated
        public String getCollectionGetterName(String property) {
            return "get" + property.substring( 0, 1 ).toUpperCase( Locale.ROOT ) + property.substring( 1 );
        }
    }

    private static final class CapturingEnumMappingStrategy implements EnumMappingStrategy {

        private LangElements elements;
        private TypeDescriptor lastNullRequest;
        private TypeDescriptor lastEnumConstantRequest;

        @Override
        public void init(MapStructProcessingEnvironment processingEnvironment) {
            EnumMappingStrategy.super.init( processingEnvironment );
            this.elements = processingEnvironment.elements();
        }

        @Override
        public String getDefaultNullEnumConstant(TypeDescriptor enumType) {
            lastNullRequest = enumType;
            return "DEFAULT_NULL";
        }

        @Override
        public String getEnumConstant(TypeDescriptor enumType, String enumConstant) {
            lastEnumConstantRequest = enumType;
            return enumConstant + "_MAPPED";
        }

        @Override
        public TypeDescriptor getUnexpectedValueMappingExceptionType() {
            return elements.typeElement( "java.lang.IllegalStateException" ).asType();
        }
    }

    private static final class CapturingMappingExclusionProvider implements MappingExclusionProvider {

        private TypeDescriptor lastChecked;

        @Override
        public boolean isExcluded(TypeDescriptor type) {
            lastChecked = type;
            if ( type == null ) {
                return false;
            }
            String name = type.qualifiedName().orElse( type.displayName() );
            return "java.lang.String".equals( name );
        }
    }

}
