/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.ksp;

import com.google.devtools.ksp.processing.CodeGenerator;
import com.google.devtools.ksp.processing.KSPLogger;
import com.google.devtools.ksp.processing.Resolver;
import com.google.devtools.ksp.processing.SymbolProcessor;
import com.google.devtools.ksp.symbol.KSAnnotated;
import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSVisitorVoid;

import org.mapstruct.ap.internal.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.MapperEntryPoint;
import org.mapstruct.ap.internal.langmodel.MissingLangModelCapabilityException;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.codegen.GeneratedFileSink;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.ksp.KspLangModelContext;
import org.mapstruct.ap.internal.langmodel.ksp.KspLangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.ksp.KspSequenceUtils;
import org.mapstruct.ap.internal.model.Mapper;
import org.mapstruct.ap.internal.option.MappingOption;
import org.mapstruct.ap.internal.option.Options;
import org.mapstruct.ap.internal.processor.AnnotationProcessorContext;
import org.mapstruct.ap.internal.processor.DefaultModelElementProcessorContext;
import org.mapstruct.ap.internal.processor.ModelElementProcessor;
import org.mapstruct.ap.internal.processor.ModelElementProcessor.ProcessorContext;
import org.mapstruct.ap.internal.util.AnnotationProcessingException;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.RoundContext;
import org.mapstruct.ap.internal.util.Services;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * KSP (Kotlin Symbol Processing) implementation of the MapStruct annotation processor.
 * <p>
 * This processor finds all classes annotated with {@code @Mapper} and generates implementation classes.
 */
public class MapStructSymbolProcessor implements SymbolProcessor {

    private static final String MAPPER_ANNOTATION_FQN = "org.mapstruct.Mapper";

    private final KSPLogger logger;
    private final CodeGenerator codeGenerator;
    private final Map<String, String> processorOptions;
    private final Options options;
    private final VersionInformation versionInformation;
    private final KspLangModelContextFactory langModelContextFactory;
    private final DescriptorUnwrapper descriptorUnwrapper;
    private final AnnotationProcessorContext annotationProcessorContext;

    private final Set<String> deferredMappers = new HashSet<>();
    private boolean processed = false;

    public MapStructSymbolProcessor(KSPLogger logger,
                                     CodeGenerator codeGenerator,
                                     Map<String, String> processorOptions) {
        this.logger = Objects.requireNonNull( logger, "logger" );
        this.codeGenerator = Objects.requireNonNull( codeGenerator, "codeGenerator" );
        this.processorOptions = processorOptions != null ? processorOptions : Collections.emptyMap();
        this.options = new Options( this.processorOptions );
        this.versionInformation = new KspVersionInformation();
        this.langModelContextFactory = new KspLangModelContextFactory();
        this.descriptorUnwrapper = langModelContextFactory.descriptorUnwrapper();

        KspDiagnosticReporter diagnosticReporter = new KspDiagnosticReporter( logger );
        this.annotationProcessorContext = new AnnotationProcessorContext(
            diagnosticReporter,
            options.isDisableBuilders(),
            options.isVerbose(),
            resolveAdditionalOptions( this.processorOptions ),
            descriptorUnwrapper,
            langModelContextFactory.accessorNamingAdapterFactory()
        );
    }

    @Override
    public List<KSAnnotated> process(Resolver resolver) {
        if ( processed ) {
            // KSP may call process multiple times
            return Collections.emptyList();
        }

        List<KSAnnotated> deferred = new ArrayList<>();

        try {
            RoundContext roundContext = new RoundContext( annotationProcessorContext );

            // Find all classes annotated with @Mapper
            List<KSClassDeclaration> mappers = findMappers( resolver );

            if ( options.isVerbose() && !mappers.isEmpty() ) {
                logger.info( "MapStruct KSP: Found " + mappers.size() + " mapper(s) to process", null );
            }

            for ( KSClassDeclaration mapper : mappers ) {
                try {
                    processMapper( resolver, mapper, roundContext );
                }
                catch ( TypeHierarchyErroneousException thie ) {
                    // Type is not complete yet, defer processing
                    if ( options.isVerbose() ) {
                        logger.info( "MapStruct: referred types not available (yet), deferring mapper: "
                            + mapper.getQualifiedName().asString(), mapper );
                    }
                    deferredMappers.add( mapper.getQualifiedName().asString() );
                    deferred.add( mapper );
                }
                catch ( MissingLangModelCapabilityException missing ) {
                    logger.error( missing.getMessage(), mapper );
                }
                catch ( Throwable t ) {
                    handleUncaughtError( mapper, t );
                }
            }

            processed = true;
        }
        catch ( Throwable t ) {
            logger.error( "MapStruct KSP internal error: " + t.getMessage(), null );
            StringWriter sw = new StringWriter();
            t.printStackTrace( new PrintWriter( sw ) );
            logger.error( sw.toString(), null );
        }

        return deferred;
    }

    private List<KSClassDeclaration> findMappers(Resolver resolver) {
        List<KSClassDeclaration> mappers = new ArrayList<>();

        // Get all symbols annotated with @Mapper
        Iterable<KSAnnotated> annotatedSymbols = KspSequenceUtils.toIterable(
            resolver.getSymbolsWithAnnotation( MAPPER_ANNOTATION_FQN, true ) );

        for ( KSAnnotated annotated : annotatedSymbols ) {
            if ( annotated instanceof KSClassDeclaration ) {
                KSClassDeclaration classDecl = (KSClassDeclaration) annotated;

                // Skip if already processed in a previous round
                if ( deferredMappers.contains( classDecl.getQualifiedName().asString() ) ) {
                    continue;
                }

                // Verify it has @Mapper annotation
                if ( hasMapperAnnotation( classDecl ) ) {
                    mappers.add( classDecl );
                }
            }
        }

        return mappers;
    }

    private boolean hasMapperAnnotation(KSClassDeclaration classDecl) {
        for ( KSAnnotation annotation : KspSequenceUtils.toIterable( classDecl.getAnnotations() ) ) {
            KSType annotationType = annotation.getAnnotationType().resolve();
            KSDeclaration declaration = annotationType.getDeclaration();
            if ( declaration != null && declaration.getQualifiedName() != null ) {
                if ( MAPPER_ANNOTATION_FQN.equals( declaration.getQualifiedName().asString() ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    private void processMapper(Resolver resolver, KSClassDeclaration mapper, RoundContext roundContext) {
        MapperEntryPoint mapperEntryPoint = MapperEntryPoint.of(
            versionInformation,
            resolver,
            logger,
            codeGenerator,
            mapper
        );

        try ( LangModelContext langModelContext = langModelContextFactory.create( mapperEntryPoint ) ) {
            annotationProcessorContext.prepare( langModelContext );

            GeneratedFileSink generatedFileSink = Objects.requireNonNull(
                langModelContext.generatedFiles().generatedFileSink(),
                () -> langModelContext.getClass().getName() + " produced a null GeneratedFileSink"
            );

            FormattingMessager formattingMessager = new KspFormattingMessager( logger, options.isVerbose() );

            ProcessorContext context = new DefaultModelElementProcessorContext(
                options,
                roundContext,
                getDeclaredTypesNotToBeImported( mapper ),
                langModelContext,
                descriptorUnwrapper,
                formattingMessager,
                generatedFileSink,
                versionInformation
            );

            TypeElementDescriptor mapperDescriptor = toTypeElementDescriptor( context, mapper );
            processMapperDescriptor( context, mapperDescriptor );

            if ( options.isVerbose() ) {
                logger.info( "MapStruct KSP: Successfully processed mapper "
                    + mapper.getQualifiedName().asString(), mapper );
            }
        }
    }

    private Map<String, String> getDeclaredTypesNotToBeImported(KSClassDeclaration classDecl) {
        Map<String, String> result = new HashMap<>();

        for ( KSDeclaration declaration : KspSequenceUtils.toIterable( classDecl.getDeclarations() ) ) {
            if ( declaration instanceof KSClassDeclaration ) {
                String simpleName = declaration.getSimpleName().asString();
                String qualifiedName = classDecl.getQualifiedName().asString() + "." + simpleName;
                result.put( simpleName, qualifiedName );
            }
        }

        return result;
    }

    private TypeElementDescriptor toTypeElementDescriptor(ProcessorContext context, KSClassDeclaration classDecl) {
        LangModelContext langModelContext = context.getLangModelContext();
        LangDescriptorFactory descriptorFactory = langModelContext.typeSystem().descriptors();
        return descriptorFactory.typeElementDescriptor( classDecl );
    }

    private void processMapperDescriptor(ProcessorContext context, TypeElementDescriptor mapperDescriptor) {
        Object model = null;

        for ( ModelElementProcessor<?, ?> processor : getProcessors() ) {
            try {
                model = process( context, processor, mapperDescriptor, model );
            }
            catch ( AnnotationProcessingException e ) {
                logger.error( e.getMessage(), mapperDescriptor != null
                    ? (KSClassDeclaration) mapperDescriptor.unwrap()
                    : null );
                break;
            }
        }
    }

    private <P, R> R process(ProcessorContext context, ModelElementProcessor<P, R> processor,
                             TypeElementDescriptor mapperDescriptor, Object modelElement) {
        @SuppressWarnings("unchecked")
        P sourceElement = (P) modelElement;
        return processor.process( context, mapperDescriptor, sourceElement );
    }

    private Iterable<ModelElementProcessor<?, ?>> getProcessors() {
        @SuppressWarnings("rawtypes")
        Iterator<ModelElementProcessor> processorIterator = ServiceLoader.load(
            ModelElementProcessor.class,
            MapStructSymbolProcessor.class.getClassLoader()
        ).iterator();

        List<ModelElementProcessor<?, ?>> processors = new ArrayList<>();
        while ( processorIterator.hasNext() ) {
            processors.add( processorIterator.next() );
        }

        processors.sort( Comparator.comparingInt( ModelElementProcessor::getPriority ) );
        return processors;
    }

    private void handleUncaughtError(KSClassDeclaration element, Throwable thrown) {
        StringWriter sw = new StringWriter();
        thrown.printStackTrace( new PrintWriter( sw ) );
        String reportableStacktrace = sw.toString().replace( System.lineSeparator(), "  " );
        logger.error( "Internal error in the mapping processor: " + reportableStacktrace, element );
    }

    private Map<String, String> resolveAdditionalOptions(Map<String, String> allOptions) {
        Map<String, String> result = new HashMap<>();
        for ( Map.Entry<String, String> entry : allOptions.entrySet() ) {
            // Filter out mapstruct options, keep only additional SPI options
            if ( !entry.getKey().startsWith( "mapstruct" ) ) {
                result.put( entry.getKey(), entry.getValue() );
            }
        }
        return result;
    }

    @Override
    public void finish() {
        // Called when processing is complete
    }

    @Override
    public void onError() {
        // Called when an error occurs
    }

    /**
     * KSP-specific version information.
     */
    private static final class KspVersionInformation implements VersionInformation {

        private final String runtimeVersion;
        private final String runtimeVendor;
        private final String mapStructVersion;

        KspVersionInformation() {
            this.runtimeVersion = System.getProperty( "java.version" );
            this.runtimeVendor = System.getProperty( "java.vendor" );
            this.mapStructVersion = detectMapStructVersion();
        }

        @Override
        public String getRuntimeVersion() {
            return runtimeVersion;
        }

        @Override
        public String getRuntimeVendor() {
            return runtimeVendor;
        }

        @Override
        public String getMapStructVersion() {
            return mapStructVersion;
        }

        @Override
        public String getCompiler() {
            return "KSP";
        }

        @Override
        public boolean isSourceVersionAtLeast9() {
            // Kotlin targets JVM 1.8+ by default, so we assume at least Java 9 features
            return true;
        }

        @Override
        public boolean isSourceVersionAtLeast19() {
            try {
                int majorVersion = Integer.parseInt( runtimeVersion.split( "[._-]" )[0] );
                return majorVersion >= 19;
            }
            catch ( NumberFormatException e ) {
                return false;
            }
        }

        @Override
        public boolean isEclipseJDTCompiler() {
            return false;
        }

        @Override
        public boolean isJavacCompiler() {
            return false;
        }

        private static String detectMapStructVersion() {
            Package pkg = MapStructSymbolProcessor.class.getPackage();
            if ( pkg != null ) {
                String version = pkg.getImplementationVersion();
                if ( version != null ) {
                    return version;
                }
            }
            return "";
        }
    }
}
