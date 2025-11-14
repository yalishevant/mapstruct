/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.processor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import org.mapstruct.ap.internal.langmodel.MissingLangModelCapabilityException;
import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapter;
import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapterFactory;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;
import org.mapstruct.ap.spi.AccessorNamingStrategy;
import org.mapstruct.ap.spi.AstModifyingAnnotationProcessor;
import org.mapstruct.ap.spi.BuilderProvider;
import org.mapstruct.ap.spi.DefaultAccessorNamingStrategy;
import org.mapstruct.ap.spi.DefaultBuilderProvider;
import org.mapstruct.ap.spi.DefaultEnumMappingStrategy;
import org.mapstruct.ap.spi.EnumMappingStrategy;
import org.mapstruct.ap.spi.EnumTransformationStrategy;
import org.mapstruct.ap.spi.FreeBuilderAccessorNamingStrategy;
import org.mapstruct.ap.spi.ImmutablesAccessorNamingStrategy;
import org.mapstruct.ap.spi.ImmutablesBuilderProvider;
import org.mapstruct.ap.spi.MapStructProcessingEnvironment;
import org.mapstruct.ap.spi.NoOpBuilderProvider;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;
import org.mapstruct.ap.internal.util.AccessorNamingUtils;
import org.mapstruct.ap.internal.util.AnnotationProcessorContextView;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.util.Services;
import org.mapstruct.ap.internal.util.FreeBuilderConstants;
import org.mapstruct.ap.internal.util.ImmutablesConstants;
import org.mapstruct.ap.internal.util.DiagnosticReporter;
import org.mapstruct.ap.internal.langmodel.spi.SpiBridgeCapability;

/**
 * Keeps contextual data in the scope of the entire annotation processor ("application scope").
 *
 * @author Gunnar Morling
 */
public class AnnotationProcessorContext implements AnnotationProcessorContextView {

    private final List<AstModifyingAnnotationProcessor> astModifyingAnnotationProcessors;

    private BuilderProvider builderProvider;
    private AccessorNamingStrategy accessorNamingStrategy;
    private EnumMappingStrategy enumMappingStrategy;
    private boolean initialized;
    private Map<String, EnumTransformationStrategy> enumTransformationStrategies;

    private AccessorNamingUtils accessorNaming;
    private final boolean disableBuilder;
    private final boolean verbose;
    private final Map<String, String> options;
    private final DescriptorUnwrapper descriptorUnwrapper;
    private final AccessorNamingAdapterFactory accessorNamingAdapterFactory;
    private final DiagnosticReporter diagnosticReporter;
    private LangModelContext pendingLangModelContext;
    private LangModelContext currentLangModelContext;
    private MapStructProcessingEnvironment spiEnvironment;

    public AnnotationProcessorContext(DiagnosticReporter diagnosticReporter,
                                      boolean disableBuilder,
                                      boolean verbose,
                                      Map<String, String> options,
                                      DescriptorUnwrapper descriptorUnwrapper,
                                      AccessorNamingAdapterFactory accessorNamingAdapterFactory) {
        this.diagnosticReporter = Objects.requireNonNull( diagnosticReporter, "diagnosticReporter" );
        this.disableBuilder = disableBuilder;
        this.verbose = verbose;
        Map<String, String> resolvedOptions = new LinkedHashMap<>();
        if ( options != null ) {
            resolvedOptions.putAll( options );
        }
        this.options = Collections.unmodifiableMap( resolvedOptions );
        this.descriptorUnwrapper = Objects.requireNonNull( descriptorUnwrapper, "descriptorUnwrapper" );
        this.accessorNamingAdapterFactory = Objects.requireNonNull(
            accessorNamingAdapterFactory,
            "accessorNamingAdapterFactory"
        );
        this.astModifyingAnnotationProcessors = Collections.unmodifiableList(
            findAstModifyingAnnotationProcessors( diagnosticReporter )
        );
    }

    public void prepare(LangModelContext langModelContext) {
        Objects.requireNonNull( langModelContext, "langModelContext" );
        this.currentLangModelContext = langModelContext;
        if ( initialized ) {
            return;
        }
        this.pendingLangModelContext = langModelContext;
    }

    /**
     * Method for initializing the context with the SPIs. The reason why we do this is due to the fact that
     * when custom SPI implementations are done and users don't set {@code proc:none} then our processor
     * would be triggered. And this context will always get initialized and the SPI won't be found. However,
     * if this is lazily evaluated it won't be a problem, as in the SPI implementation module there won't be any
     * processing done.
     */
    private void initializeIfNeeded() {
        if ( initialized ) {
            return;
        }

        LangModelContext langModelContext = pendingLangModelContext;
        if ( langModelContext == null ) {
            throw new IllegalStateException(
                "AnnotationProcessorContext.prepare must be invoked before initialization"
            );
        }
        pendingLangModelContext = null;

        SpiBridgeCapability bridgeCapability = langModelContext.optional( SpiBridgeCapability.class )
            .orElseThrow( () -> MissingLangModelCapabilityException.required( SpiBridgeCapability.class ) );

        MapStructProcessingEnvironment environment = Objects.requireNonNull(
            bridgeCapability.spiEnvironment( options ),
            "LangModelContext did not provide a MapStructProcessingEnvironment"
        );

        this.spiEnvironment = environment;

        LangElements elements = langModelContext.elementQuery().elements();
        AccessorNamingStrategy defaultAccessorNamingStrategy;
        BuilderProvider defaultBuilderProvider;
        if ( elements.typeElement( ImmutablesConstants.IMMUTABLE_FQN ) != null ) {
            defaultAccessorNamingStrategy = new ImmutablesAccessorNamingStrategy();
            defaultBuilderProvider = new ImmutablesBuilderProvider();
            if ( verbose ) {
                diagnosticReporter.note( "MapStruct: Immutables found on classpath" );
            }
        }
        else if ( elements.typeElement( FreeBuilderConstants.FREE_BUILDER_FQN ) != null ) {
            defaultAccessorNamingStrategy = new FreeBuilderAccessorNamingStrategy();
            defaultBuilderProvider = new DefaultBuilderProvider();
            if ( verbose ) {
                diagnosticReporter.note( "MapStruct: Freebuilder found on classpath" );
            }
        }
        else {
            defaultAccessorNamingStrategy = new DefaultAccessorNamingStrategy();
            defaultBuilderProvider = new DefaultBuilderProvider();
        }
        this.accessorNamingStrategy = Services.get( AccessorNamingStrategy.class, defaultAccessorNamingStrategy );
        this.accessorNamingStrategy.init( spiEnvironment );
        if ( verbose ) {
            diagnosticReporter.note(
                "MapStruct: Using accessor naming strategy: "
                    + accessorNamingStrategy.getClass().getCanonicalName()
            );
        }
        this.builderProvider = this.disableBuilder ?
            new NoOpBuilderProvider() :
            Services.get( BuilderProvider.class, defaultBuilderProvider );
        this.builderProvider.init( spiEnvironment );
        if ( verbose ) {
            diagnosticReporter.note(
                "MapStruct: Using builder provider: " + builderProvider.getClass().getCanonicalName()
            );
        }
        AccessorNamingAdapter accessorNamingAdapter = accessorNamingAdapterFactory.create(
            this.accessorNamingStrategy,
            descriptorUnwrapper,
            langModelContext
        );
        this.accessorNaming = new AccessorNamingUtils( accessorNamingAdapter );

        this.enumMappingStrategy = Services.get( EnumMappingStrategy.class, new DefaultEnumMappingStrategy() );
        this.enumMappingStrategy.init( spiEnvironment );
        if ( verbose ) {
            diagnosticReporter.note(
                "MapStruct: Using enum naming strategy: "
                    + enumMappingStrategy.getClass().getCanonicalName()
            );
        }

        this.enumTransformationStrategies = new LinkedHashMap<>();
        ServiceLoader<EnumTransformationStrategy> transformationStrategiesLoader = ServiceLoader.load(
            EnumTransformationStrategy.class,
            AnnotationProcessorContext.class.getClassLoader()
        );

        for ( EnumTransformationStrategy transformationStrategy : transformationStrategiesLoader ) {
            String transformationStrategyName = transformationStrategy.getStrategyName();
            if ( enumTransformationStrategies.containsKey( transformationStrategyName ) ) {
                throw new IllegalStateException(
                    "Multiple EnumTransformationStrategies are using the same ma,e. Found: " +
                        enumTransformationStrategies.get( transformationStrategyName ) + " and " +
                        transformationStrategy + " for name " + transformationStrategyName );
            }

            transformationStrategy.init( spiEnvironment );
            enumTransformationStrategies.put( transformationStrategyName, transformationStrategy );
        }


        this.initialized = true;
    }

    private List<AstModifyingAnnotationProcessor> findAstModifyingAnnotationProcessors(
        DiagnosticReporter diagnosticReporter) {
        List<AstModifyingAnnotationProcessor> processors = new ArrayList<>();

        ServiceLoader<AstModifyingAnnotationProcessor> loader = ServiceLoader.load(
                AstModifyingAnnotationProcessor.class, AnnotationProcessorContext.class.getClassLoader()
        );

        // Lombok packages an AstModifyingAnnotationProcessor as part of their jar
        // this leads to problems within Eclipse when lombok is used as an agent
        // Therefore we are wrapping this into an iterator that can handle exceptions by ignoring
        // the faulty processor
        Iterator<AstModifyingAnnotationProcessor> loaderIterator = new FaultyDelegatingIterator(
            diagnosticReporter,
            loader.iterator()
        );

        while ( loaderIterator.hasNext() ) {
            AstModifyingAnnotationProcessor processor = loaderIterator.next();
            if ( processor != null ) {
                processors.add( processor );
            }
        }

        return processors;
    }

    private static class FaultyDelegatingIterator implements Iterator<AstModifyingAnnotationProcessor> {

        private final DiagnosticReporter diagnosticReporter;
        private final Iterator<AstModifyingAnnotationProcessor> delegate;

        private FaultyDelegatingIterator(DiagnosticReporter diagnosticReporter,
            Iterator<AstModifyingAnnotationProcessor> delegate) {
            this.diagnosticReporter = diagnosticReporter;
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            // Check the delegate maximum of 5 times
            // before returning false
            int failures = 5;
            while ( failures > 0 ) {
                try {
                    return delegate.hasNext();
                }
                catch ( Throwable t ) {
                    failures--;
                    logFailure( t );
                }
            }

            return false;
        }

        @Override
        public AstModifyingAnnotationProcessor next() {
            try {
                return delegate.next();
            }
            catch ( Throwable t ) {
                logFailure( t );
                return null;
            }
        }

        private void logFailure(Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace( new PrintWriter( sw ) );

            String reportableStacktrace = sw.toString().replace( System.lineSeparator(), "  " );

            diagnosticReporter.warning(
                "Failed to read AstModifyingAnnotationProcessor. Reading next processor. Reason: "
                    + reportableStacktrace
            );
        }
    }

    public List<AstModifyingAnnotationProcessor> getAstModifyingAnnotationProcessors() {
        return astModifyingAnnotationProcessors;
    }

    public boolean isTypeComplete(TypeDescriptor descriptor) {
        initializeIfNeeded();
        LangModelContext langModelContext = currentLangModelContext;
        if ( langModelContext == null ) {
            return true;
        }
        return langModelContext.diagnostics().isTypeComplete( descriptor, astModifyingAnnotationProcessors );
    }

    public TypeHierarchyErroneousException typeHierarchyErroneousException(TypeDescriptor descriptor) {
        initializeIfNeeded();
        LangModelContext langModelContext = currentLangModelContext;
        if ( langModelContext == null ) {
            return new TypeHierarchyErroneousException();
        }
        return langModelContext.diagnostics().typeHierarchyErroneousException( descriptor );
    }

    public AccessorNamingUtils getAccessorNaming() {
        initializeIfNeeded();
        return accessorNaming;
    }

    public AccessorNamingStrategy getAccessorNamingStrategy() {
        initializeIfNeeded();
        return accessorNamingStrategy;
    }

    public EnumMappingStrategy getEnumMappingStrategy() {
        initializeIfNeeded();
        return enumMappingStrategy;
    }

    public BuilderProvider getBuilderProvider() {
        initializeIfNeeded();
        return builderProvider;
    }

    public Map<String, EnumTransformationStrategy> getEnumTransformationStrategies() {
        initializeIfNeeded();
        return enumTransformationStrategies;
    }

    public Map<String, String> getOptions() {
        return this.options;
    }
}
