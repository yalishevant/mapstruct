/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.util.AnnotationProcessingException;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.JavaCollectionConstants;
import org.mapstruct.ap.internal.util.JavaStreamConstants;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.util.RoundContext;
import org.mapstruct.ap.internal.util.accessor.Accessor;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.langmodel.AnnotationGemFactory;
import org.mapstruct.ap.langmodel.AnnotationGemsCapability;
import org.mapstruct.ap.descriptor.BuilderDescriptor;
import org.mapstruct.ap.spi.lang.BuilderIntrospector;
import org.mapstruct.ap.langmodel.BuilderIntrospectorCapability;
import org.mapstruct.ap.descriptor.ElementDescriptor;
import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.langmodel.ExecutableSignature;
import org.mapstruct.ap.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.MissingLangModelCapabilityException;
import org.mapstruct.ap.langmodel.LangModelContext;
import org.mapstruct.ap.langmodel.LangModelElementQuery;
import org.mapstruct.ap.langmodel.LangModelTypeSystem;
import org.mapstruct.ap.descriptor.LangTypeKind;
import org.mapstruct.ap.langmodel.api.LangTypes;
import org.mapstruct.ap.descriptor.ParameterDescriptor;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.langmodel.TypeIntrospector;
import org.mapstruct.ap.spi.BuilderInfo;
import org.mapstruct.ap.spi.MoreThanOneBuilderCreationMethodException;

import static org.mapstruct.ap.internal.model.common.ImplementationType.withDefaultConstructor;
import static org.mapstruct.ap.internal.model.common.ImplementationType.withFactoryMethod;
import static org.mapstruct.ap.internal.model.common.ImplementationType.withInitialCapacity;
import static org.mapstruct.ap.internal.model.common.ImplementationType.withLoadFactorAdjustment;

/**
 * Factory creating {@link Type} instances.
 *
 * @author Gunnar Morling
 */
public class TypeFactory {

    private static final String LINKED_HASH_SET_FACTORY_METHOD_NAME = "newLinkedHashSet";
    private static final String LINKED_HASH_MAP_FACTORY_METHOD_NAME = "newLinkedHashMap";

    private final LangModelContext<?, ?, ?, ?> langModelContext;
    private final LangModelTypeSystem<?, ?, ?, ?> typeSystem;
    private final LangModelElementQuery elementQuery;
    private final LangTypes langTypes;
    private final LangElements langElements;
    private final LangDescriptorFactory descriptorFactory;
    private final TypeIntrospector typeIntrospector;
    private final BuilderIntrospector builderIntrospector;
    private final AnnotationGemFactory annotationGemFactory;
    private final FormattingMessager messager;
    private final RoundContext roundContext;

    private final TypeDescriptor iterableType;
    private final TypeDescriptor collectionType;
    private final TypeDescriptor mapType;
    private final TypeDescriptor streamType;

    private final Map<String, ImplementationType> implementationTypes = new HashMap<>();
    private final Map<String, String> toBeImportedTypes = new HashMap<>();
    private final Map<String, String> notToBeImportedTypes;
    private final boolean loggingVerbose;

    public TypeFactory(LangModelContext<?, ?, ?, ?> langModelContext,
                       FormattingMessager messager,
                       RoundContext roundContext, Map<String, String> notToBeImportedTypes, boolean loggingVerbose,
                       VersionInformation versionInformation) {
        this.langModelContext = langModelContext;
        this.typeSystem = langModelContext.typeSystem();
        this.elementQuery = langModelContext.elementQuery();
        this.langTypes = typeSystem.types();
        this.langElements = elementQuery.elements();
        @SuppressWarnings("unchecked")
        LangDescriptorFactory<Object, Object, Object, Object> descriptorFactory =
            (LangDescriptorFactory<Object, Object, Object, Object>) typeSystem.descriptors();
        this.descriptorFactory = descriptorFactory;
        this.typeIntrospector = typeSystem.typeIntrospector();
        BuilderIntrospectorCapability builderIntrospectorCapability =
            langModelContext.optional( BuilderIntrospectorCapability.class ).orElse( null );
        if ( builderIntrospectorCapability != null ) {
            this.builderIntrospector = builderIntrospectorCapability.builderIntrospector(
                roundContext.getAnnotationProcessorContext()
            );
        }
        else {
            messager.printMessage(
                Message.OPTIONAL_CAPABILITY_MISSING,
                "BuilderIntrospectorCapability",
                "Builder-based mappings"
            );
            this.builderIntrospector = descriptor -> null;
        }
        AnnotationGemsCapability annotationGemsCapability =
            langModelContext.optional( AnnotationGemsCapability.class )
                .orElseThrow( () -> MissingLangModelCapabilityException.required( AnnotationGemsCapability.class ) );
        this.annotationGemFactory = annotationGemsCapability.annotationGems();
        this.messager = messager;
        this.roundContext = roundContext;
        this.notToBeImportedTypes = notToBeImportedTypes;

        TypeElementDescriptor iterableElement = langElements.typeElement( Iterable.class.getCanonicalName() );
        iterableType = iterableElement == null ? null : langTypes.erasure( iterableElement.asType() );
        TypeElementDescriptor collectionElement = langElements.typeElement( Collection.class.getCanonicalName() );
        collectionType = collectionElement == null ? null : langTypes.erasure( collectionElement.asType() );
        TypeElementDescriptor mapElement = langElements.typeElement( Map.class.getCanonicalName() );
        mapType = mapElement == null ? null : langTypes.erasure( mapElement.asType() );
        TypeElementDescriptor streamElement = langElements.typeElement( JavaStreamConstants.STREAM_FQN );
        streamType = streamElement == null ? null : langTypes.erasure( streamElement.asType() );

        implementationTypes.put( Iterable.class.getName(), withInitialCapacity( getType( ArrayList.class ) ) );
        implementationTypes.put( Collection.class.getName(), withInitialCapacity( getType( ArrayList.class ) ) );
        implementationTypes.put( List.class.getName(), withInitialCapacity( getType( ArrayList.class ) ) );

        boolean sourceVersionAtLeast19 = versionInformation.isSourceVersionAtLeast19();
        implementationTypes.put(
            Set.class.getName(),
            sourceVersionAtLeast19 ?
                withFactoryMethod( getType( LinkedHashSet.class ), LINKED_HASH_SET_FACTORY_METHOD_NAME ) :
                withLoadFactorAdjustment( getType( LinkedHashSet.class ) )
        );
        implementationTypes.put( SortedSet.class.getName(), withDefaultConstructor( getType( TreeSet.class ) ) );
        implementationTypes.put( NavigableSet.class.getName(), withDefaultConstructor( getType( TreeSet.class ) ) );

        implementationTypes.put(
            Map.class.getName(),
            sourceVersionAtLeast19 ?
                withFactoryMethod( getType( LinkedHashMap.class ), LINKED_HASH_MAP_FACTORY_METHOD_NAME ) :
                withLoadFactorAdjustment( getType( LinkedHashMap.class ) )
        );
        implementationTypes.put( SortedMap.class.getName(), withDefaultConstructor( getType( TreeMap.class ) ) );
        implementationTypes.put( NavigableMap.class.getName(), withDefaultConstructor( getType( TreeMap.class ) ) );
        implementationTypes.put(
            ConcurrentMap.class.getName(),
            withLoadFactorAdjustment( getType( ConcurrentHashMap.class ) )
        );
        implementationTypes.put(
            ConcurrentNavigableMap.class.getName(),
            withDefaultConstructor( getType( ConcurrentSkipListMap.class ) )
        );
        implementationTypes.put(
            JavaCollectionConstants.SEQUENCED_SET_FQN,
            sourceVersionAtLeast19 ?
                withFactoryMethod( getType( LinkedHashSet.class ), LINKED_HASH_SET_FACTORY_METHOD_NAME ) :
                withLoadFactorAdjustment( getType( LinkedHashSet.class ) )
        );
        implementationTypes.put(
            JavaCollectionConstants.SEQUENCED_MAP_FQN,
            sourceVersionAtLeast19 ?
                withFactoryMethod( getType( LinkedHashMap.class ), LINKED_HASH_MAP_FACTORY_METHOD_NAME ) :
                withLoadFactorAdjustment( getType( LinkedHashMap.class ) )
        );

        this.loggingVerbose = loggingVerbose;
    }

    public Type getTypeForLiteral(Class<?> type) {
        TypeDescriptor descriptor = type.isPrimitive()
            ? langTypes.primitive( type.getName() )
            : requireTypeElementDescriptor( type.getCanonicalName() ).asType();
        return getTypeInternal( descriptor, true, null );
    }

    public Type getType(Class<?> type) {
        TypeDescriptor descriptor = type.isPrimitive()
            ? langTypes.primitive( type.getName() )
            : requireTypeElementDescriptor( type.getCanonicalName() ).asType();
        return getType( descriptor );
    }

    public Type fromDescriptor(TypeDescriptor descriptor) {
        return getType( descriptor );
    }

    public Type getType(String canonicalName) {
        TypeElementDescriptor descriptor = requireTypeElementDescriptor( canonicalName );
        return getType( descriptor.asType() );
    }

    /**
     * Determines if the type with the given full qualified name is part of the classpath
     *
     * @param canonicalName Name of the type to be checked for availability
     * @return true if the type with the given full qualified name is part of the classpath.
     */
    public boolean isTypeAvailable(String canonicalName) {
        return langElements.typeElement( canonicalName ) != null;
    }

    public Type getWrappedType(Type type ) {
        Type result = type;
        if ( type.isPrimitive() ) {
            TypeDescriptor boxedDescriptor = langTypes.boxed( type.getTypeDescriptor() );
            result = getType( boxedDescriptor );
        }
        return result;
    }

    public Type getType(TypeDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        return getTypeInternal( descriptor, false, null );
    }

    public Type getAlwaysImportedType(TypeDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        return getTypeInternal( descriptor, false, Boolean.TRUE );
    }

    public LangElements getLangElements() {
        return langElements;
    }

    public LangTypes langTypes() {
        return langTypes;
    }

    public LangElements langElements() {
        return langElements;
    }

    public LangDescriptorFactory getDescriptorFactory() {
        return descriptorFactory;
    }

    public AnnotationGemFactory annotationGems() {
        return annotationGemFactory;
    }

    private Type getType(TypeDescriptor descriptor, boolean isLiteral, Boolean alwaysImport) {
        if ( descriptor == null ) {
            return null;
        }
        return getTypeInternal( descriptor, isLiteral, alwaysImport );
    }

    private TypeElementDescriptor requireTypeElementDescriptor(String canonicalName) {
        TypeElementDescriptor descriptor = langElements.typeElement( canonicalName );
        if ( descriptor == null ) {
            throw new AnnotationProcessingException(
                "Couldn't find type " + canonicalName + ". Are you missing a dependency on your classpath?"
            );
        }
        return descriptor;
    }

    private Type getTypeInternal(TypeDescriptor descriptor, boolean isLiteral, Boolean alwaysImport) {
        if ( descriptor == null ) {
            return null;
        }

        if ( !roundContext.canBeProcessed( descriptor ) ) {
            throw roundContext.typeHierarchyErroneousException( descriptor );
        }

        TypeIntrospector.Metadata metadata = typeIntrospector.describe( descriptor );

        ImplementationType implementationType = getImplementationType( descriptor );

        boolean isIterableType = isIterableDescriptor( descriptor );
        boolean isCollectionType = isCollectionDescriptor( descriptor );
        boolean isMapType = isMapDescriptor( descriptor );
        boolean isStreamType = isStreamDescriptor( descriptor );

        boolean isEnumType = metadata.isEnumType();
        boolean isInterface = metadata.isInterfaceType();

        String name = sanitizeDisplayName( metadata.simpleName().orElse( descriptor.displayName() ) );
        String packageName = metadata.packageName().orElse( null );
        String qualifiedName = sanitizeDisplayName( metadata.qualifiedName().orElse( name ) );

        TypeElementDescriptor typeElementDescriptor = metadata.typeElement().orElse( null );
        Type componentType = metadata.componentType()
            .map( this::getType )
            .orElse( null );

        Boolean toBeImported = alwaysImport != null
            ? alwaysImport
            : determineImportHint( descriptor, metadata );

        return new Type(
            this,
            roundContext.getAnnotationProcessorContext().getAccessorNaming(),
            descriptor,
            typeElementDescriptor,
            getTypeParameters( descriptor, false ),
            implementationType,
            componentType,
            packageName,
            name,
            qualifiedName,
            isInterface,
            isEnumType,
            isIterableType,
            isCollectionType,
            isMapType,
            isStreamType,
            toBeImportedTypes,
            notToBeImportedTypes,
            toBeImported,
            isLiteral,
            loggingVerbose,
            typeIntrospector,
            metadata
        );
    }

    private String sanitizeDisplayName(String name) {
        if ( name == null || name.indexOf( '@' ) < 0 ) {
            return name;
        }

        StringBuilder sanitized = new StringBuilder( name.length() );
        int length = name.length();
        int index = 0;

        while ( index < length ) {
            char current = name.charAt( index );
            if ( current == '@' ) {
                index++;
                int parenLevel = 0;
                while ( index < length ) {
                    char ch = name.charAt( index );
                    if ( ch == '(' ) {
                        parenLevel++;
                        index++;
                    }
                    else if ( ch == ')' ) {
                        if ( parenLevel > 0 ) {
                            parenLevel--;
                            index++;
                        }
                        else {
                            index++;
                            break;
                        }
                    }
                    else if ( parenLevel == 0
                        && ( Character.isWhitespace( ch ) || ch == '[' || ch == '<' || ch == ',' || ch == '>' ) ) {
                        break;
                    }
                    else {
                        index++;
                    }
                }

                while ( index < length && Character.isWhitespace( name.charAt( index ) ) ) {
                    index++;
                }
            }
            else {
                sanitized.append( current );
                index++;
            }
        }

        return sanitized.toString();
    }

    private Boolean determineImportHint(TypeDescriptor descriptor, TypeIntrospector.Metadata metadata) {
        if ( descriptor == null ) {
            return Boolean.FALSE;
        }
        if ( descriptor.kind() == LangTypeKind.DECLARED ) {
            return null;
        }
        if ( descriptor.kind() == LangTypeKind.ARRAY ) {
            TypeDescriptor baseComponent = metadata.baseComponentType().orElse( null );
            if ( baseComponent == null ) {
                return Boolean.FALSE;
            }
            if ( baseComponent.kind() == LangTypeKind.DECLARED ) {
                return null;
            }
            return Boolean.FALSE;
        }
        return Boolean.FALSE;
    }

    /**
     * Returns the Type that represents the declared Class type of the given type. For primitive types, the boxed class
     * will be used. Examples:
     * <ul>
     * <li>If type represents {@code java.lang.Integer}, it will return the type that represents {@code Class<Integer>}.
     * </li>
     * <li>If type represents {@code int}, it will return the type that represents {@code Class<Integer>}.</li>
     * </ul>
     *
     * @param type the type to return the declared class type for
     * @return the type representing {@code Class<type>}.
     */
    public Type classTypeOf(Type type) {
        if ( type == null || type.isVoid() ) {
            return null;
        }
        TypeDescriptor descriptor = type.getTypeDescriptor();
        if ( descriptor == null ) {
            return null;
        }
        if ( descriptor.isPrimitive() ) {
            descriptor = langTypes.boxed( descriptor );
        }
        TypeElementDescriptor classElement = requireTypeElementDescriptor( "java.lang.Class" );
        TypeDescriptor classDescriptor = langTypes.declaredType(
            classElement,
            java.util.Collections.singletonList( descriptor )
        );
        return getType( classDescriptor );
    }

    public Parameter getSingleParameter(TypeDescriptor includingType, Accessor accessor) {
        if ( accessor == null || accessor.getAccessorType().isFieldAssignment() ) {
            return null;
        }
        ElementDescriptor elementDescriptor = accessor.getElement();
        if ( !( elementDescriptor instanceof ExecutableDescriptor ) ) {
            return null;
        }
        ExecutableDescriptor executableDescriptor = (ExecutableDescriptor) elementDescriptor;
        if ( executableDescriptor.parameters().size() != 1 ) {
            return null;
        }
        return getSingleParameter( includingType, executableDescriptor );
    }

    public Parameter getSingleParameter(Type includingType, Accessor accessor) {
        return getSingleParameter( includingType != null ? includingType.getTypeDescriptor() : null, accessor );
    }

    public Parameter getSingleParameter(TypeDescriptor includingType, ExecutableDescriptor method) {
        List<Parameter> parameters = getParameters( includingType, method );
        return parameters.isEmpty() ? null : parameters.get( 0 );
    }

    public Parameter getSingleParameter(Type includingType, ExecutableDescriptor method) {
        return getSingleParameter( includingType != null ? includingType.getTypeDescriptor() : null, method );
    }

    public List<Parameter> getParameters(TypeDescriptor includingType, Accessor accessor) {
        if ( accessor == null ) {
            return java.util.Collections.emptyList();
        }
        ElementDescriptor element = accessor.getElement();
        if ( element instanceof ExecutableDescriptor ) {
            return getParameters( includingType, (ExecutableDescriptor) element );
        }
        return java.util.Collections.emptyList();
    }

    public List<Parameter> getParameters(Type includingType, Accessor accessor) {
        return getParameters( includingType != null ? includingType.getTypeDescriptor() : null, accessor );
    }

    public List<Parameter> getParameters(TypeDescriptor includingType, ExecutableDescriptor method) {
        if ( method == null ) {
            return java.util.Collections.emptyList();
        }
        ExecutableSignature signature = typeIntrospector.resolveExecutable( includingType, method );
        List<TypeDescriptor> resolvedTypes = signature != null
            ? signature.parameterTypes()
            : java.util.Collections.emptyList();

        List<ParameterDescriptor> descriptors = method.parameters();
        if ( descriptors.isEmpty() ) {
            return java.util.Collections.emptyList();
        }
        List<Parameter> result = new ArrayList<>( descriptors.size() );
        for ( int i = 0; i < descriptors.size(); i++ ) {
            ParameterDescriptor parameterDescriptor = descriptors.get( i );
            TypeDescriptor parameterTypeDescriptor = resolvedTypes.size() > i
                ? resolvedTypes.get( i )
                : parameterDescriptor.type();
            Type parameterType = getType( parameterTypeDescriptor );
            result.add( Parameter.forDescriptor( parameterDescriptor, parameterType ) );
        }
        return result;
    }

    public List<Parameter> getParameters(Type includingType, ExecutableDescriptor method) {
        return getParameters( includingType != null ? includingType.getTypeDescriptor() : null, method );
    }

    public Type getReturnType(TypeDescriptor includingType, Accessor accessor) {
        if ( accessor == null ) {
            return null;
        }
        ElementDescriptor element = accessor.getElement();
        if ( element instanceof ExecutableDescriptor ) {
            return getReturnType( includingType, (ExecutableDescriptor) element );
        }
        TypeDescriptor resolved = includingType != null
            ? typeIntrospector.asMemberOf( includingType, element )
            : element.asType();
        return getType( resolved );
    }

    public Type getReturnType(Type includingType, Accessor accessor) {
        return getReturnType( includingType != null ? includingType.getTypeDescriptor() : null, accessor );
    }

    public Type getReturnType(TypeDescriptor includingType, ExecutableDescriptor method) {
        if ( method == null ) {
            return null;
        }
        ExecutableSignature signature = typeIntrospector.resolveExecutable( includingType, method );
        TypeDescriptor returnDescriptor = signature != null ? signature.returnType() : method.returnType();
        return getType( returnDescriptor );
    }

    public Type getReturnType(Type includingType, ExecutableDescriptor method) {
        return getReturnType( includingType != null ? includingType.getTypeDescriptor() : null, method );
    }

    public List<Type> getThrownTypes(TypeDescriptor includingType, ExecutableDescriptor method) {
        if ( method == null ) {
            return java.util.Collections.emptyList();
        }
        ExecutableSignature signature = typeIntrospector.resolveExecutable( includingType, method );
        List<TypeDescriptor> thrownDescriptors = signature != null
            ? signature.thrownTypes()
            : method.thrownTypes();
        if ( thrownDescriptors.isEmpty() ) {
            return java.util.Collections.emptyList();
        }
        java.util.LinkedHashSet<Type> types = new java.util.LinkedHashSet<>( thrownDescriptors.size() );
        for ( TypeDescriptor thrownDescriptor : thrownDescriptors ) {
            types.add( getType( thrownDescriptor ) );
        }
        return new ArrayList<>( types );
    }

    public List<Type> getThrownTypes(Type includingType, ExecutableDescriptor method) {
        return getThrownTypes( includingType != null ? includingType.getTypeDescriptor() : null, method );
    }

    boolean isIterableDescriptor(TypeDescriptor descriptor) {
        return isSubtypeErased( descriptor, iterableType );
    }

    boolean isCollectionDescriptor(TypeDescriptor descriptor) {
        return isSubtypeErased( descriptor, collectionType );
    }

    boolean isMapDescriptor(TypeDescriptor descriptor) {
        return isSubtypeErased( descriptor, mapType );
    }

    boolean isStreamDescriptor(TypeDescriptor descriptor) {
        return isSubtypeErased( descriptor, streamType );
    }

    private List<Type> getTypeParameters(TypeDescriptor descriptor, boolean isImplementationType) {
        if ( descriptor == null || descriptor.typeArguments().isEmpty() ) {
            return java.util.Collections.emptyList();
        }

        List<Type> result = new ArrayList<>( descriptor.typeArguments().size() );
        for ( TypeDescriptor argument : descriptor.typeArguments() ) {
            Type argumentType = getType( argument, false, null );
            result.add( isImplementationType ? argumentType.getTypeBound() : argumentType );
        }

        return result;
    }

    private boolean isSubtypeErased(TypeDescriptor descriptor, TypeDescriptor target) {
        if ( descriptor == null || target == null ) {
            return false;
        }
        LangTypeKind kind = descriptor.kind();
        if ( kind == LangTypeKind.PRIMITIVE
            || kind == LangTypeKind.VOID
            || kind == LangTypeKind.ARRAY
            || kind == LangTypeKind.ERROR ) {
            return false;
        }
        return langTypes.isSubtypeErased( descriptor, target );
    }

    private ImplementationType getImplementationType(TypeDescriptor descriptor) {
        if ( descriptor == null || descriptor.kind() != LangTypeKind.DECLARED ) {
            return null;
        }

        Optional<String> qualifiedName = descriptor.qualifiedName();
        if ( !qualifiedName.isPresent() ) {
            return null;
        }

        ImplementationType implementation = implementationTypes.get( qualifiedName.get() );

        if ( implementation != null ) {
            Type implementationType = implementation.getType();
            Type replacement = implementationType;

            List<TypeDescriptor> typeArguments = descriptor.typeArguments();
            if ( !typeArguments.isEmpty() ) {
                TypeDescriptor implementationDescriptor = implementationType.getTypeDescriptor();
                TypeElementDescriptor implElement = implementationDescriptor != null
                    ? implementationDescriptor.typeElement().orElse( null )
                    : null;

                if ( implElement != null ) {
                    List<TypeDescriptor> normalizedArguments = new ArrayList<>( typeArguments.size() );
                    for ( TypeDescriptor typeArgument : typeArguments ) {
                        normalizedArguments.add( normalizeImplementationTypeArgument( typeArgument ) );
                    }
                    TypeDescriptor replacementDescriptor = langTypes.declaredType( implElement, normalizedArguments );
                    replacement = getType( replacementDescriptor ).withoutBounds();
                }
            }

            return implementation.createNew( replacement );
        }

        return null;
    }

    private TypeDescriptor normalizeImplementationTypeArgument(TypeDescriptor typeArgument) {
        if ( typeArgument == null ) {
            return null;
        }

        LangTypeKind kind = typeArgument.kind();
        if ( kind == LangTypeKind.WILDCARD || kind == LangTypeKind.TYPE_PARAMETER ) {
            TypeDescriptor boundDescriptor = getTypeBoundDescriptor( typeArgument );
            if ( boundDescriptor == null ) {
                return typeArgument;
            }

            if ( boundDescriptor.kind() == LangTypeKind.INTERSECTION ) {
                return langTypes.erasure( boundDescriptor );
            }

            return boundDescriptor;
        }

        return typeArgument;
    }

    private BuilderDescriptor findBuilder(Type type, BuilderGem builderGem, boolean report) {
        if ( type == null ) {
            return null;
        }
        if ( builderGem != null && builderGem.disableBuilder().get() ) {
            return null;
        }

        TypeDescriptor descriptor = type.getTypeDescriptor();
        if ( descriptor == null ) {
            return null;
        }

        try {
            return builderIntrospector.findBuilder( descriptor );
        }
        catch ( org.mapstruct.ap.langmodel.BuilderIntrospectionException ex ) {
            if ( report ) {
                messager.printMessage(
                    type.getTypeElementDescriptor(),
                    Message.BUILDER_MORE_THAN_ONE_BUILDER_CREATION_METHOD,
                    type.getFullyQualifiedName(),
                    describeExecutables( ex.getConflictingCreationMethods() )
                );
            }
        }
        catch ( MoreThanOneBuilderCreationMethodException ex ) {
            if ( report ) {
                messager.printMessage(
                    type.getTypeElementDescriptor(),
                    Message.BUILDER_MORE_THAN_ONE_BUILDER_CREATION_METHOD,
                    type.getFullyQualifiedName(),
                    describeBuilderInfos( ex.getBuilderInfo() )
                );
            }
        }

        return null;
    }

    private String describeBuilderInfos(Collection<BuilderInfo> builderInfos) {
        if ( builderInfos == null || builderInfos.isEmpty() ) {
            return "";
        }
        List<ExecutableDescriptor> executables = new ArrayList<>( builderInfos.size() );
        for ( BuilderInfo info : builderInfos ) {
            if ( info == null ) {
                continue;
            }
            Object creation = info.getBuilderCreationMethod();
            if ( creation != null ) {
                executables.add( (ExecutableDescriptor) descriptorFactory.elementDescriptor( creation ) );
            }
        }
        return describeExecutables( executables );
    }

    private String describeExecutables(Collection<ExecutableDescriptor> executables) {
        if ( executables == null || executables.isEmpty() ) {
            return "";
        }
        return executables.stream()
            .filter( executable -> executable != null )
            .map( this::describeExecutable )
            .collect( Collectors.joining( ", " ) );
    }

    private String describeExecutable(ExecutableDescriptor executable) {
        StringBuilder sb = new StringBuilder( executable.simpleName().content() );
        sb.append( '(' );
        List<ParameterDescriptor> parameters = executable.parameters();
        if ( parameters != null && !parameters.isEmpty() ) {
            sb.append(
                parameters.stream()
                    .map( parameter -> {
                        TypeDescriptor type = parameter.type();
                        if ( type == null ) {
                            return "?";
                        }
                        Type resolvedType = getType( type );
                        if ( resolvedType != null ) {
                            return sanitizeDisplayName( resolvedType.getFullyQualifiedName() );
                        }
                        return sanitizeDisplayName( type.displayName() );
                    } )
                    .collect( Collectors.joining( ", " ) )
            );
        }
        sb.append( ')' );
        return sb.toString();
    }

    /**
     * creates a void return type
     *
     * @return void type
     */
    public Type createVoidType() {
        return getType( langTypes.voidType() );
    }

    /**
     * Establishes the type bound:
     * <ol>
     * <li>{@code <? extends Number>}, returns Number</li>
     * <li>{@code <? super Number>}, returns Number</li>
     * <li>{@code <?>}, returns Object</li>
     * <li>{@code <T extends Number>, returns Number}</li>
     * </ol>
     *
     * @param descriptor the type to return the bound for
     * @return the bound for this parameter
     */
    public TypeDescriptor getTypeBoundDescriptor(TypeDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        if ( descriptor.kind() == LangTypeKind.WILDCARD ) {
            Optional<TypeDescriptor> extendsBound = descriptor.wildcardExtendsBound();
            if ( extendsBound.isPresent() ) {
                return extendsBound.get();
            }

            Optional<TypeDescriptor> superBound = descriptor.wildcardSuperBound();
            if ( superBound.isPresent() ) {
                return superBound.get();
            }

            if ( "?".equals( descriptor.displayName() ) ) {
                TypeElementDescriptor objectElement = langElements.typeElement( Object.class.getCanonicalName() );
                if ( objectElement != null ) {
                    return objectElement.asType();
                }
            }
        }
        else if ( descriptor.kind() == LangTypeKind.TYPE_PARAMETER ) {
            Optional<TypeDescriptor> upper = descriptor.typeVariableUpperBound();
            if ( upper.isPresent() ) {
                return upper.get();
            }

            Optional<TypeDescriptor> lower = descriptor.typeVariableLowerBound();
            if ( lower.isPresent() ) {
                return lower.get();
            }
        }

        return descriptor;
    }

    public Type getTypeBound(TypeDescriptor descriptor) {
        TypeDescriptor bound = getTypeBoundDescriptor( descriptor );
        return bound == null ? null : getType( bound );
    }

    public BuilderType builderTypeFor( Type type, BuilderGem builder ) {
        if ( type != null ) {
            BuilderDescriptor builderDescriptor = findBuilder( type, builder, true );
            return BuilderType.create( builderDescriptor, type, this, this.langTypes );
        }
        return null;
    }

    public Type effectiveResultTypeFor( Type type, BuilderGem builder ) {
        if ( type != null ) {
            BuilderDescriptor builderDescriptor = findBuilder( type, builder, false );
            BuilderType builderType = BuilderType.create( builderDescriptor, type, this, this.langTypes );
            return builderType != null ? builderType.getBuilder() : type;
        }
        return type;
    }
}
