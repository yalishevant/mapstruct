/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mapstruct.ap.internal.model.common.Parameter;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.model.source.BeanMappingOptions;
import org.mapstruct.ap.internal.model.source.EnumMappingOptions;
import org.mapstruct.ap.internal.model.source.IterableMappingOptions;
import org.mapstruct.ap.internal.model.source.MapMappingOptions;
import org.mapstruct.ap.internal.model.source.MapperOptions;
import org.mapstruct.ap.internal.model.source.MappingOptions;
import org.mapstruct.ap.internal.model.source.ParameterProvidedMethods;
import org.mapstruct.ap.internal.model.source.SourceMethod;
import org.mapstruct.ap.internal.model.source.SubclassMappingOptions;
import org.mapstruct.ap.internal.model.source.SubclassValidator;
import org.mapstruct.ap.internal.option.Options;
import org.mapstruct.ap.internal.util.AccessorNamingUtils;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.spi.EnumTransformationStrategy;
import org.mapstruct.ap.descriptor.ElementDescriptor;
import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.langmodel.AnnotationGemFactory;
import org.mapstruct.ap.langmodel.AnnotationGemsCapability;
import org.mapstruct.ap.internal.langmodel.MissingLangModelCapabilityException;
import org.mapstruct.ap.langmodel.api.LangElements;
import org.mapstruct.ap.descriptor.LangModifier;
import org.mapstruct.ap.langmodel.LangModelContext;
import org.mapstruct.ap.langmodel.LangModelElementQuery;
import org.mapstruct.ap.langmodel.api.LangTypes;
import org.mapstruct.ap.langmodel.MapperAnnotation;
import org.mapstruct.ap.descriptor.ParameterDescriptor;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;

/**
 * A {@link ModelElementProcessor} which retrieves a list of {@link SourceMethod}s
 * representing all the mapping methods of the given bean mapper type as well as
 * all referenced mapper methods declared by other mappers referenced by the
 * current mapper.
 *
 * @author Gunnar Morling
 */
public class MethodRetrievalProcessor implements ModelElementProcessor<Void, List<SourceMethod>> {

    private FormattingMessager messager;
    private TypeFactory typeFactory;
    private AccessorNamingUtils accessorNaming;
    private Map<String, EnumTransformationStrategy> enumTransformationStrategies;
    private LangModelContext<?, ?, ?, ?> langModelContext;
    private LangModelElementQuery langElementQuery;
    private LangElements langElements;
    private LangTypes langTypes;
    private AnnotationGemFactory annotationGems;
    private Options options;

    private TypeElementDescriptor toTypeElementDescriptor(Type type) {
        if ( type == null ) {
            return null;
        }
        TypeDescriptor descriptor = type.getTypeDescriptor();
        return descriptor != null ? descriptor.typeElement().orElse( null ) : null;
    }

    @Override
    public List<SourceMethod> process(ProcessorContext context,
                                      TypeElementDescriptor mapperDescriptor,
                                      Void sourceModel) {
        this.messager = context.getMessager();
        this.typeFactory = context.getTypeFactory();
        this.accessorNaming = context.getAccessorNaming();
        this.langModelContext = context.getLangModelContext();
        this.langElementQuery = langModelContext.elementQuery();
        AnnotationGemsCapability annotationGemsCapability = langModelContext.optional( AnnotationGemsCapability.class )
            .orElseThrow( () -> MissingLangModelCapabilityException.required( AnnotationGemsCapability.class ) );
        this.annotationGems = annotationGemsCapability.annotationGems();
        this.langElements = langElementQuery.elements();
        this.langTypes = langModelContext.typeSystem().types();
        this.enumTransformationStrategies = context.getEnumTransformationStrategies();
        this.options = context.getOptions();

        String mapperName = mapperDescriptor != null
            ? mapperDescriptor.qualifiedName()
            : "<unknown mapper>";
        this.messager.note( 0, Message.PROCESSING_NOTE, mapperName );
        MapperAnnotation mapperAnnotation = langElementQuery.mapperAnnotation( mapperDescriptor );
        MapperOptions mapperOptions = MapperOptions.fromAnnotation(
            mapperAnnotation,
            mapperDescriptor,
            context.getOptions(),
            langModelContext
        );

        if ( !mapperOptions.isValid() ) {
            return Collections.emptyList();
        }

        if ( mapperOptions.hasMapperConfig() ) {
            TypeDescriptor configDescriptor = mapperOptions.mapperConfigType();
            TypeElementDescriptor configElement = configDescriptor != null
                ? configDescriptor.typeElement().orElse( null )
                : null;
            if ( configElement != null ) {
                this.messager.note( 0, Message.CONFIG_NOTE, configElement.simpleName().content() );
            }
        }

        TypeDescriptor mapperTypeDescriptor = mapperDescriptor != null ? mapperDescriptor.asType() : null;
        Type mapperType = mapperTypeDescriptor != null ? typeFactory.getType( mapperTypeDescriptor ) : null;
        List<SourceMethod> prototypeMethods = retrievePrototypeMethods( mapperDescriptor, mapperType, mapperOptions );
        return retrieveMethods(
            mapperType,
            mapperType,
            mapperDescriptor,
            mapperOptions,
            prototypeMethods
        );
    }

    @Override
    public int getPriority() {
        return 1;
    }

    private List<SourceMethod> retrievePrototypeMethods(TypeElementDescriptor mapperDescriptor,
            Type mapperType,
            MapperOptions mapperOptions) {
        if ( !mapperOptions.hasMapperConfig() ) {
            return Collections.emptyList();
        }

        TypeDescriptor mapperConfigDescriptor = mapperOptions.mapperConfigType();
        if ( mapperConfigDescriptor == null ) {
            return Collections.emptyList();
        }
        TypeElementDescriptor configElement = mapperConfigDescriptor.typeElement().orElse( null );
        if ( configElement == null ) {
            return Collections.emptyList();
        }

        List<SourceMethod> methods = new ArrayList<>();
        for ( ExecutableDescriptor executable : langElements.enclosedExecutables( configElement ) ) {
            if ( executable.isDefault() || executable.modifiers().contains( LangModifier.STATIC ) ) {
                continue;
            }

            List<Parameter> parameters = typeFactory.getParameters( mapperConfigDescriptor, executable );
            boolean containsTargetTypeParameter = SourceMethod.containsTargetTypeParameter( parameters );

            List<SourceMethod> prototypeMethods = Collections.emptyList();

            Type returnType = typeFactory.getReturnType( mapperConfigDescriptor, executable );
            List<Type> exceptionTypes = typeFactory.getThrownTypes( mapperConfigDescriptor, executable );

            SourceMethod method =
                getMethodRequiringImplementation(
                    executable,
                    parameters,
                    returnType,
                    exceptionTypes,
                    containsTargetTypeParameter,
                    mapperOptions,
                    prototypeMethods,
                    mapperType,
                    mapperDescriptor
                );

            if ( method != null ) {
                methods.add( method );
            }
        }

        return methods;
    }

    /**
     * Retrieves the mapping methods declared by the given mapper type.
     *
     * @param usedMapperType The type of interest (either the mapper to implement, a used mapper via @uses annotation,
     *                   or a parameter type annotated with @Context)
     * @param mapperToImplement the top level type (mapper) that requires implementation
     * @param mapperOptions the mapper config
     * @param prototypeMethods prototype methods defined in mapper config type
     * @return All mapping methods declared by the given type
     */
    private List<SourceMethod> retrieveMethods(Type usedMapperType,
                                               Type mapperToImplementType,
                                               TypeElementDescriptor mapperToImplementDescriptor,
                                               MapperOptions mapperOptions,
                                               List<SourceMethod> prototypeMethods) {
        if ( usedMapperType == null ) {
            return Collections.emptyList();
        }

        List<SourceMethod> methods = new ArrayList<>();

        TypeElementDescriptor usedMapperDescriptor = toTypeElementDescriptor( usedMapperType );

        List<ExecutableDescriptor> executables = usedMapperDescriptor != null
            ? langElements.enclosedExecutables( usedMapperDescriptor )
            : java.util.Collections.emptyList();

        for ( ExecutableDescriptor executable : executables ) {
            SourceMethod method = getMethod(
                usedMapperType,
                executable,
                mapperToImplementType,
                mapperToImplementDescriptor,
                mapperOptions,
                prototypeMethods );

            if ( method != null ) {
                methods.add( method );
            }
        }

        //Add all methods of used mappers in order to reference them in the aggregated model
        if ( mapperToImplementType != null && mapperToImplementType.equals( usedMapperType ) ) {
            for ( TypeDescriptor mapper : mapperOptions.uses() ) {
                Type usedMapperReference = typeFactory.getType( mapper );
                if ( usedMapperReference == null ) {
                    continue;
                }
                if ( !usedMapperReference.equals( mapperToImplementType ) ) {
                    methods.addAll( retrieveMethods(
                        usedMapperReference,
                        mapperToImplementType,
                        mapperToImplementDescriptor,
                        mapperOptions,
                        prototypeMethods ) );
                }
                else {
                    String mapperName = mapperToImplementDescriptor != null
                        ? mapperToImplementDescriptor.qualifiedName()
                        : String.valueOf( mapperToImplementType );
                    messager.printMessage(
                        mapperToImplementDescriptor,
                        mapperOptions.annotation(),
                        Message.RETRIEVAL_MAPPER_USES_CYCLE,
                        mapperName
                    );
                }
            }
        }

        return methods;
    }

    private SourceMethod getMethod(Type usedMapperType,
                                   ExecutableDescriptor methodDescriptor,
                                   Type mapperToImplementType,
                                   TypeElementDescriptor mapperToImplementDescriptor,
                                   MapperOptions mapperOptions,
                                   List<SourceMethod> prototypeMethods) {
        List<Parameter> parameters = typeFactory.getParameters( usedMapperType, methodDescriptor );
        Type returnType = typeFactory.getReturnType( usedMapperType, methodDescriptor );
        List<Type> exceptionTypes = typeFactory.getThrownTypes( usedMapperType, methodDescriptor );

        boolean methodRequiresImplementation = methodDescriptor.modifiers().contains( LangModifier.ABSTRACT );
        boolean containsTargetTypeParameter = SourceMethod.containsTargetTypeParameter( parameters );

        Type definingType = methodDescriptor.enclosingElement()
            .map( ElementDescriptor::asType )
            .map( typeFactory::fromDescriptor )
            .orElse( null );

        //add method with property mappings if an implementation needs to be generated
        if ( mapperToImplementType != null
            && mapperToImplementType.equals( usedMapperType )
            && methodRequiresImplementation ) {
            return getMethodRequiringImplementation(
                methodDescriptor,
                parameters,
                returnType,
                exceptionTypes,
                containsTargetTypeParameter,
                mapperOptions,
                prototypeMethods,
                mapperToImplementType,
                mapperToImplementDescriptor );
        }
        // otherwise add reference to existing mapper method
        else if ( isValidReferencedMethod( parameters )
            || isValidFactoryMethod( methodDescriptor, parameters, returnType )
            || isValidLifecycleCallbackMethod( methodDescriptor )
            || isValidPresenceCheckMethod( methodDescriptor, parameters, returnType ) ) {
            return getReferencedMethod(
                usedMapperType,
                methodDescriptor,
                mapperToImplementType,
                mapperToImplementDescriptor,
                parameters,
                returnType,
                exceptionTypes,
                definingType );
        }
        else {
            return null;
        }
    }

    private SourceMethod getMethodRequiringImplementation(ExecutableDescriptor methodDescriptor,
            List<Parameter> parameters,
            Type returnType,
            List<Type> exceptionTypes,
            boolean containsTargetTypeParameter,
            MapperOptions mapperOptions,
            List<SourceMethod> prototypeMethods,
            Type mapperToImplementType,
            TypeElementDescriptor mapperToImplementDescriptor) {
        List<Parameter> sourceParameters = Parameter.getSourceParameters( parameters );
        List<Parameter> contextParameters = Parameter.getContextParameters( parameters );
        Parameter targetParameter = extractTargetParameter( parameters );
        Type resultType = selectResultType( returnType, targetParameter );

        boolean isValid = checkParameterAndReturnType(
            methodDescriptor,
            sourceParameters,
            targetParameter,
            contextParameters,
            resultType,
            returnType,
            containsTargetTypeParameter
        );

        if ( !isValid ) {
            return null;
        }

        ParameterProvidedMethods contextProvidedMethods = retrieveContextProvidedMethods(
            contextParameters,
            mapperToImplementType,
            mapperToImplementDescriptor,
            mapperOptions
        );

        BeanMappingOptions beanMappingOptions = ExecutableDescriptorBridge.beanMappingOptions(
            methodDescriptor,
            mapperOptions,
            langElements,
            annotationGems,
            messager,
            typeFactory
        );

        Set<MappingOptions> mappingOptions = ExecutableDescriptorBridge.mappingOptions(
            methodDescriptor,
            beanMappingOptions,
            langElements,
            annotationGems,
            messager,
            typeFactory
        );

        IterableMappingOptions iterableMappingOptions = ExecutableDescriptorBridge.iterableMappingOptions(
            methodDescriptor,
            mapperOptions,
            langElements,
            annotationGems,
            messager,
            typeFactory
        );

        MapMappingOptions mapMappingOptions = ExecutableDescriptorBridge.mapMappingOptions(
            methodDescriptor,
            mapperOptions,
            langElements,
            annotationGems,
            messager,
            typeFactory
        );

        EnumMappingOptions enumMappingOptions = ExecutableDescriptorBridge.enumMappingOptions(
            methodDescriptor,
            mapperOptions,
            langElements,
            annotationGems,
            messager,
            enumTransformationStrategies
        );

        // We want to get as much error reporting as possible.
        // If targetParameter is not null it means we have an update method
        SubclassValidator subclassValidator = new SubclassValidator( messager, langTypes );
        Set<SubclassMappingOptions> subclassMappingOptions = ExecutableDescriptorBridge.subclassMappings(
            methodDescriptor,
            sourceParameters,
            targetParameter != null ? null : resultType,
            beanMappingOptions,
            typeFactory,
            subclassValidator,
            langElements,
            annotationGems,
            messager
        );

        return new SourceMethod.Builder()
            .setExecutable( methodDescriptor )
            .setParameters( parameters )
            .setReturnType( returnType )
            .setExceptionTypes( exceptionTypes )
            .setMapper( mapperOptions )
            .setBeanMappingOptions( beanMappingOptions )
            .setMappingOptions( mappingOptions )
            .setIterableMappingOptions( iterableMappingOptions )
            .setMapMappingOptions( mapMappingOptions )
            .setValueMappingOptionss( ExecutableDescriptorBridge.valueMappings(
                methodDescriptor,
                langElements,
                annotationGems,
                messager
            ) )
            .setEnumMappingOptions( enumMappingOptions )
            .setSubclassMappings( subclassMappingOptions )
            .setSubclassValidator( subclassValidator )
            .setTypeFactory( typeFactory )
            .setPrototypeMethods( prototypeMethods )
            .setContextProvidedMethods( contextProvidedMethods )
            .setVerboseLogging( options.isVerbose() )
            .build();
    }

    private ParameterProvidedMethods retrieveContextProvidedMethods(
            List<Parameter> contextParameters,
            Type mapperToImplementType,
            TypeElementDescriptor mapperToImplementDescriptor,
            MapperOptions mapperConfig) {

        ParameterProvidedMethods.Builder builder = ParameterProvidedMethods.builder();
        for ( Parameter contextParam : contextParameters ) {
            if ( contextParam.getType().isPrimitive() || contextParam.getType().isArrayType() ) {
                continue;
            }
            List<SourceMethod> contextParamMethods = retrieveMethods(
                contextParam.getType(),
                mapperToImplementType,
                mapperToImplementDescriptor,
                mapperConfig,
                Collections.emptyList() );

            List<SourceMethod> contextProvidedMethods = new ArrayList<>( contextParamMethods.size() );
            for ( SourceMethod sourceMethod : contextParamMethods ) {
                if ( sourceMethod.isLifecycleCallbackMethod() || sourceMethod.isObjectFactory()
                    || sourceMethod.getConditionOptions().isAnyStrategyApplicable() ) {
                    contextProvidedMethods.add( sourceMethod );
                }
            }

            builder.addMethodsForParameter( contextParam, contextProvidedMethods );
        }

        return builder.build();
    }

    private SourceMethod getReferencedMethod(Type usedMapperType,
                                             ExecutableDescriptor methodDescriptor,
                                             Type mapperToImplementType,
                                             TypeElementDescriptor mapperToImplementDescriptor,
                                             List<Parameter> parameters,
                                             Type returnType,
                                             List<Type> exceptionTypes,
                                             Type definingType) {
        Type usedMapperAsType = usedMapperType;

        if ( mapperToImplementType != null && !mapperToImplementType.canAccess( usedMapperAsType, methodDescriptor ) ) {
            return null;
        }

        Type declaringMapper = null;
        if ( mapperToImplementType == null || !usedMapperAsType.equals( mapperToImplementType ) ) {
            declaringMapper = usedMapperAsType;
        }

        return new SourceMethod.Builder()
            .setDeclaringMapper( declaringMapper )
            .setDefiningType( definingType )
            .setExecutable( methodDescriptor )
            .setParameters( parameters )
            .setReturnType( returnType )
            .setExceptionTypes( exceptionTypes )
            .setTypeFactory( typeFactory )
            .setConditionOptions(
                ExecutableDescriptorBridge.conditionOptions(
                    methodDescriptor,
                    parameters,
                    langElements,
                    annotationGems,
                    messager
                )
            )
            .setVerboseLogging( options.isVerbose() )
            .build();
    }

    private boolean isValidLifecycleCallbackMethod(ExecutableDescriptor methodDescriptor) {
        return ExecutableDescriptorBridge.isLifecycleCallbackMethod( methodDescriptor, langElements );
    }

    private boolean isValidReferencedMethod(List<Parameter> parameters) {
        return isValidReferencedOrFactoryMethod( 1, 1, parameters );
    }

    private boolean isValidFactoryMethod(ExecutableDescriptor methodDescriptor,
                                         List<Parameter> parameters,
                                         Type returnType) {
        return !isVoid( returnType )
            && ( isValidReferencedOrFactoryMethod( 0, 0, parameters ) || hasFactoryAnnotation( methodDescriptor ) );
    }

    private boolean hasFactoryAnnotation(ExecutableDescriptor methodDescriptor) {
        return ExecutableDescriptorBridge.hasFactoryAnnotation( methodDescriptor, langElements );
    }

    private boolean isValidPresenceCheckMethod(ExecutableDescriptor methodDescriptor,
                                               List<Parameter> parameters,
                                               Type returnType) {
        for ( Parameter param : parameters ) {

            if ( param.isSourcePropertyName() && !param.getType().isString() ) {
                messager.printMessage(
                    param.getElement(),
                    parameterAnnotation( param, "org.mapstruct.SourcePropertyName" ),
                    Message.RETRIEVAL_SOURCE_PROPERTY_NAME_WRONG_TYPE
                );
                return false;
            }

            if ( param.isTargetPropertyName() && !param.getType().isString() ) {
                messager.printMessage(
                    param.getElement(),
                    parameterAnnotation( param, "org.mapstruct.TargetPropertyName" ),
                    Message.RETRIEVAL_TARGET_PROPERTY_NAME_WRONG_TYPE
                );
                return false;
            }
        }
        return isBoolean( returnType ) && hasConditionAnnotation( methodDescriptor );
    }

    private AnnotationDescriptor parameterAnnotation(Parameter parameter, String annotationFqn) {
        ParameterDescriptor descriptor = parameter.getDescriptor();
        if ( descriptor == null ) {
            return null;
        }
        return AnnotationDescriptorUtils.findAnnotation( langElements, descriptor, annotationFqn ).orElse( null );
    }

    private boolean hasConditionAnnotation(ExecutableDescriptor methodDescriptor) {
        return ExecutableDescriptorBridge.hasConditionAnnotation( methodDescriptor, langElements );
    }

    private boolean isVoid(Type returnType) {
        return returnType != null && returnType.isVoid();
    }

    private boolean isBoolean(Type returnType) {
        return Boolean.class.getCanonicalName().equals( returnType.getBoxedEquivalent().getFullyQualifiedName() );
    }

    private boolean isValidReferencedOrFactoryMethod(int sourceParamCount, int targetParamCount,
                                                     List<Parameter> parameters) {
        int validSourceParameters = 0;
        int targetParameters = 0;
        int targetTypeParameters = 0;

        for ( Parameter param : parameters ) {
            if ( param.isMappingTarget() ) {
                targetParameters++;
            }
            else if ( param.isTargetType() ) {
                targetTypeParameters++;
            }
            else if ( !param.isMappingContext() ) {
                validSourceParameters++;
            }
        }

        return validSourceParameters == sourceParamCount
            && targetParameters <= targetParamCount
            && targetTypeParameters <= 1;
    }

    private Parameter extractTargetParameter(List<Parameter> parameters) {
        for ( Parameter param : parameters ) {
            if ( param.isMappingTarget() ) {
                return param;
            }
        }

        return null;
    }

    private Type selectResultType(Type returnType, Parameter targetParameter) {
        if ( null != targetParameter ) {
            return targetParameter.getType();
        }
        else {
            return returnType;
        }
    }

    private boolean checkParameterAndReturnType(ExecutableDescriptor methodDescriptor, List<Parameter> sourceParameters,
                                                Parameter targetParameter, List<Parameter> contextParameters,
                                                Type resultType, Type returnType, boolean containsTargetTypeParameter) {
        if ( sourceParameters.isEmpty() ) {
            messager.printMessage( methodDescriptor, Message.RETRIEVAL_NO_INPUT_ARGS );
            return false;
        }

        if ( targetParameter != null
            && ( sourceParameters.size() + contextParameters.size() + 1 != methodDescriptor.parameters().size() ) ) {
            messager.printMessage( methodDescriptor, Message.RETRIEVAL_DUPLICATE_MAPPING_TARGETS );
            return false;
        }

        if ( isVoid( resultType ) ) {
            messager.printMessage( methodDescriptor, Message.RETRIEVAL_VOID_MAPPING_METHOD );
            return false;
        }

        if ( returnType != null && !returnType.isVoid() &&
                        !resultType.isAssignableTo( returnType ) &&
                        !resultType.isAssignableTo( typeFactory.effectiveResultTypeFor( returnType, null ) ) ) {
            messager.printMessage( methodDescriptor, Message.RETRIEVAL_NON_ASSIGNABLE_RESULTTYPE );
            return false;
        }

        for ( Parameter sourceParameter : sourceParameters ) {
            if ( sourceParameter.getType().isTypeVar() ) {
                messager.printMessage( methodDescriptor, Message.RETRIEVAL_TYPE_VAR_SOURCE );
                return false;
            }
        }

        Set<Type> contextParameterTypes = new HashSet<>();
        for ( Parameter contextParameter : contextParameters ) {
            if ( !contextParameterTypes.add( contextParameter.getType() ) ) {
                messager.printMessage( methodDescriptor, Message.RETRIEVAL_CONTEXT_PARAMS_WITH_SAME_TYPE );
                return false;
            }
        }

        if ( returnType.isTypeVar() || resultType.isTypeVar() ) {
                messager.printMessage( methodDescriptor, Message.RETRIEVAL_TYPE_VAR_RESULT );
                return false;
        }

        if ( sourceParameters.size() == 1 ) {
            Type parameterType = sourceParameters.get( 0 ).getType();

            if ( isStreamTypeOrIterableFromJavaStdLib( parameterType ) && !resultType.isIterableOrStreamType() ) {
                messager.printMessage( methodDescriptor, Message.RETRIEVAL_ITERABLE_TO_NON_ITERABLE );
                return false;
            }

            if ( !parameterType.isIterableOrStreamType() && isStreamTypeOrIterableFromJavaStdLib( resultType ) ) {
                messager.printMessage( methodDescriptor, Message.RETRIEVAL_NON_ITERABLE_TO_ITERABLE );
                return false;
            }

            if ( !parameterType.isIterableOrStreamType() && resultType.isArrayType() ) {
                messager.printMessage( methodDescriptor, Message.RETRIEVAL_NON_ITERABLE_TO_ARRAY );
                return false;
            }

            if ( parameterType.isPrimitive() ) {
                messager.printMessage( methodDescriptor, Message.RETRIEVAL_PRIMITIVE_PARAMETER );
                return false;
            }

            for ( Type typeParameter : parameterType.getTypeParameters() ) {
                if ( typeParameter.hasSuperBound() ) {
                    messager.printMessage( methodDescriptor, Message.RETRIEVAL_WILDCARD_SUPER_BOUND_SOURCE );
                    return false;
                }

                if ( typeParameter.isTypeVar() ) {
                    messager.printMessage( methodDescriptor, Message.RETRIEVAL_TYPE_VAR_SOURCE );
                    return false;
                }
            }
        }

        if ( containsTargetTypeParameter ) {
            messager.printMessage( methodDescriptor, Message.RETRIEVAL_MAPPING_HAS_TARGET_TYPE_PARAMETER );
            return false;
        }

        if ( resultType.isPrimitive() ) {
            messager.printMessage( methodDescriptor, Message.RETRIEVAL_PRIMITIVE_RETURN );
            return false;
        }

        for ( Type typeParameter : resultType.getTypeParameters() ) {
            if ( typeParameter.isTypeVar() ) {
                messager.printMessage( methodDescriptor, Message.RETRIEVAL_TYPE_VAR_RESULT );
                return false;
            }
            if ( typeParameter.hasExtendsBound() ) {
                messager.printMessage( methodDescriptor, Message.RETRIEVAL_WILDCARD_EXTENDS_BOUND_RESULT );
                return false;
            }
        }

        if ( ExecutableDescriptorBridge.isAfterMappingMethod( methodDescriptor, langElements ) ) {
            messager.printMessage( methodDescriptor, Message.RETRIEVAL_AFTER_METHOD_NOT_IMPLEMENTED );
            return false;
        }

        if ( ExecutableDescriptorBridge.isBeforeMappingMethod( methodDescriptor, langElements ) ) {
            messager.printMessage( methodDescriptor, Message.RETRIEVAL_BEFORE_METHOD_NOT_IMPLEMENTED );
            return false;
        }

        return true;
    }

    private boolean isStreamTypeOrIterableFromJavaStdLib(Type type) {
        return type.isStreamType() || ( type.isIterableType() && type.isJavaLangType() );
    }

    /**
     * Retrieves the mappings configured via {@code @Mapping} from the given method.
     *
     * @param method The method of interest
     * @param beanMapping options coming from bean mapping method
     * @return The mappings for the given method, keyed by target property name
     */
}
