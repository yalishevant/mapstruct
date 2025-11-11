/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.gem.DecoratedWithGem;
import org.mapstruct.ap.internal.gem.InheritConfigurationGem;
import org.mapstruct.ap.internal.gem.InheritInverseConfigurationGem;
import org.mapstruct.ap.internal.gem.JavadocGem;
import org.mapstruct.ap.internal.gem.MappingInheritanceStrategyGem;
import org.mapstruct.ap.internal.gem.NullValueMappingStrategyGem;
import org.mapstruct.ap.internal.model.AdditionalAnnotationsBuilder;
import org.mapstruct.ap.internal.model.Annotation;
import org.mapstruct.ap.internal.model.BeanMappingMethod;
import org.mapstruct.ap.internal.model.ContainerMappingMethod;
import org.mapstruct.ap.internal.model.ContainerMappingMethodBuilder;
import org.mapstruct.ap.internal.model.Decorator;
import org.mapstruct.ap.internal.model.DefaultMapperReference;
import org.mapstruct.ap.internal.model.DelegatingMethod;
import org.mapstruct.ap.internal.model.Field;
import org.mapstruct.ap.internal.model.IterableMappingMethod;
import org.mapstruct.ap.internal.model.Javadoc;
import org.mapstruct.ap.internal.model.MapMappingMethod;
import org.mapstruct.ap.internal.model.Mapper;
import org.mapstruct.ap.internal.model.MapperReference;
import org.mapstruct.ap.internal.model.MappingBuilderContext;
import org.mapstruct.ap.internal.model.MappingMethod;
import org.mapstruct.ap.internal.model.StreamMappingMethod;
import org.mapstruct.ap.internal.model.SupportingConstructorFragment;
import org.mapstruct.ap.internal.model.ValueMappingMethod;
import org.mapstruct.ap.internal.model.common.FormattingParameters;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.model.source.MapperOptions;
import org.mapstruct.ap.internal.model.source.MappingMethodOptions;
import org.mapstruct.ap.internal.model.source.Method;
import org.mapstruct.ap.internal.model.source.SelectionParameters;
import org.mapstruct.ap.internal.model.source.SourceMethod;
import org.mapstruct.ap.internal.option.Options;
import org.mapstruct.ap.internal.processor.creation.MappingResolverImpl;
import org.mapstruct.ap.internal.util.AccessorNamingUtils;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.util.Strings;
import org.mapstruct.ap.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.langmodel.AnnotationGemFactory;
import org.mapstruct.ap.langmodel.AnnotationGemsCapability;
import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.MissingLangModelCapabilityException;
import org.mapstruct.ap.descriptor.LangModifier;
import org.mapstruct.ap.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.descriptor.LangElementKind;
import org.mapstruct.ap.langmodel.api.LangElements;
import org.mapstruct.ap.langmodel.LangModelContext;
import org.mapstruct.ap.langmodel.LangModelElementQuery;
import org.mapstruct.ap.langmodel.LangModelTypeSystem;
import org.mapstruct.ap.langmodel.api.LangTypes;
import org.mapstruct.ap.descriptor.FieldDescriptor;
import org.mapstruct.ap.descriptor.ParameterDescriptor;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.langmodel.MapperAnnotation;
import org.mapstruct.ap.internal.version.VersionInformation;

import static org.mapstruct.ap.internal.model.SupportingConstructorFragment.addAllFragmentsIn;
import static org.mapstruct.ap.internal.model.SupportingField.addAllFieldsIn;
import static org.mapstruct.ap.internal.util.Collections.first;
import static org.mapstruct.ap.internal.util.Collections.join;

/**
 * A {@link ModelElementProcessor} which creates a {@link Mapper} from the given
 * list of {@link SourceMethod}s.
 *
 * @author Gunnar Morling
 */
public class MapperCreationProcessor implements ModelElementProcessor<List<SourceMethod>, Mapper> {

    private LangModelContext<?, ?, ?, ?> langModelContext;
    private LangModelElementQuery langElementQuery;
    private LangElements langElements;
    private LangTypes langTypes;
    private LangDescriptorFactory<Object, Object, Object, Object> descriptorFactory;
    private AnnotationGemFactory annotationGems;
    private FormattingMessager messager;
    private Options options;
    private VersionInformation versionInformation;
    private TypeFactory typeFactory;
    private AccessorNamingUtils accessorNaming;
    private MappingBuilderContext mappingContext;
    private AdditionalAnnotationsBuilder additionalAnnotationsBuilder;

    @Override
    public Mapper process(ProcessorContext context,
                          TypeElementDescriptor mapperDescriptor,
                          List<SourceMethod> sourceModel) {
        this.langModelContext = context.getLangModelContext();
        LangModelTypeSystem<?, ?, ?, ?> typeSystem = langModelContext.typeSystem();
        this.langElementQuery = langModelContext.elementQuery();
        AnnotationGemsCapability annotationGemsCapability = langModelContext.optional( AnnotationGemsCapability.class )
            .orElseThrow( () -> MissingLangModelCapabilityException.required( AnnotationGemsCapability.class ) );
        this.annotationGems = annotationGemsCapability.annotationGems();
        this.langElements = langElementQuery.elements();
        this.langTypes = typeSystem.types();
        @SuppressWarnings("unchecked")
        LangDescriptorFactory<Object, Object, Object, Object> descriptorFactory =
            (LangDescriptorFactory<Object, Object, Object, Object>) typeSystem.descriptors();
        this.descriptorFactory = descriptorFactory;
        this.messager =
            new MapperAnnotatedFormattingMessenger(
                context.getMessager(),
                mapperDescriptor
            );
        this.options = context.getOptions();
        this.versionInformation = context.getVersionInformation();
        this.typeFactory = context.getTypeFactory();
        this.accessorNaming = context.getAccessorNaming();
        additionalAnnotationsBuilder = new AdditionalAnnotationsBuilder( typeFactory, messager );

        MapperAnnotation mapperAnnotation = langElementQuery.mapperAnnotation( mapperDescriptor );
        MapperOptions mapperOptions = MapperOptions.fromAnnotation(
            mapperAnnotation,
            mapperDescriptor,
            context.getOptions(),
            langModelContext
        );
        List<MapperReference> mapperReferences = initReferencedMappers( mapperOptions );

        MappingBuilderContext ctx = new MappingBuilderContext(
            typeFactory,
            langModelContext,
            messager,
            accessorNaming,
            context.getEnumMappingStrategy(),
            context.getEnumTransformationStrategies(),
            options,
            new MappingResolverImpl(
                messager,
                typeFactory,
                langModelContext,
                new ArrayList<>( sourceModel ),
                mapperReferences,
                options,
                options.isVerbose()
            ),
            mapperDescriptor,
            //sourceModel is passed only to fetch the after/before mapping methods in lifecycleCallbackFactory;
            //Consider removing those methods directly into MappingBuilderContext.
            Collections.unmodifiableList( sourceModel ),
            mapperReferences
        );
        this.mappingContext = ctx;
        return getMapper( mapperDescriptor, mapperOptions, sourceModel );
    }

    @Override
    public int getPriority() {
        return 1000;
    }

    private List<MapperReference> initReferencedMappers(MapperOptions mapperAnnotation) {
        List<MapperReference> result = new LinkedList<>();
        List<String> variableNames = new LinkedList<>();

        for ( TypeDescriptor usedMapper : mapperAnnotation.uses() ) {
            Type type = typeFactory.getType( usedMapper );

            TypeElementDescriptor mapperDescriptor = usedMapper.typeElement().orElse( null );
            boolean isAnnotatedMapper = mapperDescriptor != null
                && langElementQuery.mapperAnnotation( mapperDescriptor ).isValid();
            boolean isSingleton = mapperDescriptor != null && hasSingletonInstance( mapperDescriptor );

            DefaultMapperReference mapperReference = DefaultMapperReference.getInstance(
                type,
                isAnnotatedMapper,
                isSingleton,
                typeFactory,
                variableNames
            );

            result.add( mapperReference );
            variableNames.add( mapperReference.getVariableName() );
        }

        return result;
    }

    private boolean hasSingletonInstance(TypeElementDescriptor mapperDescriptor) {
        if ( mapperDescriptor == null ) {
            return false;
        }
        TypeDescriptor mapperType = mapperDescriptor.asType();
        if ( mapperType == null ) {
            return false;
        }
        for ( FieldDescriptor field : langElements.enclosedFields( mapperDescriptor ) ) {
            if ( isPublicConstantOfType( field, "INSTANCE", mapperType ) ) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublicConstantOfType(FieldDescriptor field, String fieldName, TypeDescriptor expectedType) {
        if ( field == null || expectedType == null ) {
            return false;
        }
        boolean nameMatches = field.simpleName().content().equals( fieldName );
        boolean modifiersMatch = field.modifiers().contains( LangModifier.PUBLIC )
            && field.modifiers().contains( LangModifier.STATIC )
            && field.modifiers().contains( LangModifier.FINAL );
        TypeDescriptor fieldType = field.fieldType();
        boolean typeMatches = fieldType != null && langTypes.isSameType( fieldType, expectedType );
        return nameMatches && modifiersMatch && typeMatches;
    }

    private Mapper getMapper(TypeElementDescriptor elementDescriptor,
                             MapperOptions mapperOptions,
                             List<SourceMethod> methods) {

        List<MappingMethod> mappingMethods = getMappingMethods( mapperOptions, methods );
        mappingMethods.addAll( mappingContext.getUsedSupportedMappings() );
        mappingMethods.addAll( mappingContext.getMappingsToGenerate() );

        // handle fields
        List<Field> fields = new ArrayList<>( mappingContext.getMapperReferences() );
        Set<Field> supportingFieldSet = new LinkedHashSet<>(mappingContext.getUsedSupportedFields());
        addAllFieldsIn( mappingContext.getUsedSupportedMappings(), supportingFieldSet );
        fields.addAll( supportingFieldSet );

        // handle constructor fragments
        Set<SupportingConstructorFragment> constructorFragments = new LinkedHashSet<>();
        addAllFragmentsIn( mappingContext.getUsedSupportedMappings(), constructorFragments );

        Mapper mapper = new Mapper.Builder()
            .element( elementDescriptor )
            .methods( mappingMethods )
            .fields( fields )
            .constructorFragments(  constructorFragments )
            .options( options )
            .versionInformation( versionInformation )
            .decorator( getDecorator( elementDescriptor, methods, mapperOptions ) )
            .typeFactory( typeFactory )
            .extraImports( getExtraImports( elementDescriptor, mapperOptions ) )
            .implName( mapperOptions.implementationName() )
            .implPackage( mapperOptions.implementationPackage() )
            .suppressGeneratorTimestamp( mapperOptions.suppressTimestampInGenerated() )
            .additionalAnnotations( additionalAnnotationsBuilder.getProcessedAnnotations( elementDescriptor ) )
            .javadoc( getJavadoc( elementDescriptor ) )
            .build();

        if ( !mappingContext.getForgedMethodsUnderCreation().isEmpty() ) {
            messager.printMessage( elementDescriptor, Message.GENERAL_NOT_ALL_FORGED_CREATED,
                mappingContext.getForgedMethodsUnderCreation().keySet() );
        }

        if ( elementDescriptor != null && elementDescriptor.modifiers().contains( LangModifier.PRIVATE ) ) {
            // If the mapper element is private then we should report an error
            // we can't generate an implementation for a private mapper
            mappingContext.getMessager()
                .printMessage( elementDescriptor,
                    Message.GENERAL_CANNOT_IMPLEMENT_PRIVATE_MAPPER,
                    elementDescriptor.simpleName().content(),
                    elementDescriptor.kind() == LangElementKind.INTERFACE ? "interface" : "class"
                );
        }

        return mapper;
    }

    private Decorator getDecorator(TypeElementDescriptor mapperDescriptor,
                                   List<SourceMethod> methods,
                                   MapperOptions mapperOptions) {
        if ( mapperDescriptor == null ) {
            return null;
        }

        AnnotationDescriptor decoratorAnnotation = AnnotationDescriptorUtils.findAnnotation(
            langElements,
            mapperDescriptor,
            "org.mapstruct.DecoratedWith"
        ).orElse( null );

        DecoratedWithGem decoratedWith = annotationGems.decoratedWith( decoratorAnnotation );
        if ( decoratedWith == null ) {
            return null;
        }

        TypeDescriptor decoratorTypeDescriptor = decoratedWith.value().hasValue()
            ? descriptorFactory.typeDescriptor( decoratedWith.value().get() )
            : null;
        TypeElementDescriptor decoratorDescriptor = decoratorTypeDescriptor != null
            ? decoratorTypeDescriptor.typeElement().orElse( null )
            : null;
        if ( decoratorDescriptor == null ) {
            return null;
        }

        TypeDescriptor mapperTypeDescriptor = mapperDescriptor.asType();
        if ( mapperTypeDescriptor != null && decoratorTypeDescriptor != null
            && !langTypes.isAssignable( decoratorTypeDescriptor, mapperTypeDescriptor ) ) {
            messager.printMessage( mapperDescriptor, decoratorAnnotation, Message.DECORATOR_NO_SUBTYPE );
        }

        List<ExecutableDescriptor> decoratorMethods = langElements.enclosedExecutables( decoratorDescriptor )
            .stream()
            .filter( executable -> executable.kind() == LangElementKind.METHOD )
            .collect( Collectors.toList() );
        List<ExecutableDescriptor> decoratorConstructors = langElements.constructors( decoratorDescriptor );

        Type decoratorMapperType = mapperTypeDescriptor != null ? typeFactory.getType( mapperTypeDescriptor ) : null;
        List<MappingMethod> mappingMethods = new ArrayList<>( methods.size() );

        for ( SourceMethod mappingMethod : methods ) {
            boolean implementationRequired = true;
            ExecutableDescriptor mappingDescriptor = mappingMethod.getExecutableDescriptor();
            if ( mappingDescriptor != null ) {
                for ( ExecutableDescriptor methodDescriptor : decoratorMethods ) {
                    if ( langElements.overrides( methodDescriptor, mappingDescriptor, decoratorDescriptor ) ) {
                        implementationRequired = false;
                        break;
                    }
                }
            }

            Type declaringMapper = mappingMethod.getDeclaringMapper();
            if ( implementationRequired && !( mappingMethod.isDefault() || mappingMethod.isStatic() ) ) {
                if ( ( declaringMapper == null ) || declaringMapper.equals( decoratorMapperType ) ) {
                    mappingMethods.add( new DelegatingMethod( mappingMethod ) );
                }
            }
        }

        boolean hasDelegateConstructor = false;
        boolean hasDefaultConstructor = false;
        if ( mapperTypeDescriptor != null ) {
            for ( ExecutableDescriptor constructor : decoratorConstructors ) {
                if ( constructor.parameters().isEmpty() ) {
                    hasDefaultConstructor = true;
                }
                else if ( constructor.parameters().size() == 1 ) {
                    ParameterDescriptor parameter = constructor.parameters().get( 0 );
                    if ( langTypes.isAssignable( mapperTypeDescriptor, parameter.type() ) ) {
                        hasDelegateConstructor = true;
                    }
                }
            }
        }
        else {
            hasDefaultConstructor = decoratorConstructors.stream().anyMatch( c -> c.parameters().isEmpty() );
        }

        if ( !hasDelegateConstructor && !hasDefaultConstructor && decoratorAnnotation != null ) {
            messager.printMessage( mapperDescriptor, decoratorAnnotation, Message.DECORATOR_CONSTRUCTOR );
        }

        Set<Annotation> decoratorAnnotations = additionalAnnotationsBuilder
            .getProcessedAnnotations( decoratorDescriptor );

        Decorator decorator = new Decorator.Builder()
            .typeFactory( typeFactory )
            .mapperDescriptor( mapperDescriptor )
            .decoratorType( decoratorTypeDescriptor )
            .methods( mappingMethods )
            .hasDelegateConstructor( hasDelegateConstructor )
            .options( options )
            .versionInformation( versionInformation )
            .implName( mapperOptions.implementationName() )
            .implPackage( mapperOptions.implementationPackage() )
            .extraImports( getExtraImports( mapperDescriptor, mapperOptions ) )
            .suppressGeneratorTimestamp( mapperOptions.suppressTimestampInGenerated() )
            .additionalAnnotations( decoratorAnnotations )
            .build();

        return decorator;
    }

    private SortedSet<Type> getExtraImports(TypeElementDescriptor element, MapperOptions mapperOptions) {
        SortedSet<Type> extraImports = new TreeSet<>();


        for ( TypeDescriptor extraImport : mapperOptions.imports() ) {
            Type type = typeFactory.getAlwaysImportedType( extraImport );
            extraImports.add( type );
        }

        // Add original package if a dest package has been set
        if ( element != null && !"default".equals( mapperOptions.implementationPackage() ) ) {
            TypeDescriptor descriptor = element.asType();
            if ( descriptor != null ) {
                extraImports.add( typeFactory.getType( descriptor ) );
            }
        }

        return extraImports;
    }

    private List<MappingMethod> getMappingMethods(MapperOptions mapperAnnotation, List<SourceMethod> methods) {
        List<MappingMethod> mappingMethods = new ArrayList<>();

        for ( SourceMethod method : methods ) {
            if ( !method.overridesMethod() ) {
                continue;
            }

            mergeInheritedOptions( method, mapperAnnotation, methods, new ArrayList<>(), null );

            MappingMethodOptions mappingOptions = method.getOptions();

            boolean hasFactoryMethod = false;

            if ( method.isIterableMapping() ) {
                this.messager.note( 1, Message.ITERABLEMAPPING_CREATE_NOTE, method );


                IterableMappingMethod iterableMappingMethod = createWithElementMappingMethod(
                    method,
                    mappingOptions,
                    new IterableMappingMethod.Builder()
                );

                hasFactoryMethod = iterableMappingMethod.getFactoryMethod() != null;
                mappingMethods.add( iterableMappingMethod );
            }
            else if ( method.isMapMapping() ) {

                MapMappingMethod.Builder builder = new MapMappingMethod.Builder();

                SelectionParameters keySelectionParameters = null;
                FormattingParameters keyFormattingParameters = null;
                SelectionParameters valueSelectionParameters = null;
                FormattingParameters valueFormattingParameters = null;
                NullValueMappingStrategyGem nullValueMappingStrategy = null;

                if ( mappingOptions.getMapMapping() != null ) {
                    keySelectionParameters = mappingOptions.getMapMapping().getKeySelectionParameters();
                    keyFormattingParameters = mappingOptions.getMapMapping().getKeyFormattingParameters();
                    valueSelectionParameters = mappingOptions.getMapMapping().getValueSelectionParameters();
                    valueFormattingParameters = mappingOptions.getMapMapping().getValueFormattingParameters();
                    nullValueMappingStrategy = mappingOptions.getMapMapping().getNullValueMappingStrategy();
                }

                this.messager.note( 1, Message.MAPMAPPING_CREATE_NOTE, method );
                MapMappingMethod mapMappingMethod = builder
                    .mappingContext( mappingContext )
                    .method( method )
                    .keyFormattingParameters( keyFormattingParameters )
                    .keySelectionParameters( keySelectionParameters )
                    .valueFormattingParameters( valueFormattingParameters )
                    .valueSelectionParameters( valueSelectionParameters )
                    .build();

                hasFactoryMethod = mapMappingMethod.getFactoryMethod() != null;
                mappingMethods.add( mapMappingMethod );
            }
            else if ( method.isValueMapping() ) {
                // prefer value mappings over enum mapping
                this.messager.note( 1, Message.VALUEMAPPING_CREATE_NOTE, method );
                ValueMappingMethod valueMappingMethod = new ValueMappingMethod.Builder()
                    .mappingContext( mappingContext )
                    .method( method )
                    .valueMappings( mappingOptions.getValueMappings() )
                    .enumMapping( mappingOptions.getEnumMappingOptions() )
                    .build();

                if ( valueMappingMethod != null ) {
                    mappingMethods.add( valueMappingMethod );
                }
            }
            else if ( method.isRemovedEnumMapping() ) {
                messager.printMessage(
                    method.getExecutable(),
                    Message.ENUMMAPPING_REMOVED
                );
            }
            else if ( method.isStreamMapping() ) {
                this.messager.note( 1, Message.STREAMMAPPING_CREATE_NOTE, method );
                StreamMappingMethod streamMappingMethod = createWithElementMappingMethod(
                    method,
                    mappingOptions,
                    new StreamMappingMethod.Builder()
                );

                // If we do StreamMapping that means that internally there is a way to generate the result type
                hasFactoryMethod =
                    streamMappingMethod.getFactoryMethod() != null || method.getResultType().isStreamType();
                mappingMethods.add( streamMappingMethod );
            }
            else {
                this.messager.note( 1, Message.BEANMAPPING_CREATE_NOTE, method );
                BuilderGem builder = method.getOptions().getBeanMapping().getBuilder();
                Type userDefinedReturnType = getUserDesiredReturnType( method );
                Type builderBaseType = userDefinedReturnType != null ? userDefinedReturnType : method.getReturnType();
                BeanMappingMethod.Builder beanMappingBuilder = new BeanMappingMethod.Builder();
                BeanMappingMethod beanMappingMethod = beanMappingBuilder
                    .mappingContext( mappingContext )
                    .sourceMethod( method )
                    .userDefinedReturnType( userDefinedReturnType )
                    .returnTypeBuilder( typeFactory.builderTypeFor( builderBaseType, builder ) )
                    .build();

                // We can consider that the bean mapping method can always be constructed. If there is a problem
                // it would have been reported in its build
                hasFactoryMethod = true;
                if ( beanMappingMethod != null ) {
                    mappingMethods.add( beanMappingMethod );
                }
            }

            if ( !hasFactoryMethod ) {
                // A factory method  is allowed to return an interface type and hence, the generated
                // implementation as well. The check below must only be executed if there's no factory
                // method that could be responsible.
                reportErrorIfNoImplementationTypeIsRegisteredForInterfaceReturnType( method );
            }
        }
        return mappingMethods;
    }

    private Javadoc getJavadoc(TypeElementDescriptor elementDescriptor) {
        if ( elementDescriptor == null ) {
            return null;
        }

        AnnotationDescriptor javadocAnnotation = AnnotationDescriptorUtils.findAnnotation(
            langElements,
            elementDescriptor,
            "org.mapstruct.Javadoc"
        ).orElse( null );

        JavadocGem javadocGem = annotationGems.javadoc( javadocAnnotation );

        if ( javadocGem == null || !isConsistent( javadocGem, elementDescriptor, javadocAnnotation, messager ) ) {
            return null;
        }

        Javadoc javadoc = new Javadoc.Builder()
            .value( javadocGem.value().getValue() )
            .authors( javadocGem.authors().getValue() )
            .deprecated( javadocGem.deprecated().getValue() )
            .since( javadocGem.since().getValue() )
            .build();

        return javadoc;
    }

    private Type getUserDesiredReturnType(SourceMethod method) {
        SelectionParameters selectionParameters = method.getOptions().getBeanMapping().getSelectionParameters();
        if ( selectionParameters != null && selectionParameters.getResultType() != null ) {
            return typeFactory.getType( selectionParameters.getResultType() );
        }
        return null;
    }

    private <M extends ContainerMappingMethod> M createWithElementMappingMethod(SourceMethod method,
             MappingMethodOptions mappingMethodOptions, ContainerMappingMethodBuilder<?, M> builder) {

        FormattingParameters formattingParameters = null;
        SelectionParameters selectionParameters = null;

        if ( mappingMethodOptions.getIterableMapping() != null ) {
            formattingParameters = mappingMethodOptions.getIterableMapping().getFormattingParameters();
            selectionParameters = mappingMethodOptions.getIterableMapping().getSelectionParameters();
        }

        return builder
            .mappingContext( mappingContext )
            .method( method )
            .formattingParameters( formattingParameters )
            .selectionParameters( selectionParameters )
            .build();
    }

    private void mergeInheritedOptions(SourceMethod method, MapperOptions mapperConfig,
                                       List<SourceMethod> availableMethods, List<SourceMethod> initializingMethods,
                                       AnnotationDescriptor annotationDescriptor) {
        if ( initializingMethods.contains( method ) ) {
            // cycle detected

            initializingMethods.add( method );

            messager.printMessage(
                method.getExecutableDescriptor(),
                Message.INHERITCONFIGURATION_CYCLE,
                Strings.join( initializingMethods, " -> " ) );
            return;
        }

        initializingMethods.add( method );

        MappingMethodOptions mappingOptions = method.getOptions();
        List<SourceMethod> applicableReversePrototypeMethods = method.getApplicableReversePrototypeMethods();

        SourceMethod inverseTemplateMethod =
            getInverseTemplateMethod( join( availableMethods, applicableReversePrototypeMethods ),
                method,
                initializingMethods,
                mapperConfig );

        List<SourceMethod> applicablePrototypeMethods = method.getApplicablePrototypeMethods();

        SourceMethod forwardTemplateMethod =
            getForwardTemplateMethod(
                join( availableMethods, applicablePrototypeMethods ),
                method,
                initializingMethods,
                mapperConfig );

        // apply defined (@InheritConfiguration, @InheritInverseConfiguration) mappings
        if ( forwardTemplateMethod != null ) {
            mappingOptions.applyInheritedOptions( method, forwardTemplateMethod, false, annotationDescriptor );
        }
        if ( inverseTemplateMethod != null ) {
            mappingOptions.applyInheritedOptions( method, inverseTemplateMethod, true, annotationDescriptor );
        }

        // apply auto inherited options
        MappingInheritanceStrategyGem inheritanceStrategy = mapperConfig.getMappingInheritanceStrategy();
        if ( inheritanceStrategy.isAutoInherit() ) {

            // but.. there should not be an @InheritedConfiguration
            if ( forwardTemplateMethod == null && inheritanceStrategy.isApplyForward() ) {
                if ( applicablePrototypeMethods.size() == 1 ) {
                    mappingOptions.applyInheritedOptions( method, first( applicablePrototypeMethods ), false,
                        annotationDescriptor );
                }
                else if ( applicablePrototypeMethods.size() > 1 ) {
                    messager.printMessage(
                        method.getExecutableDescriptor(),
                        Message.INHERITCONFIGURATION_MULTIPLE_PROTOTYPE_METHODS_MATCH,
                        Strings.join( applicablePrototypeMethods, ", " )
                    );
                }
            }

            // or no @InheritInverseConfiguration
            if ( inverseTemplateMethod == null && inheritanceStrategy.isApplyReverse() ) {
                if ( applicableReversePrototypeMethods.size() == 1 ) {
                    mappingOptions.applyInheritedOptions(
                        method,
                        first( applicableReversePrototypeMethods ),
                        true,
                        annotationDescriptor
                    );
                }
                else if ( applicableReversePrototypeMethods.size() > 1 ) {
                    messager.printMessage(
                        method.getExecutableDescriptor(),
                        Message.INHERITINVERSECONFIGURATION_MULTIPLE_PROTOTYPE_METHODS_MATCH,
                        Strings.join( applicableReversePrototypeMethods, ", " )
                    );
                }
            }
        }

        // @BeanMapping( ignoreByDefault = true )
        if ( mappingOptions.getBeanMapping() != null && mappingOptions.getBeanMapping().isIgnoredByDefault() ) {
            mappingOptions.applyIgnoreAll( method, typeFactory, mappingContext.getMessager() );
        }

        mappingOptions.markAsFullyInitialized();
    }

    private void reportErrorIfNoImplementationTypeIsRegisteredForInterfaceReturnType(Method method) {
        if ( !method.getReturnType().isVoid() &&
            method.getReturnType().isInterface() &&
            method.getReturnType().getImplementationType() == null ) {
            messager.printMessage(
                method.getExecutableDescriptor(),
                Message.GENERAL_NO_IMPLEMENTATION,
                method.getReturnType()
            );
        }
    }

    /**
     * Returns the configuring inverse method's options in case the given method is annotated with
     * {@code @InheritInverseConfiguration} and exactly one such configuring method can unambiguously be selected (as
     * per the source/target type and optionally the name given via {@code @InheritInverseConfiguration}).
     */
    private SourceMethod getInverseTemplateMethod(List<SourceMethod> rawMethods, SourceMethod method,
                                                  List<SourceMethod> initializingMethods,
                                                  MapperOptions mapperConfig) {
        SourceMethod resultMethod = null;

        ExecutableDescriptor executable = method != null ? method.getExecutableDescriptor() : null;
        AnnotationDescriptor inverseConfigurationAnnotation = executable != null
            ? AnnotationDescriptorUtils.findAnnotation(
                langElements,
                executable,
                "org.mapstruct.InheritInverseConfiguration"
            ).orElse( null )
            : null;
        InheritInverseConfigurationGem inverseConfiguration =
            annotationGems.inheritInverseConfiguration( inverseConfigurationAnnotation );

        if ( inverseConfiguration != null ) {

            // method is configured as being inverse method, collect candidates
            List<SourceMethod> candidates = new ArrayList<>();
            for ( SourceMethod oneMethod : rawMethods ) {
                if ( method.inverses( oneMethod ) ) {
                    candidates.add( oneMethod );
                }
            }

            String name = inverseConfiguration.name().get();
            if ( candidates.size() == 1 ) {
                // no ambiguity: if no configuredBy is specified, or configuredBy specified and match
                if ( name.isEmpty() ) {
                    resultMethod = candidates.get( 0 );
                }
                else if ( candidates.get( 0 ).getName().equals( name ) ) {
                    resultMethod = candidates.get( 0 );
                }
                else {
                    reportErrorWhenNonMatchingName(
                        candidates.get( 0 ),
                        method,
                        inverseConfiguration,
                        inverseConfigurationAnnotation
                    );
                }
            }
            else if ( candidates.size() > 1 ) {
                // ambiguity: find a matching method that matches configuredBy

                List<SourceMethod> nameFilteredcandidates = new ArrayList<>();
                for ( SourceMethod candidate : candidates ) {
                    if ( candidate.getName().equals( name ) ) {
                        nameFilteredcandidates.add( candidate );
                    }
                }

                if ( nameFilteredcandidates.size() == 1 ) {
                    resultMethod = nameFilteredcandidates.get( 0 );
                }
                else if ( nameFilteredcandidates.size() > 1 ) {
                    reportErrorWhenSeveralNamesMatch(
                        nameFilteredcandidates,
                        method,
                        inverseConfiguration,
                        inverseConfigurationAnnotation
                    );
                }
                else {
                    reportErrorWhenAmbiguousReverseMapping(
                        candidates,
                        method,
                        inverseConfiguration,
                        inverseConfigurationAnnotation
                    );
                }
            }
        }

        return extractInitializedOptions(
            resultMethod,
            rawMethods,
            mapperConfig,
            initializingMethods,
            inverseConfigurationAnnotation
        );
    }

    private SourceMethod extractInitializedOptions(SourceMethod resultMethod,
                                                   List<SourceMethod> rawMethods,
                                                   MapperOptions mapperConfig,
                                                   List<SourceMethod> initializingMethods,
                                                   AnnotationDescriptor annotationDescriptor) {
        if ( resultMethod != null ) {
            if ( !resultMethod.getOptions().isFullyInitialized() ) {
                mergeInheritedOptions( resultMethod, mapperConfig, rawMethods, initializingMethods,
                    annotationDescriptor );
            }

            return resultMethod;
        }

        return null;
    }

    /**
     * Returns the configuring forward method's options in case the given method is annotated with
     * {@code @InheritConfiguration} and exactly one such configuring method can unambiguously be selected (as per the
     * source/target type and optionally the name given via {@code @InheritConfiguration}). The method cannot be marked
     * forward mapping itself (hence 'other'). And neither can it contain an {@code @InheritReverseConfiguration}
     */
    private SourceMethod getForwardTemplateMethod(List<SourceMethod> rawMethods, SourceMethod method,
                                                  List<SourceMethod> initializingMethods,
                                                  MapperOptions mapperConfig) {
        SourceMethod resultMethod = null;

        ExecutableDescriptor executable = method != null ? method.getExecutableDescriptor() : null;
        AnnotationDescriptor inheritConfigurationAnnotation = executable != null
            ? AnnotationDescriptorUtils.findAnnotation(
                langElements,
                executable,
                "org.mapstruct.InheritConfiguration"
            ).orElse( null )
            : null;
        InheritConfigurationGem inheritConfiguration =
            annotationGems.inheritConfiguration( inheritConfigurationAnnotation );

        if ( inheritConfiguration != null ) {

            List<SourceMethod> candidates = new ArrayList<>();
            for ( SourceMethod oneMethod : rawMethods ) {
                // method must be similar but not equal
                if ( method.canInheritFrom( oneMethod ) && !( oneMethod.equals( method ) ) ) {
                    candidates.add( oneMethod );
                }
            }

            String name = inheritConfiguration.name().get();
            if ( candidates.size() == 1 ) {
                // no ambiguity: if no configuredBy is specified, or configuredBy specified and match
                SourceMethod sourceMethod = first( candidates );
                if ( name.isEmpty() ) {
                    resultMethod = sourceMethod;
                }
                else if ( sourceMethod.getName().equals( name ) ) {
                    resultMethod = sourceMethod;
                }
                else {
                    reportErrorWhenNonMatchingName(
                        sourceMethod,
                        method,
                        inheritConfiguration,
                        inheritConfigurationAnnotation
                    );
                }
            }
            else if ( candidates.size() > 1 ) {
                // ambiguity: find a matching method that matches configuredBy

                List<SourceMethod> nameFilteredCandidates = new ArrayList<>();
                for ( SourceMethod candidate : candidates ) {
                    if ( candidate.getName().equals( name ) ) {
                        nameFilteredCandidates.add( candidate );
                    }
                }

                if ( nameFilteredCandidates.size() == 1 ) {
                    resultMethod = first( nameFilteredCandidates );
                }
                else if ( nameFilteredCandidates.size() > 1 ) {
                    reportErrorWhenSeveralNamesMatch(
                        nameFilteredCandidates,
                        method,
                        inheritConfiguration,
                        inheritConfigurationAnnotation
                    );
                }
                else {
                    reportErrorWhenAmbiguousMapping(
                        candidates,
                        method,
                        inheritConfiguration,
                        inheritConfigurationAnnotation
                    );
                }
            }
        }

        return extractInitializedOptions(
            resultMethod,
            rawMethods,
            mapperConfig,
            initializingMethods,
            inheritConfigurationAnnotation
        );
    }

    private void reportErrorWhenAmbiguousReverseMapping(List<SourceMethod> candidates,
                                                        SourceMethod method,
                                                        InheritInverseConfigurationGem inverseGem,
                                                        AnnotationDescriptor inverseAnnotation) {

        List<String> candidateNames = new ArrayList<>();
        for ( SourceMethod candidate : candidates ) {
            candidateNames.add( candidate.getName() );
        }

        String name = inverseGem.name().get();
        if ( name.isEmpty() ) {
            messager.printMessage(
                method.getExecutableDescriptor(),
                inverseAnnotation,
                Message.INHERITINVERSECONFIGURATION_DUPLICATES,
                Strings.join( candidateNames, "(), " )

            );
        }
        else {
            messager.printMessage(
                method.getExecutableDescriptor(),
                inverseAnnotation,
                Message.INHERITINVERSECONFIGURATION_INVALID_NAME,
                Strings.join( candidateNames, "(), " ),
                name

            );
        }
    }

    private void reportErrorWhenSeveralNamesMatch(List<SourceMethod> candidates,
                                                  SourceMethod method,
                                                  InheritInverseConfigurationGem inverseGem,
                                                  AnnotationDescriptor inverseAnnotation) {

        messager.printMessage(
            method.getExecutableDescriptor(),
            inverseAnnotation,
            Message.INHERITINVERSECONFIGURATION_DUPLICATE_MATCHES,
            inverseGem.name().get(),
            Strings.join( candidates, ", " )

        );
    }

    private void reportErrorWhenNonMatchingName(SourceMethod onlyCandidate,
                                                SourceMethod method,
                                                InheritInverseConfigurationGem inverseGem,
                                                AnnotationDescriptor inverseAnnotation) {

        messager.printMessage(
            method.getExecutableDescriptor(),
            inverseAnnotation,
            Message.INHERITINVERSECONFIGURATION_NO_NAME_MATCH,
            inverseGem.name().get(),
            onlyCandidate.getName()
        );
    }

    private void reportErrorWhenAmbiguousMapping(List<SourceMethod> candidates,
                                                 SourceMethod method,
                                                 InheritConfigurationGem gem,
                                                 AnnotationDescriptor annotation) {

        List<String> candidateNames = new ArrayList<>();
        for ( SourceMethod candidate : candidates ) {
            candidateNames.add( candidate.getName() );
        }

        String name = gem.name().get();
        if ( name.isEmpty() ) {
            messager.printMessage(
                method.getExecutableDescriptor(),
                annotation,
                Message.INHERITCONFIGURATION_DUPLICATES,
                Strings.join( candidateNames, "(), " )
            );
        }
        else {
            messager.printMessage(
                method.getExecutableDescriptor(),
                annotation,
                Message.INHERITCONFIGURATION_INVALIDNAME,
                Strings.join( candidateNames, "(), " ),
                name
            );
        }
    }

    private void reportErrorWhenSeveralNamesMatch(List<SourceMethod> candidates,
                                                  SourceMethod method,
                                                  InheritConfigurationGem gem,
                                                  AnnotationDescriptor annotation) {

        messager.printMessage(
            method.getExecutableDescriptor(),
            annotation,
            Message.INHERITCONFIGURATION_DUPLICATE_MATCHES,
            gem.name().get(),
            Strings.join( candidates, ", " )
        );
    }

    private void reportErrorWhenNonMatchingName(SourceMethod onlyCandidate,
                                                SourceMethod method,
                                                InheritConfigurationGem gem,
                                                AnnotationDescriptor annotation) {

        messager.printMessage(
            method.getExecutableDescriptor(),
            annotation,
            Message.INHERITCONFIGURATION_NO_NAME_MATCH,
            gem.name().get(),
            onlyCandidate.getName()
        );
    }

    private boolean isConsistent(JavadocGem gem,
                                 TypeElementDescriptor element,
                                 AnnotationDescriptor annotation,
                                 FormattingMessager messager) {
        if ( !gem.value().hasValue()
            && !gem.authors().hasValue()
            && !gem.deprecated().hasValue()
            && !gem.since().hasValue() ) {
            if ( annotation != null ) {
                messager.printMessage( element, annotation, Message.JAVADOC_NO_ELEMENTS );
            }
            else {
                messager.printMessage( element, Message.JAVADOC_NO_ELEMENTS );
            }
            return false;
        }
        return true;
    }
}
