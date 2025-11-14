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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.mapstruct.ap.internal.langmodel.javax.DefaultVersionInformation;
import org.mapstruct.ap.internal.processor.AnnotationProcessorContext;
import org.mapstruct.ap.internal.util.DiagnosticReporter;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.GeneratedFileAccess;
import org.mapstruct.ap.internal.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.LangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.MapperAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperConfigAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperEntryPoint;
import org.mapstruct.ap.internal.langmodel.OptionalCapability;
import org.mapstruct.ap.internal.langmodel.javax.JavaxLangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.api.LangTypes;
import org.mapstruct.ap.internal.langmodel.TypeIntrospector;
import org.mapstruct.ap.spi.AstModifyingAnnotationProcessor;
import org.mapstruct.ap.internal.langmodel.spi.LangDiagnostics;
import org.mapstruct.ap.internal.langmodel.spi.SpiBridgeCapability;

/**
 * Annotation processor that verifies SPI fallbacks log diagnostics via {@link LangDiagnostics} when the bridge
 * capability is unavailable.
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class LangModelSpiFallbackDiagnosticsProcessor extends AbstractProcessor {

    private static final String EXPECTED_WARNING = String.format(
        Message.OPTIONAL_CAPABILITY_MISSING.getDescription(),
        "SpiBridgeCapability",
        "SPI integrations"
    );

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

                    RecordingLangDiagnostics diagnostics = new RecordingLangDiagnostics( delegate.diagnostics() );
                    LangModelContext capabilityOverride =
                        override( delegate, SpiBridgeCapability.class, OptionalCapability.empty() );
                    LangModelContext decorated =
                        new DiagnosticsDecoratingContext( capabilityOverride, diagnostics );

                    AnnotationProcessorContext annotationContext = new AnnotationProcessorContext(
                        new RecordingDiagnosticReporter(),
                        false,
                        false,
                        Collections.emptyMap(),
                        factory.descriptorUnwrapper(),
                        factory.accessorNamingAdapterFactory()
                    );

                    annotationContext.prepare( decorated );
                    annotationContext.getAccessorNaming();
                    annotationContext.getBuilderProvider();

                    if ( !diagnostics.containsWarning( EXPECTED_WARNING ) ) {
                        errors.add( "Expected optional capability warning not emitted: " + EXPECTED_WARNING );
                    }
                }
            }
        }
        catch ( Exception ex ) {
            errors.add( "Unexpected exception during SPI fallback verification: " + ex );
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

    @SuppressWarnings("unchecked")
    private LangModelContext cast(
        LangModelContext context) {
        return (LangModelContext) context;
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

    private static final class RecordingLangDiagnostics implements LangDiagnostics {

        private final LangDiagnostics delegate;
        private final List<String> warnings = new ArrayList<>();

        private RecordingLangDiagnostics(LangDiagnostics delegate) {
            this.delegate = Objects.requireNonNull( delegate, "delegate" );
        }

        @Override
        public void warning(String message) {
            warnings.add( message );
            delegate.warning( message );
        }

        @Override
        public void error(String message) {
            delegate.error( message );
        }

        @Override
        public boolean isTypeComplete(TypeDescriptor descriptor,
                                      List<? extends AstModifyingAnnotationProcessor> processors) {
            return delegate.isTypeComplete( descriptor, processors );
        }

        @Override
        public org.mapstruct.ap.spi.TypeHierarchyErroneousException typeHierarchyErroneousException(
            TypeDescriptor descriptor) {
            return delegate.typeHierarchyErroneousException( descriptor );
        }

        boolean containsWarning(String expected) {
            return warnings.stream().anyMatch( expected::equals );
        }
    }

    private static final class DiagnosticsDecoratingContext
        implements LangModelContext {

        private final LangModelContext delegate;
        private final LangDiagnostics diagnostics;

        private DiagnosticsDecoratingContext(
            LangModelContext delegate,
            LangDiagnostics diagnostics) {
            this.delegate = Objects.requireNonNull( delegate, "delegate" );
            this.diagnostics = Objects.requireNonNull( diagnostics, "diagnostics" );
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
            return diagnostics;
        }

        @Override
        public GeneratedFileAccess generatedFiles() {
            return delegate.generatedFiles();
        }

        @Override
        public <T> OptionalCapability<T> optional(Class<T> capabilityType) {
            return delegate.optional( capabilityType );
        }

        @Override
        public void close() {
            // Delegate lifecycle handled externally.
        }
    }

    private static final class CapabilityOverridingContext
        implements LangModelContext {

        private final LangModelContext delegate;
        private final Map<Class<?>, OptionalCapability<?>> overrides;

        private CapabilityOverridingContext(
            LangModelContext delegate,
            Map<Class<?>, OptionalCapability<?>> overrides) {
            this.delegate = Objects.requireNonNull( delegate, "delegate" );
            this.overrides = new HashMap<>( overrides );
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
        public GeneratedFileAccess generatedFiles() {
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

    private static final class RecordingDiagnosticReporter implements DiagnosticReporter {

        private final List<String> warnings = new ArrayList<>();

        @Override
        public void note(String message) {
            // ignored for this test
        }

        @Override
        public void warning(String message) {
            warnings.add( message );
        }

        @Override
        public void error(String message) {
            warnings.add( message );
        }

        List<String> warnings() {
            return warnings;
        }
    }
}
