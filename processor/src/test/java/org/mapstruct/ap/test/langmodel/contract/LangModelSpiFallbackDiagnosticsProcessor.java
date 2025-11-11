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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.mapstruct.ap.langmodel.javax.DefaultVersionInformation;
import org.mapstruct.ap.internal.processor.AnnotationProcessorContext;
import org.mapstruct.ap.internal.util.DiagnosticReporter;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.langmodel.GeneratedFileAccess;
import org.mapstruct.ap.langmodel.LangModelContext;
import org.mapstruct.ap.langmodel.LangModelContextFactory;
import org.mapstruct.ap.langmodel.LangModelElementQuery;
import org.mapstruct.ap.langmodel.LangModelTypeSystem;
import org.mapstruct.ap.langmodel.MapperEntryPoint;
import org.mapstruct.ap.langmodel.OptionalCapability;
import org.mapstruct.ap.langmodel.javax.JavaxLangModelContextFactory;
import org.mapstruct.ap.spi.AstModifyingAnnotationProcessor;
import org.mapstruct.ap.spi.lang.LangDiagnostics;
import org.mapstruct.ap.spi.lang.SpiBridgeCapability;

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
                try ( LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> delegate =
                    cast( factory.create( entryPoint ) ) ) {

                    RecordingLangDiagnostics diagnostics = new RecordingLangDiagnostics( delegate.diagnostics() );
                    LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> capabilityOverride =
                        override( delegate, SpiBridgeCapability.class, OptionalCapability.empty() );
                    LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> decorated =
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
    private LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> cast(
        LangModelContext<?, ?, ?, ?> context) {
        return (LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue>) context;
    }

    private LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> override(
        LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> delegate,
        Class<?> capabilityType,
        OptionalCapability<?> capability) {

        Map<Class<?>, OptionalCapability<?>> overrides = new HashMap<>();
        LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> base = delegate;
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
        implements LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> {

        private final LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> delegate;
        private final LangDiagnostics diagnostics;

        private DiagnosticsDecoratingContext(
            LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> delegate,
            LangDiagnostics diagnostics) {
            this.delegate = Objects.requireNonNull( delegate, "delegate" );
            this.diagnostics = Objects.requireNonNull( diagnostics, "diagnostics" );
        }

        @Override
        public LangModelTypeSystem<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> typeSystem() {
            return delegate.typeSystem();
        }

        @Override
        public LangModelElementQuery elementQuery() {
            return delegate.elementQuery();
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
        implements LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> {

        private final LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> delegate;
        private final Map<Class<?>, OptionalCapability<?>> overrides;

        private CapabilityOverridingContext(
            LangModelContext<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> delegate,
            Map<Class<?>, OptionalCapability<?>> overrides) {
            this.delegate = Objects.requireNonNull( delegate, "delegate" );
            this.overrides = new HashMap<>( overrides );
        }

        @Override
        public LangModelTypeSystem<TypeMirror, TypeElement, AnnotationMirror, AnnotationValue> typeSystem() {
            return delegate.typeSystem();
        }

        @Override
        public LangModelElementQuery elementQuery() {
            return delegate.elementQuery();
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
