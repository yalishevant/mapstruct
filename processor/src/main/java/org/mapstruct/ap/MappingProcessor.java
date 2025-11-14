/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.lang.reflect.Proxy;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementKindVisitor6;
import javax.tools.Diagnostic.Kind;

import org.mapstruct.ap.internal.model.Mapper;
import org.mapstruct.ap.internal.option.MappingOption;
import org.mapstruct.ap.internal.option.Options;
import org.mapstruct.ap.internal.processor.DefaultModelElementProcessorContext;
import org.mapstruct.ap.internal.processor.ModelElementProcessor;
import org.mapstruct.ap.internal.processor.ModelElementProcessor.ProcessorContext;
import org.mapstruct.ap.internal.util.AnnotationProcessingException;
import org.mapstruct.ap.internal.processor.AnnotationProcessorContext;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.MessagerBackedFormattingMessager;
import org.mapstruct.ap.internal.util.MessagerDiagnosticReporter;
import org.mapstruct.ap.internal.util.RoundContext;
import org.mapstruct.ap.internal.util.Services;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.internal.langmodel.MissingLangModelCapabilityException;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.LangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.MapperEntryPoint;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.codegen.GeneratedFileSink;
import org.mapstruct.ap.internal.langmodel.javax.JavaxLangModelContextFactory;
import org.mapstruct.ap.spi.AdditionalSupportedOptionsProvider;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;

import static javax.lang.model.element.ElementKind.CLASS;

/**
 * A JSR 269 annotation {@link Processor} which generates the implementations for mapper interfaces (interfaces
 * annotated with {@code @Mapper}).
 * <p>
 * Implementation notes:
 * <p>
 * The generation happens by incrementally building up a model representation of each mapper to be generated (a
 * {@link Mapper} object), which is then written into the resulting Java source file.
 * <p>
 * The model instantiation and processing happens in several phases/passes by applying a sequence of
 * {@link ModelElementProcessor}s. The processors to apply are retrieved using the Java service loader mechanism and are
 * processed in order of their {@link ModelElementProcessor#getPriority() priority}. The general processing flow is
 * this:
 * <ul>
 * <li>retrieve mapping methods</li>
 * <li>create the {@code Mapper} model</li>
 * <li>perform enrichments and modifications (e.g. add annotations for dependency injection)</li>
 * <li>if no error occurred, write out the model into Java source files</li>
 * </ul>
 * <p>
 * For reading annotation attributes, gems as generated with help of <a
 * href="https://github.com/mapstruct/tools-gem">Gem Tools</a>. These gems allow comfortable access to annotations and
 * their attributes without depending on their class objects.
 * <p>
 * The creation of Java source files is done using the <a href="http://freemarker.org/"> FreeMarker</a> template engine.
 * Each node of the mapper model has a corresponding FreeMarker template file which provides the Java representation of
 * that element and can include sub-elements via a custom FreeMarker directive. That way writing out a root node of the
 * model ({@code Mapper}) will recursively include all contained sub-elements (such as its methods, their property
 * mappings etc.).
 *
 * @author Gunnar Morling
 */
@SupportedAnnotationTypes("org.mapstruct.Mapper")
public class MappingProcessor extends AbstractProcessor {

    /**
     * Whether this processor claims all processed annotations exclusively or not.
     */
    private static final boolean ANNOTATIONS_CLAIMED_EXCLUSIVELY = false;
    private static final String MAPPER_ANNOTATION_FQCN = "org.mapstruct.Mapper";

    // CHECKSTYLE:OFF
    // Deprecated options, kept for backwards compatibility.
    // They will be removed in a future release.
    @Deprecated
    protected static final String SUPPRESS_GENERATOR_TIMESTAMP = MappingOption.SUPPRESS_GENERATOR_TIMESTAMP.getOptionName();
    @Deprecated
    protected static final String SUPPRESS_GENERATOR_VERSION_INFO_COMMENT = MappingOption.SUPPRESS_GENERATOR_VERSION_INFO_COMMENT.getOptionName();
    @Deprecated
    protected static final String UNMAPPED_TARGET_POLICY = MappingOption.UNMAPPED_TARGET_POLICY.getOptionName();
    @Deprecated
    protected static final String UNMAPPED_SOURCE_POLICY = MappingOption.UNMAPPED_SOURCE_POLICY.getOptionName();
    @Deprecated
    protected static final String DEFAULT_COMPONENT_MODEL = MappingOption.DEFAULT_COMPONENT_MODEL.getOptionName();
    @Deprecated
    protected static final String DEFAULT_INJECTION_STRATEGY = MappingOption.DEFAULT_INJECTION_STRATEGY.getOptionName();
    @Deprecated
    protected static final String ALWAYS_GENERATE_SERVICE_FILE = MappingOption.ALWAYS_GENERATE_SERVICE_FILE.getOptionName();
    @Deprecated
    protected static final String DISABLE_BUILDERS = MappingOption.DISABLE_BUILDERS.getOptionName();
    @Deprecated
    protected static final String VERBOSE = MappingOption.VERBOSE.getOptionName();
    @Deprecated
    protected static final String NULL_VALUE_ITERABLE_MAPPING_STRATEGY = MappingOption.NULL_VALUE_ITERABLE_MAPPING_STRATEGY.getOptionName();
    @Deprecated
    protected static final String NULL_VALUE_MAP_MAPPING_STRATEGY = MappingOption.NULL_VALUE_MAP_MAPPING_STRATEGY.getOptionName();
    // CHECKSTYLE:ON

    private final Set<String> additionalSupportedOptions;
    private final String additionalSupportedOptionsError;

    private Options options;
    private VersionInformation versionInformation;

    private AnnotationProcessorContext annotationProcessorContext;
    private LangModelContextFactory langModelContextFactory;
    private DescriptorUnwrapper descriptorUnwrapper;

    /**
     * Any mappers for which an implementation cannot be generated in the current round because they have source/target
     * types with incomplete hierarchies (as super-types are to be generated by other processors). They will be
     * processed in subsequent rounds.
     * <p>
     * If the hierarchy of a mapper's source/target types is never completed (i.e. the missing super-types are not
     * generated by other processors), this mapper will not be generated; That's fine, the compiler will raise an error
     * due to the inconsistent Java types used as source or target anyway.
     */
    private Set<DeferredMapper> deferredMappers = new HashSet<>();

    public MappingProcessor() {
        Set<String> additionalSupportedOptions;
        String additionalSupportedOptionsError;
        try {
            additionalSupportedOptions = resolveAdditionalSupportedOptions();
            additionalSupportedOptionsError = null;
        }
        catch ( IllegalStateException ex ) {
            additionalSupportedOptions = Collections.emptySet();
            additionalSupportedOptionsError = ex.getMessage();
        }
        this.additionalSupportedOptions = additionalSupportedOptions;
        this.additionalSupportedOptionsError = additionalSupportedOptionsError;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init( processingEnv );

        options = new Options( processingEnv.getOptions() );
        versionInformation = new ProcessorVersionInformation( processingEnv );
        langModelContextFactory = resolveLangModelContextFactory( processingEnv );
        descriptorUnwrapper = langModelContextFactory.descriptorUnwrapper();
        MessagerDiagnosticReporter diagnosticReporter = new MessagerDiagnosticReporter( processingEnv.getMessager() );
        annotationProcessorContext = new AnnotationProcessorContext(
            diagnosticReporter,
            options.isDisableBuilders(),
            options.isVerbose(),
            resolveAdditionalOptions( processingEnv.getOptions() ),
            descriptorUnwrapper,
            langModelContextFactory.accessorNamingAdapterFactory()
        );

        if ( additionalSupportedOptionsError != null ) {
            processingEnv.getMessager().printMessage( Kind.ERROR, additionalSupportedOptionsError );
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
        // nothing to do in the last round
        if ( !roundEnvironment.processingOver() ) {
            RoundContext roundContext = new RoundContext( annotationProcessorContext );

            // process any mappers left over from previous rounds
            Set<TypeElement> deferredMappers = getAndResetDeferredMappers();
            processMapperElements( deferredMappers, roundContext, roundEnvironment );

            // get and process any mappers from this round
            Set<TypeElement> mappers = getMappers( annotations, roundEnvironment );
            processMapperElements( mappers, roundContext, roundEnvironment );
        }
        else if ( !deferredMappers.isEmpty() ) {
            // If the processing is over and there are deferred mappers it means something wrong occurred and
            // MapStruct didn't generate implementations for those
            for ( DeferredMapper deferredMapper : deferredMappers ) {

                TypeElement deferredMapperElement = deferredMapper.deferredMapperElement;
                Element erroneousElement = deferredMapper.erroneousElement;
                String erroneousElementName;

                if ( erroneousElement instanceof QualifiedNameable ) {
                    erroneousElementName = ( (QualifiedNameable) erroneousElement ).getQualifiedName().toString();
                }
                else {
                    erroneousElementName =
                        erroneousElement != null ? erroneousElement.getSimpleName().toString() : null;
                }

                // When running on Java 8 we need to fetch the deferredMapperElement again.
                // Otherwise the reporting will not work properly
                deferredMapperElement = processingEnv.getElementUtils()
                    .getTypeElement( deferredMapperElement.getQualifiedName() );

                processingEnv.getMessager()
                    .printMessage(
                        Kind.ERROR,
                        "No implementation was created for " + deferredMapperElement.getSimpleName() +
                            " due to having a problem in the erroneous element " + erroneousElementName + "." +
                            " Hint: this often means that some other annotation processor was supposed to" +
                            " process the erroneous element. You can also enable MapStruct verbose mode by setting" +
                            " -Amapstruct.verbose=true as a compilation argument.",
                        deferredMapperElement
                    );
            }

        }

        return ANNOTATIONS_CLAIMED_EXCLUSIVELY;
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Stream.concat(
                Stream.of( MappingOption.values() ).map( MappingOption::getOptionName ),
                additionalSupportedOptions.stream()
            )
            .collect( Collectors.toSet() );
    }

    private LangModelContextFactory resolveLangModelContextFactory(ProcessingEnvironment processingEnvironment) {
        List<LangModelContextFactory> factories = new ArrayList<>();
        for ( LangModelContextFactory factory : Services.all( LangModelContextFactory.class ) ) {
            factories.add( factory );
        }
        if ( factories.isEmpty() ) {
            factories.add( new JavaxLangModelContextFactory() );
        }

        LangModelContextFactory defaultFactory = selectDefaultFactory( factories );

        String requestedBackend = options.getLangModelBackend();
        if ( requestedBackend == null || requestedBackend.trim().isEmpty() ) {
            return defaultFactory;
        }

        String normalizedRequest = normalizeBackendName( requestedBackend );
        for ( LangModelContextFactory factory : factories ) {
            if ( matchesBackend( factory, normalizedRequest ) ) {
                return factory;
            }
        }

        String availableBackends = factories.stream()
            .map( LangModelContextFactory::backendId )
            .map( MappingProcessor::normalizeBackendName )
            .filter( Objects::nonNull )
            .distinct()
            .sorted()
            .collect( Collectors.joining( ", " ) );
        if ( availableBackends.isEmpty() ) {
            availableBackends = factories.stream()
                .map( LangModelContextFactory::getClass )
                .map( Class::getName )
                .map( MappingProcessor::normalizeBackendName )
                .filter( Objects::nonNull )
                .distinct()
                .sorted()
                .collect( Collectors.joining( ", " ) );
        }

        String message = "Unknown MapStruct lang model backend \"" + requestedBackend + "\". Available backends: "
            + availableBackends;
        processingEnvironment.getMessager().printMessage( Kind.ERROR, message );
        throw new IllegalStateException( "Unknown MapStruct lang model backend: " + requestedBackend );
    }

    private LangModelContextFactory selectDefaultFactory(List<LangModelContextFactory> factories) {
        for ( LangModelContextFactory factory : factories ) {
            String backendId = normalizeBackendName( factory.backendId() );
            if ( "javax".equals( backendId ) ) {
                return factory;
            }
        }
        return factories.get( 0 );
    }

    private static boolean matchesBackend(LangModelContextFactory factory, String normalizedRequest) {
        if ( normalizedRequest == null ) {
            return false;
        }
        String backendId = normalizeBackendName( factory.backendId() );
        if ( backendId != null && backendId.equals( normalizedRequest ) ) {
            return true;
        }
        String className = normalizeBackendName( factory.getClass().getName() );
        return className != null && className.equals( normalizedRequest );
    }

    private static String normalizeBackendName(String name) {
        if ( name == null ) {
            return null;
        }
        String trimmed = name.trim();
        if ( trimmed.isEmpty() ) {
            return null;
        }
        return trimmed.toLowerCase( Locale.ROOT );
    }

    /**
     * Gets fresh copies of all mappers deferred from previous rounds (the originals may contain references to
     * erroneous source/target type elements).
     */
    private Set<TypeElement> getAndResetDeferredMappers() {
        Set<TypeElement> deferred = new HashSet<>( deferredMappers.size() );

        for ( DeferredMapper deferredMapper : deferredMappers ) {
            TypeElement element = deferredMapper.deferredMapperElement;
            deferred.add( processingEnv.getElementUtils().getTypeElement( element.getQualifiedName() ) );
        }

        deferredMappers.clear();
        return deferred;
    }

    private Set<TypeElement> getMappers(final Set<? extends TypeElement> annotations,
                                        final RoundEnvironment roundEnvironment) {
        Set<TypeElement> mapperTypes = new HashSet<>();

        for ( TypeElement annotation : annotations ) {
            //Indicates that the annotation's type isn't on the class path of the compiled
            //project. Let the compiler deal with that and print an appropriate error.
            if ( annotation.getKind() != ElementKind.ANNOTATION_TYPE ) {
                continue;
            }

            try {
                Set<? extends Element> annotatedMappers = roundEnvironment.getElementsAnnotatedWith( annotation );
                for (Element mapperElement : annotatedMappers) {
                    TypeElement mapperTypeElement = asTypeElement( mapperElement );

                    // on some JDKs, RoundEnvironment.getElementsAnnotatedWith( ... ) returns types with
                    // annotations unknown to the compiler, even though they are not declared Mappers
                    if ( mapperTypeElement != null && isMapperAnnotationPresent( mapperTypeElement ) ) {
                        mapperTypes.add( mapperTypeElement );
                    }
                }
            }
            catch ( Throwable t ) { // whenever that may happen, but just to stay on the save side
                handleUncaughtError( annotation, t );
                continue;
            }
        }
        return mapperTypes;
    }

    private boolean isMapperAnnotationPresent(TypeElement element) {
        try {
            for ( AnnotationMirror annotationMirror : element.getAnnotationMirrors() ) {
                Element annotationElement = annotationMirror.getAnnotationType().asElement();
                if ( annotationElement instanceof TypeElement ) {
                    CharSequence qualifiedName = ( (TypeElement) annotationElement ).getQualifiedName();
                    if ( qualifiedName != null && MAPPER_ANNOTATION_FQCN.contentEquals( qualifiedName ) ) {
                        return true;
                    }
                }
                else if ( MAPPER_ANNOTATION_FQCN.equals( annotationMirror.getAnnotationType().toString() ) ) {
                    return true;
                }
            }
            return false;
        }
        catch ( RuntimeException ex ) {
            return false;
        }
    }

    private void processMapperElements(Set<TypeElement> mapperElements,
                                       RoundContext roundContext,
                                       RoundEnvironment roundEnvironment) {
        for ( TypeElement mapperElement : mapperElements ) {
            try {
                // create a new context for each generated mapper in order to have imports of referenced types
                // correctly managed;
                // note that this assumes that a new source file is created for each mapper which must not
                // necessarily be the case, e.g. in case of several mapper interfaces declared as inner types
                // of one outer interface
                MapperEntryPoint mapperEntryPoint = mapperEntryPointFor( mapperElement );
                try ( LangModelContext langModelContext = langModelContextFactory.create( mapperEntryPoint ) ) {
                    annotationProcessorContext.prepare( langModelContext );
                    GeneratedFileSink generatedFileSink = Objects.requireNonNull(
                        langModelContext.generatedFiles().generatedFileSink(),
                        () -> langModelContext.getClass().getName() + " produced a null GeneratedFileSink"
                    );
                    FormattingMessager formattingMessager = new MessagerBackedFormattingMessager(
                        processingEnv.getMessager(),
                        options.isVerbose(),
                        descriptorUnwrapper
                    );

                    ProcessorContext context = new DefaultModelElementProcessorContext(
                        options,
                        roundContext,
                        getDeclaredTypesNotToBeImported( mapperElement ),
                        langModelContext,
                        descriptorUnwrapper,
                        formattingMessager,
                        generatedFileSink,
                        versionInformation
                    );

                    TypeElementDescriptor mapperDescriptor = toTypeElementDescriptor( context, mapperElement );
                    processMapperDescriptor( context, mapperDescriptor );
                }
            }
            catch ( MissingLangModelCapabilityException missing ) {
                processingEnv.getMessager().printMessage(
                    Kind.ERROR,
                    missing.getMessage(),
                    mapperElement
                );
                break;
            }
            catch ( TypeHierarchyErroneousException thie ) {
                TypeDescriptor erroneousType = thie.getType();
                Element erroneousElement = null;
                if ( erroneousType != null && descriptorUnwrapper != null ) {
                    TypeElementDescriptor erroneousDescriptor = erroneousType.typeElement().orElse( null );
                    if ( erroneousDescriptor != null ) {
                        erroneousElement = descriptorUnwrapper.element(
                            erroneousDescriptor,
                            TypeElement.class
                        ).orElse( null );
                    }
                    if ( erroneousElement == null ) {
                        erroneousElement = descriptorUnwrapper.type( erroneousType, TypeElement.class ).orElse( null );
                    }
                }
                if ( erroneousElement == null ) {
                    TypeMirror erroneousMirror = thie.getTypeMirror();
                    if ( erroneousMirror != null ) {
                        erroneousElement = processingEnv.getTypeUtils().asElement( erroneousMirror );
                    }
                }
                if ( options.isVerbose() ) {
                    processingEnv.getMessager().printMessage(
                        Kind.NOTE, "MapStruct: referred types not available (yet), deferring mapper: "
                            + mapperElement );
                }
                deferredMappers.add( new DeferredMapper( mapperElement, erroneousElement ) );
            }
            catch ( Throwable t ) {
                handleUncaughtError( mapperElement, t );
                break;
            }
        }
    }

    private TypeElementDescriptor toTypeElementDescriptor(ProcessorContext context, TypeElement mapperElement) {
        LangModelContext langModelContext = context.getLangModelContext();
        LangDescriptorFactory descriptorFactory = langModelContext.typeSystem().descriptors();
        return descriptorFactory.typeElementDescriptor( mapperElement );
    }

    private Map<String, String> getDeclaredTypesNotToBeImported(TypeElement element) {
        return element.getEnclosedElements().stream()
            .filter( e -> CLASS.equals( e.getKind() ) )
            .map( Element::getSimpleName )
            .map( Name::toString )
            .collect( Collectors.toMap( k -> k, v -> element.getQualifiedName().toString() + "." + v ) );
    }

    private MapperEntryPoint mapperEntryPointFor(TypeElement mapperElement) {
        return MapperEntryPoint.of( versionInformation, processingEnv, mapperElement );
    }

    private void handleUncaughtError(Element element, Throwable thrown) {
        StringWriter sw = new StringWriter();
        thrown.printStackTrace( new PrintWriter( sw ) );

        String reportableStacktrace = sw.toString().replace( System.lineSeparator( ), "  " );

        processingEnv.getMessager().printMessage(
            Kind.ERROR, "Internal error in the mapping processor: " + reportableStacktrace, element );
    }

    /**
     * Applies all registered {@link ModelElementProcessor}s to the given mapper
     * type.
     *
     * @param context The processor context.
     * @param mapperDescriptor Descriptor of the mapper type element.
     */
    private void processMapperDescriptor(ProcessorContext context, TypeElementDescriptor mapperDescriptor) {
        Object model = null;

        for ( ModelElementProcessor<?, ?> processor : getProcessors() ) {
            try {
                model = process( context, processor, mapperDescriptor, model );
            }
            catch ( AnnotationProcessingException e ) {
                Element element = unwrapElement( e.getElement() );
                AnnotationMirror annotation = unwrapAnnotation( e.getAnnotation() );
                AnnotationValue annotationValue = unwrapAnnotationValue( e.getAnnotationValue() );

                if ( element != null && annotation != null && annotationValue != null ) {
                    processingEnv.getMessager()
                        .printMessage( Kind.ERROR, e.getMessage(), element, annotation, annotationValue );
                }
                else if ( element != null && annotation != null ) {
                    processingEnv.getMessager()
                        .printMessage( Kind.ERROR, e.getMessage(), element, annotation );
                }
                else if ( element != null ) {
                    processingEnv.getMessager().printMessage( Kind.ERROR, e.getMessage(), element );
                }
                else {
                    processingEnv.getMessager().printMessage( Kind.ERROR, e.getMessage() );
                }
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

    private Element unwrapElement(ElementDescriptor descriptor) {
        return descriptorUnwrapper != null
            ? descriptorUnwrapper.element( descriptor, Element.class ).orElse( null )
            : null;
    }

    private AnnotationMirror unwrapAnnotation(AnnotationDescriptor descriptor) {
        return descriptorUnwrapper != null
            ? descriptorUnwrapper.annotation( descriptor, AnnotationMirror.class ).orElse( null )
            : null;
    }

    private AnnotationValue unwrapAnnotationValue(AnnotationValueDescriptor descriptor) {
        return descriptorUnwrapper != null
            ? descriptorUnwrapper.annotationValue( descriptor, AnnotationValue.class ).orElse( null )
            : null;
    }

    /**
     * Retrieves all model element processors, ordered by their priority value
     * (with the method retrieval processor having the lowest priority value (1))
     * and the code generation processor the highest priority value.
     *
     * @return A list with all model element processors.
     */
    private Iterable<ModelElementProcessor<?, ?>> getProcessors() {
        // TODO Re-consider which class loader to use in case processors are
        // loaded from other modules, too
        @SuppressWarnings("rawtypes")
        Iterator<ModelElementProcessor> processorIterator = ServiceLoader.load(
            ModelElementProcessor.class,
            MappingProcessor.class.getClassLoader()
        )
            .iterator();
        List<ModelElementProcessor<?, ?>> processors = new ArrayList<>();

        while ( processorIterator.hasNext() ) {
            processors.add( processorIterator.next() );
        }

        processors.sort( new ProcessorComparator() );

        return processors;
    }

    private TypeElement asTypeElement(Element element) {
        return element.accept(
            new ElementKindVisitor6<TypeElement, Void>() {
                @Override
                public TypeElement visitTypeAsInterface(TypeElement e, Void p) {
                    return e;
                }

                @Override
                public TypeElement visitTypeAsClass(TypeElement e, Void p) {
                    return e;
                }

            }, null
        );
    }

    /**
     * Fetch the additional supported options provided by the SPI {@link AdditionalSupportedOptionsProvider}.
     *
     * @return the additional supported options
     */
    private static Set<String> resolveAdditionalSupportedOptions() {
        Set<String> additionalSupportedOptions = null;
        for ( AdditionalSupportedOptionsProvider optionsProvider :
            Services.all( AdditionalSupportedOptionsProvider.class ) ) {
            if ( additionalSupportedOptions == null ) {
                additionalSupportedOptions = new HashSet<>();
            }
            Set<String> providerOptions = optionsProvider.getAdditionalSupportedOptions();

            for ( String providerOption : providerOptions ) {
                // Ensure additional options are not in the mapstruct namespace
                if ( providerOption.startsWith( "mapstruct" ) ) {
                    throw new IllegalStateException(
                        "Additional SPI options cannot start with \"mapstruct\". Provider " + optionsProvider +
                            " provided option " + providerOption );
                }
                additionalSupportedOptions.add( providerOption );
            }

        }

        return additionalSupportedOptions == null ? Collections.emptySet() : additionalSupportedOptions;
    }

    private static class ProcessorComparator implements Comparator<ModelElementProcessor<?, ?>> {

        @Override
        public int compare(ModelElementProcessor<?, ?> o1, ModelElementProcessor<?, ?> o2) {
            return Integer.compare( o1.getPriority(), o2.getPriority() );
        }
    }

    private static final class ProcessorVersionInformation implements VersionInformation {

        private final String runtimeVersion;
        private final String runtimeVendor;
        private final String mapStructVersion;
        private final String compiler;
        private final boolean sourceVersionAtLeast9;
        private final boolean sourceVersionAtLeast19;
        private final boolean eclipseJdt;
        private final boolean javac;

        ProcessorVersionInformation(ProcessingEnvironment processingEnv) {
            this.runtimeVersion = System.getProperty( "java.version" );
            this.runtimeVendor = System.getProperty( "java.vendor" );
            this.mapStructVersion = detectMapStructVersion();
            this.compiler = detectCompiler( processingEnv );
            SourceVersion sourceVersion = processingEnv.getSourceVersion();
            this.sourceVersionAtLeast9 = sourceVersion.compareTo( SourceVersion.RELEASE_6 ) > 2;
            this.sourceVersionAtLeast19 = sourceVersion.compareTo( SourceVersion.RELEASE_6 ) > 12;
            String className = processingEnv.getClass().getName();
            this.eclipseJdt = className.startsWith( "org.eclipse.jdt" );
            this.javac = className.equals( "com.sun.tools.javac.processing.JavacProcessingEnvironment" );
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
            return compiler;
        }

        @Override
        public boolean isSourceVersionAtLeast9() {
            return sourceVersionAtLeast9;
        }

        @Override
        public boolean isSourceVersionAtLeast19() {
            return sourceVersionAtLeast19;
        }

        @Override
        public boolean isEclipseJDTCompiler() {
            return eclipseJdt;
        }

        @Override
        public boolean isJavacCompiler() {
            return javac;
        }

        private static String detectMapStructVersion() {
            Package pkg = MappingProcessor.class.getPackage();
            if ( pkg != null ) {
                String version = pkg.getImplementationVersion();
                if ( version != null ) {
                    return version;
                }
            }
            return "";
        }

        private static String detectCompiler(ProcessingEnvironment processingEnv) {
            String className = processingEnv.getClass().getName();
            if ( Proxy.isProxyClass( processingEnv.getClass() ) ) {
                className = processingEnv.toString();
            }
            if ( className.contains( "JavacProcessingEnvironment" ) ) {
                return "javac";
            }
            if ( className.contains( "org.eclipse.jdt" ) ) {
                return "Eclipse JDT";
            }
            return className;
        }
    }

    private static class DeferredMapper {

        private final TypeElement deferredMapperElement;
        private final Element erroneousElement;

        private DeferredMapper(TypeElement deferredMapperElement, Element erroneousElement) {
            this.deferredMapperElement = deferredMapperElement;
            this.erroneousElement = erroneousElement;
        }
    }

    /**
     * Filters only the options belonging to the declared additional supported options.
     *
     * @param options all processor environment options
     * @return filtered options
     */
    private Map<String, String> resolveAdditionalOptions(Map<String, String> options) {
        return options.entrySet().stream()
            .filter( entry -> additionalSupportedOptions.contains( entry.getKey() ) )
            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
    }
}
