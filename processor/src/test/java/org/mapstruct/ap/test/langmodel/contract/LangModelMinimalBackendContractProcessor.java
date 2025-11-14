/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.mapstruct.ap.internal.model.MappingBuilderContext;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.processor.ModelElementProcessor;
import org.mapstruct.ap.internal.util.AnnotationProcessorContextView;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.util.RoundContext;
import org.mapstruct.ap.internal.langmodel.javax.DefaultVersionInformation;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.internal.langmodel.AnnotationGemsCapability;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.spi.EnumMappingCapability;
import org.mapstruct.ap.internal.langmodel.spi.MappingExclusionCapability;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.LangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.internal.langmodel.MapperEntryPoint;
import org.mapstruct.ap.internal.langmodel.OptionalCapability;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.javax.JavaxLangModelContextFactory;
import org.mapstruct.ap.spi.EnumMappingStrategy;
import org.mapstruct.ap.spi.EnumTransformationStrategy;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;
import org.mapstruct.ap.internal.langmodel.TypeIntrospector;
import org.mapstruct.ap.internal.langmodel.MapperAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperConfigAnnotation;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.api.LangTypes;
import org.mapstruct.ap.internal.langmodel.spi.LangDiagnostics;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class LangModelMinimalBackendContractProcessor extends AbstractProcessor {

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
                try ( LangModelContext delegate =
                    cast( factory.create( entryPoint ) ) ) {
                    verifyMissingCapabilities( delegate, versionInformation, errors );
                }
            }
        }
        catch ( Exception ex ) {
            errors.add( "Unexpected exception during minimal backend verification: " + ex );
        }

        if ( !errors.isEmpty() ) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                String.join( System.lineSeparator(), errors )
            );
        }

        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @SuppressWarnings("unchecked")
    private LangModelContext cast(
        LangModelContext context) {
        return (LangModelContext) context;
    }

    private void verifyMissingCapabilities(
        LangModelContext delegate,
        VersionInformation versionInformation,
        List<String> errors) {

        verifyAnnotationGemsCapability( delegate, versionInformation, errors );
        verifyBuilderCapability( delegate, versionInformation, errors );
        verifyEnumMappingCapability( delegate, versionInformation, errors );
    }

    private void runScenario(String scenario, Runnable action, List<String> errors) {
        try {
            action.run();
        }
        catch ( Exception ex ) {
            errors.add( scenario + " threw unexpected exception: " + ex );
        }
    }

    private void verifyAnnotationGemsCapability(
        LangModelContext delegate,
        VersionInformation versionInformation,
        List<String> errors) {

        LangModelContext context =
            override( delegate, AnnotationGemsCapability.class, OptionalCapability.empty() );

        TypeFactory fallbackTypeFactory = new TypeFactory(
            override(
                context,
                org.mapstruct.ap.internal.langmodel.BuilderIntrospectorCapability.class,
                OptionalCapability.empty()
            ),
            new NoopFormattingMessager(),
            new RoundContext( new StubAnnotationProcessorContext() ),
            Collections.emptyMap(),
            false,
            versionInformation
        );

        runScenario(
            "Annotation gems capability fallback",
            () -> fallbackTypeFactory.annotationGems(),
            errors
        );
    }

    private void verifyBuilderCapability(
        LangModelContext delegate,
        VersionInformation versionInformation,
        List<String> errors) {

        LangModelContext context =
            override(
                delegate,
                org.mapstruct.ap.internal.langmodel.BuilderIntrospectorCapability.class,
                OptionalCapability.empty()
            );

        runScenario(
            "Builder introspector capability fallback",
            () -> new TypeFactory(
                context,
                new NoopFormattingMessager(),
                new RoundContext( new StubAnnotationProcessorContext() ),
                Collections.emptyMap(),
                false,
                versionInformation
            ),
            errors
        );
    }

    private void verifyEnumMappingCapability(
        LangModelContext delegate,
        VersionInformation versionInformation,
        List<String> errors) {

        LangModelContext context =
            override(
                override( delegate, EnumMappingCapability.class, OptionalCapability.empty() ),
                MappingExclusionCapability.class,
                OptionalCapability.empty()
            );

        runScenario(
            "Enum mapping and exclusion capability fallback",
            () -> new MappingBuilderContext(
                null,
                context,
                new NoopFormattingMessager(),
                null,
                null,
                Collections.emptyMap(),
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList()
            ),
            errors
        );
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

    private LangModelContext override(
        LangModelContext delegate,
        Class<?> capabilityType,
        OptionalCapability<?> capability) {

        Map<Class<?>, OptionalCapability<?>> overrides = new HashMap<>();
        LangModelContext base = delegate;
        if ( delegate instanceof CapabilityOverridingContext ) {
            CapabilityOverridingContext existing = (CapabilityOverridingContext) delegate;
            overrides.putAll( existing.overrides );
            base = existing.delegate;
        }
        overrides.put( capabilityType, capability );
        return new CapabilityOverridingContext( base, overrides );
    }

    private static final class CapabilityOverridingContext
        implements LangModelContext {

        private final LangModelContext delegate;
        private final Map<Class<?>, OptionalCapability<?>> overrides;

        private CapabilityOverridingContext(
            LangModelContext delegate,
            Map<Class<?>, OptionalCapability<?>> overrides) {
            this.delegate = delegate;
            this.overrides = overrides;
        }

        @Override
        public LangDescriptorFactory descriptors() {
            return delegate.descriptors();
        }

        @Override
        public LangTypes types() {
            return delegate.types();
        }

        @Override
        public TypeIntrospector typeIntrospector() {
            return delegate.typeIntrospector();
        }

        @Override
        public LangElements elements() {
            return delegate.elements();
        }

        @Override
        public MapperAnnotation mapperAnnotation(TypeElementDescriptor element) {
            return delegate.mapperAnnotation( element );
        }

        @Override
        public Optional<MapperConfigAnnotation> mapperConfig(TypeDescriptor configType) {
            return delegate.mapperConfig( configType );
        }

        @Override
        public LangDiagnostics diagnostics() {
            return delegate.diagnostics();
        }

        @Override
        public org.mapstruct.ap.internal.langmodel.GeneratedFileAccess generatedFiles() {
            return delegate.generatedFiles();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> OptionalCapability<T> optional(Class<T> capabilityType) {
            OptionalCapability<?> override = overrides.get( capabilityType );
            if ( override != null ) {
                return (OptionalCapability<T>) override;
            }
            return delegate.optional( capabilityType );
        }

        @Override
        public void close() {
            // Delegate lifecycle handled externally.
        }
    }

    private static final class StubProcessorContext implements ModelElementProcessor.ProcessorContext {

        private final LangModelContext context;
        private final org.mapstruct.ap.internal.option.Options options =
            new org.mapstruct.ap.internal.option.Options( Collections.emptyMap() );
        private final VersionInformation versionInformation;
        private final TypeFactory typeFactory;
        private final DescriptorUnwrapper descriptorUnwrapper = new DescriptorUnwrapper() {
            @Override
            public <NATIVE> Optional<NATIVE> type(org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor descriptor,
                                                  Class<NATIVE> nativeType) {
                return Optional.empty();
            }

            @Override
            public <NATIVE> Optional<NATIVE> type(org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor descriptor,
                                                  Class<NATIVE> nativeType) {
                return Optional.empty();
            }

            @Override
            public <NATIVE> Optional<NATIVE> element(org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor descriptor,
                                                    Class<NATIVE> nativeType) {
                return Optional.empty();
            }

            @Override
            public <NATIVE> Optional<NATIVE> annotation(org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor descriptor,
                                                       Class<NATIVE> nativeType) {
                return Optional.empty();
            }

            @Override
            public <NATIVE> Optional<NATIVE> annotationValue(
                org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor descriptor,
                Class<NATIVE> nativeType) {
                return Optional.empty();
            }
        };

        private StubProcessorContext(LangModelContext context,
                                     VersionInformation versionInformation,
                                     TypeFactory typeFactory) {
            this.context = context;
            this.versionInformation = versionInformation;
            this.typeFactory = typeFactory;
        }

        private StubProcessorContext(LangModelContext context, VersionInformation versionInformation) {
            this( context, versionInformation, null );
        }

        @Override
        public org.mapstruct.ap.internal.codegen.CodeGenerator getCodeGenerator() {
            return null;
        }

        @Override
        public org.mapstruct.ap.internal.codegen.CodeGenerationContext getCodeGenerationContext() {
            return null;
        }

        @Override
        public LangModelContext getLangModelContext() {
            return context;
        }

        @Override
        public TypeFactory getTypeFactory() {
            return typeFactory;
        }

        @Override
        public FormattingMessager getMessager() {
            return new NoopFormattingMessager();
        }

        @Override
        public org.mapstruct.ap.internal.util.AccessorNamingUtils getAccessorNaming() {
            return null;
        }

        @Override
        public Map<String, EnumTransformationStrategy> getEnumTransformationStrategies() {
            return Collections.emptyMap();
        }

        @Override
        public EnumMappingStrategy getEnumMappingStrategy() {
            return null;
        }

        @Override
        public org.mapstruct.ap.internal.option.Options getOptions() {
            return options;
        }

        @Override
        public VersionInformation getVersionInformation() {
            return versionInformation;
        }

        @Override
        public org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper getDescriptorUnwrapper() {
            return descriptorUnwrapper;
        }

        @Override
        public boolean isErroneous() {
            return false;
        }
    }

    private static final class NoopFormattingMessager implements FormattingMessager {

        @Override
        public void printMessage(Message msg, Object... args) {
        }

        @Override
        public void printMessage(org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor element, Message msg, Object... args) {
        }

        @Override
        public void printMessage(org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor element,
                                 org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor annotation,
                                 org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor value,
                                 Message msg,
                                 Object... args) {
        }

        @Override
        public void note(int level, Message log, Object... args) {
        }

        @Override
        public boolean isErroneous() {
            return false;
        }
    }

    private static final class StubAnnotationProcessorContext implements AnnotationProcessorContextView {

        @Override
        public org.mapstruct.ap.internal.util.AccessorNamingUtils getAccessorNaming() {
            return null;
        }

        @Override
        public Map<String, EnumTransformationStrategy> getEnumTransformationStrategies() {
            return Collections.emptyMap();
        }

        @Override
        public EnumMappingStrategy getEnumMappingStrategy() {
            return null;
        }

        @Override
        public boolean isTypeComplete(TypeDescriptor descriptor) {
            return true;
        }

        @Override
        public TypeHierarchyErroneousException typeHierarchyErroneousException(TypeDescriptor descriptor) {
            return new TypeHierarchyErroneousException();
        }
    }
}
