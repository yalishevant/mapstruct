/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mapstruct.ap.internal.gem.CollectionMappingStrategyGem;
import org.mapstruct.ap.internal.util.AccessorNamingUtils;
import org.mapstruct.ap.internal.util.Executables;
import org.mapstruct.ap.internal.util.Filters;
import org.mapstruct.ap.internal.util.NativeTypes;
import org.mapstruct.ap.internal.util.Nouns;
import org.mapstruct.ap.internal.util.accessor.Accessor;
import org.mapstruct.ap.internal.util.accessor.AccessorType;
import org.mapstruct.ap.internal.util.accessor.ElementAccessor;
import org.mapstruct.ap.internal.util.accessor.MapValueAccessor;
import org.mapstruct.ap.internal.util.accessor.PresenceCheckAccessor;
import org.mapstruct.ap.internal.util.accessor.ReadAccessor;
import org.mapstruct.ap.descriptor.ElementDescriptor;
import org.mapstruct.ap.descriptor.LangElementKind;
import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.descriptor.FieldDescriptor;
import org.mapstruct.ap.descriptor.LangModifier;
import org.mapstruct.ap.descriptor.LangTypeKind;
import org.mapstruct.ap.langmodel.api.LangTypes;
import org.mapstruct.ap.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.langmodel.TypeIntrospector;

import static org.mapstruct.ap.internal.util.Collections.first;

/**
 * Represents (a reference to) the type of a bean property, parameter etc. Types are managed per generated source file.
 * Each type corresponds to a {@link TypeMirror}, i.e. there are different instances for e.g. {@code Set<String>} and
 * {@code Set<Integer>}.
 * <p>
 * Allows for a unified handling of declared and primitive types and usage within templates. Instances are obtained
 * through {@link TypeFactory}.
 *
 * @author Gunnar Morling
 * @author Filip Hrisafov
 */
public class Type extends ModelElement implements Comparable<Type> {

    private final TypeFactory typeFactory;
    private final AccessorNamingUtils accessorNaming;
    private final TypeIntrospector typeIntrospector;
    private final TypeIntrospector.Metadata metadata;
    private final TypeElementDescriptor typeElementDescriptor;
    private final TypeDescriptor typeDescriptor;
    private final List<Type> typeParameters;

    private final ImplementationType implementationType;
    private final Type componentType;
    private final Type topLevelType;

    private final String packageName;
    private final String name;
    private final String nameWithTopLevelTypeName;
    private final String qualifiedName;

    private final boolean isInterface;
    private final boolean isEnumType;
    private final boolean isIterableType;
    private final boolean isCollectionType;
    private final boolean isMapType;
    private final boolean isVoid;
    private final boolean isStream;
    private final boolean isLiteral;

    private final boolean loggingVerbose;

    private final List<String> enumConstants;

    private final Map<String, String> toBeImportedTypes;
    private final Map<String, String> notToBeImportedTypes;
    private Boolean isToBeImported;

    private Map<String, ReadAccessor> readAccessors = null;
    private Map<String, PresenceCheckAccessor> presenceCheckers = null;

    private List<ExecutableDescriptor> allMethods = null;
    private List<FieldDescriptor> allFields = null;
    private List<RecordComponentDescriptor> recordComponents = null;

    private List<Accessor> setters = null;
    private List<Accessor> adders = null;
    private List<Accessor> alternativeTargetAccessors = null;

    private Type boundingBase = null;
    private List<Type> boundTypes = null;

    private Type boxedEquivalent = null;

    private Boolean hasAccessibleConstructor;

    private final Filters filters;

    private LangTypes langTypes() {
        return typeFactory.langTypes();
    }

    private boolean isNestedType() {
        if ( typeElementDescriptor == null ) {
            return false;
        }
        return typeElementDescriptor.enclosingElement()
            .map( element -> element.kind() != LangElementKind.PACKAGE )
            .orElse( false );
    }

    private boolean hasKind(LangTypeKind expected) {
        if ( typeDescriptor == null ) {
            return false;
        }
        return typeDescriptor.kind() == expected;
    }

    private static boolean hasKind(Type type, LangTypeKind expected) {
        return type != null && type.hasKind( expected );
    }

    private boolean isPrimitiveKind() {
        return typeDescriptor != null && typeDescriptor.isPrimitive();
    }

    //CHECKSTYLE:OFF
    public Type(TypeFactory typeFactory,
                AccessorNamingUtils accessorNaming,
                TypeDescriptor typeDescriptor,
                TypeElementDescriptor explicitTypeElementDescriptor,
                List<Type> typeParameters, ImplementationType implementationType, Type componentType,
                String packageName, String name, String qualifiedName,
                boolean isInterface, boolean isEnumType, boolean isIterableType,
                boolean isCollectionType, boolean isMapType, boolean isStreamType,
                Map<String, String> toBeImportedTypes,
                Map<String, String> notToBeImportedTypes,
                Boolean isToBeImported,
                boolean isLiteral, boolean loggingVerbose,
                TypeIntrospector typeIntrospector,
                TypeIntrospector.Metadata metadata) {

        this.typeFactory = typeFactory;
        this.accessorNaming = accessorNaming;
        this.typeIntrospector = typeIntrospector;
        this.metadata = metadata;
        this.typeElementDescriptor = explicitTypeElementDescriptor != null
            ? explicitTypeElementDescriptor
            : metadata != null ? metadata.typeElement().orElse( null ) : null;

        this.typeDescriptor = typeDescriptor;
        this.typeParameters = typeParameters != null ? typeParameters : Collections.emptyList();
        this.componentType = componentType;
        this.implementationType = implementationType;

        this.packageName = packageName;
        this.name = name;
        this.qualifiedName = qualifiedName;

        this.isInterface = isInterface;
        this.isEnumType = isEnumType;
        this.isIterableType = isIterableType;
        this.isCollectionType = isCollectionType;
        this.isMapType = isMapType;
        this.isStream = isStreamType;
        this.isVoid = typeDescriptor != null && typeDescriptor.isVoid();
        this.isLiteral = isLiteral;

        List<String> constants = metadata != null ? metadata.enumConstants() : java.util.Collections.emptyList();
        enumConstants = constants.isEmpty() ? Collections.emptyList() : new ArrayList<>( constants );

        this.isToBeImported = isToBeImported;
        this.toBeImportedTypes = toBeImportedTypes;
        this.notToBeImportedTypes = notToBeImportedTypes;
        this.filters = new Filters(
            accessorNaming,
            method -> {
                Type resolved = typeFactory.getReturnType( typeDescriptor, method );
                return resolved != null ? resolved.getTypeDescriptor() : null;
            },
            field -> {
                TypeDescriptor resolved = typeIntrospector.asMemberOf( typeDescriptor, field );
                return resolved != null ? resolved : field.fieldType();
            },
            recordComponent -> {
                TypeDescriptor resolved = typeIntrospector.asMemberOf( typeDescriptor, recordComponent );
                return resolved != null ? resolved : recordComponent.componentType();
            },
            method -> {
                Parameter parameter = typeFactory.getSingleParameter( typeDescriptor, method );
                return parameter != null ? parameter.getType().getTypeDescriptor() : null;
            }
        );

        this.loggingVerbose = loggingVerbose;

        this.topLevelType = resolveTopLevelType( isToBeImported, componentType );
        this.nameWithTopLevelTypeName = resolveNameWithTopLevel( componentType, name );
    }
    //CHECKSTYLE:ON

    public TypeDescriptor getTypeDescriptor() {
        return typeDescriptor;
    }

    public TypeElementDescriptor getTypeElementDescriptor() {
        return typeElementDescriptor;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns a String that could be used in generated code to reference to this {@link Type}.<br>
     *  <p>
     * The first time a name is referred-to it will be marked as to be imported. For instance
     * {@code LocalDateTime} can be one of {@code java.time.LocalDateTime} and {@code org.joda.LocalDateTime})
     * <p>
     * If the {@code java.time} variant is referred to first, the {@code java.time.LocalDateTime} will be imported
     * and the {@code org.joda} variant will be referred to with its FQN.
     * <p>
     * If the type is nested and its top level type is to be imported
     * then the name including its top level type will be returned.
     *
     * @return Just the name if this {@link Type} will be imported, the name up to the top level {@link Type}
     * (if the top level type is important, otherwise the fully-qualified name.
     */
    public String createReferenceName() {
        if ( isToBeImported() ) {
            // isToBeImported() returns true for arrays.
            // Therefore, we need to check the top level type when creating the reference
            if ( isTopLevelTypeToBeImported() ) {
                return nameWithTopLevelTypeName != null ? nameWithTopLevelTypeName : name;
            }

            return name;
        }

        if ( shouldUseSimpleName() ) {
            return name;
        }

        if ( isTopLevelTypeToBeImported() && nameWithTopLevelTypeName != null) {
            return nameWithTopLevelTypeName;
        }

        return qualifiedName;
    }

    public List<Type> getTypeParameters() {
        return typeParameters;
    }

    public Type getComponentType() {
        return componentType;
    }

    public boolean isPrimitive() {
        return isPrimitiveKind();
    }

    public boolean isInterface() {
        return isInterface;
    }

    public boolean isEnumType() {
        return isEnumType;
    }

    public boolean isVoid() {
        return isVoid;
    }

    public boolean isAbstract() {
        return typeElementDescriptor != null
            && typeElementDescriptor.modifiers().contains( LangModifier.ABSTRACT );
    }

    public boolean isString() {
        return String.class.getName().equals( getFullyQualifiedName() );
    }

    /**
     * @return this type's enum constants in case it is an enum, an empty list otherwise.
     */
    public List<String> getEnumConstants() {
        return enumConstants;
    }

    /**
     * Returns the implementation type to be instantiated in case this type is an interface iterable, collection or map
     * type. The type will have the correct type arguments, so if this type e.g. represents {@code Set<String>}, the
     * implementation type is {@code HashSet<String>}.
     *
     * @return The implementation type to be instantiated in case this type is an interface iterable, collection or map
     * type, {@code null} otherwise.
     */
    public Type getImplementationType() {
        return implementationType != null ? implementationType.getType() : null;
    }

    public ImplementationType getImplementation() {
        return implementationType;
    }

    /**
     * Whether this type is a sub-type of {@link Iterable} or an array type.
     *
     * @return {@code true} if this type is a sub-type of {@link Iterable} or an array type, {@code false} otherwise.
     */
    public boolean isIterableType() {
        return isIterableType || isArrayType();
    }

    /**
     * Whether this type is a sub-type of{@link Iterable}, {@link java.util.stream.Stream} or an array type
     *
     * @return {@code true} if this type is a sub-type of{@link Iterable}, {@link java.util.stream.Stream} or
     * an array type, {@code false} otherwise
     */
    public boolean isIterableOrStreamType() {
        return isIterableType() || isStreamType();
    }

    public boolean isCollectionType() {
        return isCollectionType;
    }

    public boolean isMapType() {
        return isMapType;
    }

    private boolean hasStringMapSignature() {
        if ( isMapType() ) {
            List<Type> typeParameters = getTypeParameters();
            if ( typeParameters.size() == 2 && typeParameters.get( 0 ).isString() ) {
                return true;
            }
        }

        return false;
    }

    public boolean isCollectionOrMapType() {
        return isCollectionType || isMapType;
    }

    public boolean isArrayType() {
        return componentType != null;
    }

    private boolean isType(Class<?> type) {
        return type.getName().equals( getFullyQualifiedName() );
    }

    private boolean isOptionalType() {
        return isType( Optional.class ) || isType( OptionalInt.class ) || isType( OptionalDouble.class ) ||
            isType( OptionalLong.class );
    }

    public boolean isTypeVar() {
        return hasKind( LangTypeKind.TYPE_PARAMETER );
    }

    public boolean isIntersection() {
        return hasKind( LangTypeKind.INTERSECTION );
    }

    public boolean isJavaLangType() {
        return packageName != null && packageName.startsWith( "java." );
    }

    public boolean isRecord() {
        return metadata != null && metadata.isRecordType();
    }

    /**
     * Whether this type is a sub-type of {@link java.util.stream.Stream}.
     *
     * @return {@code true} it this type is a sub-type of {@link java.util.stream.Stream}, {@code false otherwise}
     */
    public boolean isStreamType() {
        return isStream;
    }

    /**
     * A wild card type can have two types of bounds (mutual exclusive): extends and super.
     *
     * @return true if the bound has a wild card super bound (e.g. ? super Number)
     */
    public boolean hasSuperBound() {
        return typeDescriptor != null && typeDescriptor.wildcardSuperBound().isPresent();
    }

    /**
     * A wild card type can have two types of bounds (mutual exclusive): extends and super.
     *
     * @return true if the bound has a wild card super bound (e.g. ? extends Number)
     */
    public boolean hasExtendsBound() {
        return typeDescriptor != null && typeDescriptor.wildcardExtendsBound().isPresent();
    }

    /**
     * A type variable type can have two types of bounds (mutual exclusive): lower and upper.
     *
     * Note that its use is only permitted on a definition (not on the place where its used). For instance:
     * {@code<T super Number> T map( T in)}
     *
     * @return true if the bound has a type variable lower bound (e.g. T super Number)
     */
    public boolean hasLowerBound() {
        return typeDescriptor != null && typeDescriptor.typeVariableLowerBound().isPresent();
    }

    /**
     * A type variable type can have two types of bounds (mutual exclusive): lower and upper.
     *
     * Note that its use is only permitted on a definition  (not on the place where its used). For instance:
     * {@code><T extends Number> T map( T in)}
     *
     * @return true if the bound has a type variable upper bound (e.g. T extends Number)
     */
    public boolean hasUpperBound() {
        return typeDescriptor != null && typeDescriptor.typeVariableUpperBound().isPresent();
    }

    public String getFullyQualifiedName() {
        return qualifiedName;
    }

    /**
     * @return The name of this type as to be used within import statements.
     */
    public String getImportName() {
        return isArrayType() ? trimSimpleClassName( qualifiedName ) : qualifiedName;
    }

    @Override
    public Set<Type> getImportTypes() {
        Set<Type> result = new HashSet<>();

        if ( hasKind( LangTypeKind.DECLARED ) ) {
            result.add( this );
        }

        if ( componentType != null ) {
            result.addAll( componentType.getImportTypes() );
        }

        if ( topLevelType != null ) {
            result.addAll( topLevelType.getImportTypes() );
        }

        for ( Type parameter : typeParameters ) {
            result.addAll( parameter.getImportTypes() );
        }

        if ( ( hasExtendsBound() || hasSuperBound() ) && getTypeBound() != null ) {
            result.addAll( getTypeBound().getImportTypes() );
        }

        return result;
    }

    protected boolean isTopLevelTypeToBeImported() {
        return topLevelType != null && topLevelType.isToBeImported();
    }

    /**
     * Whether this type is to be imported by means of an import statement in the currently generated source file
     * (it can be referenced in the generated source using its simple name) or not (referenced using the FQN).
     *
     * @return {@code true} if the type is imported, {@code false} otherwise.
     */
    public boolean isToBeImported() {
        if ( isToBeImported == null ) {
            String trimmedName = trimSimpleClassName( name );
            if ( notToBeImportedTypes.containsKey( trimmedName ) ) {
                isToBeImported = false;
                return isToBeImported;
            }
            String trimmedQualifiedName = trimSimpleClassName( qualifiedName );
            String importedType = toBeImportedTypes.get( trimmedName );

            isToBeImported = false;
            if ( importedType != null ) {
                if ( importedType.equals( trimmedQualifiedName ) ) {
                    isToBeImported = true;
                }
            }
            else if ( !isNestedType() ) {
                toBeImportedTypes.put( trimmedName, trimmedQualifiedName );
                isToBeImported = true;
            }
        }
        return isToBeImported;
    }

    private boolean shouldUseSimpleName() {
        // Using trimSimpleClassName since the same is used in the isToBeImported()
        // to check whether notToBeImportedTypes contains it
        String trimmedName = trimSimpleClassName( name );
        String fqn = notToBeImportedTypes.get( trimmedName );
        return trimSimpleClassName( this.qualifiedName ).equals( fqn );
    }

    public Type erasure() {
        if ( typeDescriptor == null ) {
            return this;
        }
        TypeDescriptor erasureDescriptor = langTypes().erasure( typeDescriptor );
        return typeFactory.getType( erasureDescriptor );
    }

    public Type withoutBounds() {
        if ( typeParameters.isEmpty() ) {
            return this;
        }

        TypeElementDescriptor elementDescriptor = typeElementDescriptor;
        if ( elementDescriptor == null ) {
            return this;
        }

        List<TypeDescriptor> boundDescriptors = new ArrayList<>( typeParameters.size() );
        for ( Type typeParameter : typeParameters ) {
            Type bound = typeParameter.getTypeBound();
            TypeDescriptor descriptor = bound != null ? bound.getTypeDescriptor() : null;
            if ( descriptor == null ) {
                descriptor = typeParameter.getTypeDescriptor();
            }
            if ( descriptor == null ) {
                return this;
            }
            boundDescriptors.add( descriptor );
        }

        TypeDescriptor declaredDescriptor = langTypes().declaredType( elementDescriptor, boundDescriptors );
        return typeFactory.getType( declaredDescriptor );
    }

    private Type replaceGeneric(Type oldGenericType, Type newType) {
        if ( !typeParameters.contains( oldGenericType ) || newType == null ) {
            return this;
        }
        newType = newType.getBoxedEquivalent();
        TypeElementDescriptor elementDescriptor = typeElementDescriptor;
        if ( elementDescriptor == null ) {
            return this;
        }
        List<TypeDescriptor> replacementDescriptors = new ArrayList<>( typeParameters.size() );
        for ( Type typeParameter : typeParameters ) {
            TypeDescriptor descriptor = typeParameter.getTypeDescriptor();
            if ( descriptor == null ) {
                return this;
            }
            if ( typeParameter.equals( oldGenericType ) ) {
                TypeDescriptor newDescriptor = newType.getTypeDescriptor();
                if ( newDescriptor == null ) {
                    return this;
                }
                descriptor = newDescriptor;
            }
            replacementDescriptors.add( descriptor );
        }

        TypeDescriptor declaredDescriptor = langTypes().declaredType( elementDescriptor, replacementDescriptors );
        return typeFactory.getType( declaredDescriptor );
    }

    /**
     * Whether this type is assignable to the given other type, considering the "extends / upper bounds"
     * as well.
     *
     * @param other The other type.
     *
     * @return {@code true} if and only if this type is assignable to the given other type.
     */
    public boolean isAssignableTo(Type other) {
        if ( other == null ) {
            return false;
        }
        TypeDescriptor thisDescriptor = typeDescriptor;
        TypeDescriptor otherDescriptor = other.getTypeDescriptor();
        if ( thisDescriptor == null || otherDescriptor == null ) {
            return false;
        }
        LangTypes langTypes = langTypes();
        TypeDescriptor targetDescriptor = hasKind( other, LangTypeKind.WILDCARD )
            ? langTypes.erasure( otherDescriptor )
            : otherDescriptor;
        if ( hasKind( LangTypeKind.WILDCARD ) ) {
            return langTypes.contains( thisDescriptor, targetDescriptor );
        }
        return langTypes.isAssignable( thisDescriptor, targetDescriptor );
    }

    /**
     * Whether this type is raw assignable to the given other type. We can't make a verdict on typevars,
     * they need to be resolved first.
     *
     * @param other The other type.
     *
     * @return {@code true} if and only if this type is assignable to the given other type.
     */
    public boolean isRawAssignableTo(Type other) {
        if ( isTypeVar() || other.isTypeVar() ) {
            return true;
        }
        if ( equals( other ) ) {
            return true;
        }
        TypeDescriptor thisDescriptor = typeDescriptor;
        TypeDescriptor otherDescriptor = other.getTypeDescriptor();
        if ( thisDescriptor != null && otherDescriptor != null ) {
            LangTypes langTypes = langTypes();
            return langTypes.isAssignable(
                langTypes.erasure( thisDescriptor ),
                langTypes.erasure( otherDescriptor )
            );
        }
        return false;
    }

    /**
     * removes any bounds from this type.
     * @return the raw type
     */
    public Type asRawType() {
        if ( getTypeBound() != null ) {
            if ( typeDescriptor != null ) {
                return typeFactory.getType( langTypes().erasure( typeDescriptor ) );
            }
        }
        else {
            return this;
        }
        return this;
    }

    public ReadAccessor getReadAccessor(String propertyName, boolean allowedMapToBean) {
        if ( allowedMapToBean && hasStringMapSignature() ) {
            ExecutableDescriptor getMethod = getAllMethods()
                .stream()
                .filter( m -> m.simpleName().content().equals( "get" ) )
                .filter( m -> m.parameters().size() == 1 )
                .findAny()
                .orElse( null );
            return new MapValueAccessor(
                getMethod,
                typeParameters.get( 1 ).getTypeDescriptor(),
                propertyName
            );
        }

        Map<String, ReadAccessor> readAccessors = getPropertyReadAccessors();

        return readAccessors.get( propertyName );
    }

    public PresenceCheckAccessor getPresenceChecker(String propertyName) {
        if ( hasStringMapSignature() ) {
            return PresenceCheckAccessor.mapContainsKey( propertyName );
        }

        Map<String, PresenceCheckAccessor> presenceCheckers = getPropertyPresenceCheckers();
        return presenceCheckers.get( propertyName );
    }

    /**
     * getPropertyReadAccessors
     *
     * @return an unmodifiable map of all read accessors (including 'is' for booleans), indexed by property name
     */
    public Map<String, ReadAccessor> getPropertyReadAccessors() {
        if ( readAccessors == null ) {

            Map<String, ReadAccessor> recordAccessors = filters.recordAccessorsIn( getRecordComponentDescriptors() );
            Map<String, ReadAccessor> modifiableGetters = new LinkedHashMap<>(recordAccessors);

            List<ReadAccessor> getterList = filters.getterMethodsIn( getAllMethods() );
            for ( ReadAccessor getter : getterList ) {
                String simpleName = getter.getSimpleName();
                if ( recordAccessors.containsKey( simpleName ) ) {
                    // If there is already a record accessor that contains the simple name
                    // then it means that the getter is actually a record component.
                    // In that case we need to ignore it.
                    // e.g. record component named isActive.
                    // The DefaultAccessorNamingStrategy will return active as property name,
                    // but the property name is isActive, since it is a record
                    continue;
                }
                String propertyName = getPropertyName( getter );

                if ( recordAccessors.containsKey( propertyName ) ) {
                    // If there is already a record accessor, the property needs to be ignored
                    continue;
                }
                if ( modifiableGetters.containsKey( propertyName ) ) {
                    // In the DefaultAccessorNamingStrategy, this can only be the case for Booleans: isFoo() and
                    // getFoo(); The latter is preferred.
                    if ( !simpleName.startsWith( "is" ) ) {
                        modifiableGetters.put( propertyName, getter );
                    }

                }
                else {
                    modifiableGetters.put( propertyName, getter );
                }
            }

            List<ReadAccessor> fieldsList = filters.fieldsIn( getAllFields(), ReadAccessor::fromField );
            for ( ReadAccessor field : fieldsList ) {
                String propertyName = getPropertyName( field );
                // If there was no getter or is method for booleans, then resort to the field.
                // If a field was already added do not add it again.
                modifiableGetters.putIfAbsent( propertyName, field );
            }
            readAccessors = Collections.unmodifiableMap( modifiableGetters );
        }
        return readAccessors;
    }

    /**
     * getPropertyPresenceCheckers
     *
     * @return an unmodifiable map of all presence checkers, indexed by property name
     */
    public Map<String, PresenceCheckAccessor> getPropertyPresenceCheckers() {
        if ( presenceCheckers == null ) {
            List<ExecutableDescriptor> checkerList = filters.presenceCheckMethodsIn( getAllMethods() );
            Map<String, PresenceCheckAccessor> modifiableCheckers = new LinkedHashMap<>();
            for ( ExecutableDescriptor checker : checkerList ) {
                modifiableCheckers.put(
                    getPropertyName( checker ),
                    PresenceCheckAccessor.methodInvocation( checker )
                );
            }
            presenceCheckers = Collections.unmodifiableMap( modifiableCheckers );
        }
        return presenceCheckers;
    }

    /**
     * getPropertyWriteAccessors returns a map of the write accessors according to the CollectionMappingStrategy. These
     * accessors include:
     * <ul>
     * <li>setters, the obvious candidate :-), {@link #getSetters() }</li>
     * <li>readAccessors, for collections that do not have a setter, e.g. for JAXB generated collection attributes
     * {@link #getPropertyReadAccessors() }</li>
     * <li>adders, typically for from table generated entities, {@link #getAdders() }</li>
     * </ul>
     *
     * @param cmStrategy collection mapping strategy
     * @return an unmodifiable map of all write accessors indexed by property name
     */
    public Map<String, Accessor> getPropertyWriteAccessors( CollectionMappingStrategyGem cmStrategy ) {
        if ( isRecord() ) {
            // Records do not have setters, so we return an empty map
            return Collections.emptyMap();
        }
        // collect all candidate target accessors
        List<Accessor> candidates = new ArrayList<>( getSetters() );
        candidates.addAll( getAlternativeTargetAccessors() );

        Map<String, Accessor> result = new LinkedHashMap<>();

        for ( Accessor candidate : candidates ) {
            String targetPropertyName = getPropertyName( candidate );

            Accessor readAccessor = getPropertyReadAccessors().get( targetPropertyName );

            Type preferredType = determinePreferredType( readAccessor );
            Type targetType = determineTargetType( candidate );

            // A target access is in general a setter method on the target object. However, in case of collections,
            // the current target accessor can also be a getter method.
            // The following if block, checks if the target accessor should be overruled by an add method.
            if ( cmStrategy == CollectionMappingStrategyGem.SETTER_PREFERRED
                || cmStrategy == CollectionMappingStrategyGem.ADDER_PREFERRED
                || cmStrategy == CollectionMappingStrategyGem.TARGET_IMMUTABLE ) {

                // first check if there's a setter method.
                Accessor adderMethod = null;
                if ( candidate.getAccessorType() == AccessorType.SETTER
                    // ok, the current accessor is a setter. So now the strategy determines what to use
                    && cmStrategy == CollectionMappingStrategyGem.ADDER_PREFERRED ) {
                    adderMethod = getAdderForType( targetType, targetPropertyName );
                }
                else if ( candidate.getAccessorType() == AccessorType.GETTER ) {
                    // the current accessor is a getter (no setter available). But still, an add method is according
                    // to the above strategy (SETTER_PREFERRED || ADDER_PREFERRED) preferred over the getter.
                    adderMethod = getAdderForType( targetType, targetPropertyName );
                }
                if ( adderMethod != null ) {
                    // an adder has been found (according strategy) so overrule current choice.
                    candidate = adderMethod;
                }

            }
            else if ( candidate.getAccessorType() == AccessorType.FIELD  && ( Executables.isFinal( candidate ) ||
                result.containsKey( targetPropertyName ) ) ) {
                // if the candidate is a field and a mapping already exists, then use that one, skip it.
                continue;
            }

            if ( candidate.getAccessorType() == AccessorType.GETTER ) {
                // When the candidate is a getter then it can't be used in the following cases:
                // 1. The collection mapping strategy is target immutable
                // 2. The target type is a stream (streams are immutable)
                if ( cmStrategy == CollectionMappingStrategyGem.TARGET_IMMUTABLE ||
                    targetType != null && targetType.isStreamType() ) {
                    continue;
                }
            }

            Accessor previousCandidate = result.get( targetPropertyName );
            if ( previousCandidate == null || preferredType == null || ( targetType != null
                && preferredType.isAssignableTo( targetType ) ) ) {
                result.put( targetPropertyName, candidate );
            }
        }

        return result;
    }

    private List<RecordComponentDescriptor> getRecordComponentDescriptors() {
        if ( recordComponents == null ) {
            recordComponents = typeElementDescriptor != null
                ? typeIntrospector.recordComponents( typeDescriptor )
                : Collections.emptyList();
        }

        return recordComponents;
    }

    public List<RecordComponentDescriptor> getRecordComponents() {
        return getRecordComponentDescriptors();
    }

    private Type determinePreferredType(Accessor readAccessor) {
        if ( readAccessor != null ) {
            return typeFactory.getReturnType( typeDescriptor, readAccessor );
        }
        return null;
    }

    private Type determineTargetType(Accessor candidate) {
        Parameter parameter = typeFactory.getSingleParameter( typeDescriptor, candidate );
        if ( parameter != null ) {
            return parameter.getType();
        }
        else if ( candidate.getAccessorType() == AccessorType.GETTER
                        || candidate.getAccessorType().isFieldAssignment() ) {
            return typeFactory.getReturnType( typeDescriptor, candidate );
        }
        return null;
    }

    private List<ExecutableDescriptor> getAllMethods() {
        if ( allMethods == null ) {
            allMethods = typeElementDescriptor != null
                ? typeIntrospector.enclosedExecutables( typeDescriptor )
                : Collections.emptyList();
        }

        return allMethods;
    }

    private List<FieldDescriptor> getAllFields() {
        if ( allFields == null ) {
            allFields = typeElementDescriptor != null
                ? typeIntrospector.enclosedFields( typeDescriptor )
                : Collections.emptyList();
        }

        return allFields;
    }

    private String getPropertyName(Accessor accessor ) {
        ElementDescriptor accessorElement = accessor.getElement();
        if ( accessorElement instanceof ExecutableDescriptor ) {
            return getPropertyName( (ExecutableDescriptor) accessorElement );
        }
        else {
            return accessor.getSimpleName();
        }
    }

    private String getPropertyName(ExecutableDescriptor element) {
        return accessorNaming.getPropertyName( element );
    }

    /**
     * Tries to find an addMethod in this type for given collection property in this type.
     *
     * Matching occurs on:
     * <ol>
     * <li>The generic type parameter type of the collection should match the adder method argument</li>
     * <li>When there are more candidates, property name is made singular (as good as is possible). This routine
     * looks for a matching add method name.</li>
     * <li>The singularization rules of Dali are used to make a property name singular. This routine
     * looks for a matching add method name.</li>
     * </ol>
     *
     * @param collectionProperty property type (assumed collection) to find  the adder method for
     * @param pluralPropertyName the property name (assumed plural)
     *
     * @return corresponding adder method for getter when present
     */
    private Accessor getAdderForType(Type collectionProperty, String pluralPropertyName) {

        List<Accessor> candidates;

        if ( collectionProperty.isCollectionType() ) {
            candidates = getAccessorCandidates( collectionProperty, Iterable.class );
        }
        else if ( collectionProperty.isStreamType() ) {
            candidates = getAccessorCandidates( collectionProperty, Stream.class );
        }
        else {
            return null;
        }

        if ( candidates.isEmpty() ) {
            return null;
        }

        if ( candidates.size() == 1 ) {
            return candidates.get( 0 );
        }

        for ( Accessor candidate : candidates ) {
            String elementName = accessorNaming.getElementNameForAdder( candidate );
            if ( elementName != null && elementName.equals( Nouns.singularize( pluralPropertyName ) ) ) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Returns all accessor candidates that start with "add" and have exactly one argument
     * whose type matches the collection or stream property's type argument.
     *
     * @param property the collection or stream property
     * @param superclass the superclass to use for type argument lookup
     *
     * @return accessor candidates
     */
    private List<Accessor> getAccessorCandidates(Type property, Class<?> superclass) {
        Type typeArgument = first( property.determineTypeArguments( superclass ) ).getTypeBound();
        if ( typeArgument == null ) {
            return Collections.emptyList();
        }
        TypeDescriptor boxedArgumentDescriptor = typeArgument.getBoxedEquivalent().getTypeDescriptor();
        if ( boxedArgumentDescriptor == null ) {
            return Collections.emptyList();
        }
        // now, look for a method that
        // 1) starts with add,
        // 2) and has typeArg as one and only arg
        List<Accessor> adderList = getAdders();
        List<Accessor> candidateList = new ArrayList<>();
        for ( Accessor adder : adderList ) {
            Type target = determineTargetType( adder );
            TypeDescriptor adderDescriptor = target != null
                ? target.getBoxedEquivalent().getTypeDescriptor()
                : null;
            if ( adderDescriptor != null && langTypes().isSameType( adderDescriptor, boxedArgumentDescriptor ) ) {
                candidateList.add( adder );
            }
        }
        return candidateList;
    }

    /**
     * getSetters
     *
     * @return an unmodifiable list of all setters
     */
    private List<Accessor> getSetters() {
        if ( setters == null ) {
            setters = Collections.unmodifiableList( filters.setterMethodsIn( getAllMethods() ) );
        }
        return setters;
    }

    /**
     * Alternative accessors could be a getter for a collection / map. By means of the
     * {@link Collection#addAll(Collection) } or {@link Map#putAll(Map)} this getter can still be used as
     * targetAccessor. JAXB XJC tool generates such constructs. This method can be extended when new cases come along.
     * getAdders
     *
     * @return an unmodifiable list of all adders
     */
    private List<Accessor> getAdders() {
        if ( adders == null ) {
            adders = Collections.unmodifiableList( filters.adderMethodsIn( getAllMethods() ) );
        }
        return adders;
    }

    /**
     * Alternative accessors could be a getter for a collection. By means of the
     * {@link java.util.Collection#addAll(java.util.Collection) } this getter can still
     * be used as targetAccessor. JAXB XJC tool generates such constructs.
     *
     * This method can be extended when new cases come along.
     *
     * @return an unmodifiable list of alternative target accessors.
     */
    private List<Accessor> getAlternativeTargetAccessors() {
        if ( alternativeTargetAccessors != null ) {
            return alternativeTargetAccessors;
        }

        if ( isRecord() ) {
            alternativeTargetAccessors = Collections.emptyList();
        }

        if ( alternativeTargetAccessors == null ) {

            List<Accessor> result = new ArrayList<>();
            List<Accessor> setterMethods = getSetters();
            List<Accessor> readAccessors = new ArrayList<>( getPropertyReadAccessors().values() );
            // All the fields are also alternative accessors
            readAccessors.addAll( filters.fieldsIn( getAllFields(), ElementAccessor::new ) );

            // there could be a read accessor (field or  method) for a list/map that is not present as setter.
            // an accessor could substitute the setter in that case and act as setter.
            // (assuming it is initialized)
            for ( Accessor readAccessor : readAccessors ) {
                if ( isCollectionOrMapOrStream( readAccessor ) &&
                    !correspondingSetterMethodExists( readAccessor, setterMethods ) ) {
                    result.add( readAccessor );
                }
                else if ( readAccessor.getAccessorType() == AccessorType.FIELD &&
                    !correspondingSetterMethodExists( readAccessor, setterMethods ) ) {
                    result.add( readAccessor );
                }
            }

            alternativeTargetAccessors = Collections.unmodifiableList( result );
        }
        return alternativeTargetAccessors;
    }

    private boolean correspondingSetterMethodExists(Accessor getterMethod,
                                                    List<Accessor> setterMethods) {
        String getterPropertyName = getPropertyName( getterMethod );

        for ( Accessor setterMethod : setterMethods ) {
            String setterPropertyName = getPropertyName( setterMethod );
            if ( getterPropertyName.equals( setterPropertyName ) ) {
                return true;
            }
        }

        return false;
    }

    private boolean isCollectionOrMapOrStream(Accessor getterMethod) {
        TypeDescriptor accessedType = getterMethod.getAccessedType();
        return isCollection( accessedType ) || isMap( accessedType ) || isStream( accessedType );
    }

    private boolean isCollection(TypeDescriptor candidate) {
        return typeFactory.isCollectionDescriptor( candidate );
    }

    private boolean isStream(TypeDescriptor candidate) {
        return typeFactory.isStreamDescriptor( candidate );
    }

    private boolean isMap(TypeDescriptor candidate) {
        return typeFactory.isMapDescriptor( candidate );
    }

    /**
     * Returns the length of the shortest path in the type hierarchy between this type and the specified other type.
     * Returns {@code -1} if this type is not assignable to the other type. Returns {@code 0} if this type is equal to
     * the other type. Returns {@code 1}, if the other type is a direct super type of this type, and so on.
     *
     * @param assignableOther the other type
     *
     * @return the length of the shortest path in the type hierarchy between this type and the specified other type
     */
    public int distanceTo(Type assignableOther) {
        if ( assignableOther == null ) {
            return -1;
        }
        TypeDescriptor thisDescriptor = typeDescriptor;
        TypeDescriptor otherDescriptor = assignableOther.getTypeDescriptor();
        if ( thisDescriptor != null && otherDescriptor != null ) {
            return distanceTo( thisDescriptor, otherDescriptor );
        }
        return -1;
    }

    private int distanceTo(TypeDescriptor base, TypeDescriptor targetType) {
        LangTypes langTypes = langTypes();
        if ( langTypes.isSameType( base, targetType ) ) {
            return 0;
        }

        if ( !langTypes.isAssignable( base, targetType ) ) {
            return -1;
        }

        List<TypeDescriptor> directSupertypes = langTypes.directSupertypes( base );
        int minDistanceOfSuperToTargetType = Integer.MAX_VALUE;
        for ( TypeDescriptor type : directSupertypes ) {
            if ( type == null ) {
                continue;
            }
            int distanceToTargetType = distanceTo( type, targetType );
            if ( distanceToTargetType >= 0 ) {
                minDistanceOfSuperToTargetType = Math.min( minDistanceOfSuperToTargetType, distanceToTargetType );
            }
        }

        return 1 + minDistanceOfSuperToTargetType;
    }

    /**
     * @param type the type declaring the method
     * @param method the method to check
     * @return Whether this type can access the given method declared on the given type.
     */
    public boolean canAccess(Type type, ExecutableDescriptor method) {
        if ( method == null ) {
            return false;
        }
        Set<LangModifier> modifiers = method.modifiers();
        if ( modifiers.contains( LangModifier.PRIVATE ) ) {
            return false;
        }
        else if ( modifiers.contains( LangModifier.PROTECTED ) ) {
            return isAssignableTo( type ) || getPackageName().equals( type.getPackageName() );
        }
        else if ( !modifiers.contains( LangModifier.PUBLIC ) ) {
            // default
            return getPackageName().equals( type.getPackageName() );
        }
        // public
        return true;
    }

    /**
     * @return A valid Java expression most suitable for representing null - useful for dealing with primitives from
     *         FTL.
     */
    public String getNull() {
        if ( isOptionalType() ) {
            return createReferenceName() + ".empty()";
        }

        if ( !isPrimitive() || isArrayType() ) {
            return "null";
        }
        if ( "boolean".equals( getName() ) ) {
            return "false";
        }
        if ( "byte".equals( getName() ) ) {
            return "0";
        }
        if ( "char".equals( getName() ) ) {
            //"'\u0000'" would have been better, but depends on platform encoding
                return "0";
        }
        if ( "double".equals( getName() ) ) {
            return "0.0d";
        }
        if ( "float".equals( getName() ) ) {
            return "0.0f";
        }
        if ( "int".equals( getName() ) ) {
            return "0";
        }
        if ( "long".equals( getName() ) ) {
            return "0L";
        }
        if ( "short".equals( getName() ) ) {
            return "0";
        }
        throw new UnsupportedOperationException( getName() );
    }

    public String getSensibleDefault() {
        if ( isPrimitive() ) {
            return getNull();
        }
        else if ( "String".equals( getName() ) ) {
            return "\"\"";
        }
        else {
            if ( isNative() ) {
                // must be boxed, since primitive is already checked
                if ( typeDescriptor != null ) {
                    TypeDescriptor unboxed = langTypes().unboxed( typeDescriptor );
                    if ( unboxed != null ) {
                        return typeFactory.getType( unboxed ).getNull();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        // javadoc typemirror: "Types should be compared using the utility methods in Types. There is no guarantee
        // that any particular type will always be represented by the same object." This is true when the objects
        // are in another jar than the mapper. So the qualfiedName is a better candidate.
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        Type other = (Type) obj;

        if ( typeDescriptor != null && other.typeDescriptor != null ) {
            LangTypes langTypes = langTypes();
            if ( this.isWildCardBoundByTypeVar() && other.isWildCardBoundByTypeVar() ) {
                Type thisBound = getTypeBound();
                Type otherBound = other.getTypeBound();
                if ( thisBound != null && otherBound != null ) {
                    TypeDescriptor thisBoundDescriptor = thisBound.getTypeDescriptor();
                    TypeDescriptor otherBoundDescriptor = otherBound.getTypeDescriptor();
                    if ( thisBoundDescriptor != null && otherBoundDescriptor != null ) {
                        return langTypes.isSameType( thisBoundDescriptor, otherBoundDescriptor );
                    }
                }
            }
            return langTypes.isSameType( typeDescriptor, other.typeDescriptor );
        }

        if ( this.isWildCardBoundByTypeVar() && other.isWildCardBoundByTypeVar() ) {
            if ( this.hasExtendsBound() != other.hasExtendsBound()
                && this.hasSuperBound() != other.hasSuperBound() ) {
                return false;
            }
            Type thisBound = getTypeBound();
            Type otherBound = other.getTypeBound();
            if ( thisBound != null && otherBound != null ) {
                TypeDescriptor thisBoundDescriptor = thisBound.getTypeDescriptor();
                TypeDescriptor otherBoundDescriptor = otherBound.getTypeDescriptor();
                if ( thisBoundDescriptor != null && otherBoundDescriptor != null ) {
                    return langTypes().isSameType( thisBoundDescriptor, otherBoundDescriptor );
                }
                return Objects.equals( thisBound.describe(), otherBound.describe() );
            }
            return thisBound == null && otherBound == null;
        }

        return Objects.equals( name, other.name ) && Objects.equals( packageName, other.packageName );
    }

    @Override
    public int compareTo(Type o) {
        return getFullyQualifiedName().compareTo( o.getFullyQualifiedName() );
    }

    @Override
    public String toString() {
        if ( typeDescriptor != null ) {
            return typeDescriptor.displayName();
        }
        if ( qualifiedName != null ) {
            return qualifiedName;
        }
        return name;
    }

    /**
     * @return a string representation of the type for use in messages
     */
    public String describe() {
        if ( loggingVerbose ) {
            return toString();
        }
        else {
            // name allows for inner classes
            String name = getFullyQualifiedName().replaceFirst( "^" + getPackageName() + ".", "" );
            List<Type> typeParams = getTypeParameters();
            if ( typeParams.isEmpty() ) {
                return name;
            }
            else {
                String params = typeParams.stream().map( Type::describe ).collect( Collectors.joining( "," ) );
                return String.format( "%s<%s>", name, params );
            }
        }
    }

    /**
     *
     * @return an identification that can be used as part in a forged method name.
     */
    public String getIdentification() {
        if ( isArrayType() ) {
            return componentType.getName() + "Array";
        }
        else {
            return getTypeBound().getName();
        }
    }

    /**
     * Establishes the type bound:
     * <ol>
     * <li>{@code <? extends Number>}, returns Number</li>
     * <li>{@code <? super Number>}, returns Number</li>
     * <li>{@code <?>}, returns Object</li>
     * <li>{@code <T extends Number>, returns Number}</li>
     * </ol>
     * @return the bound for this parameter
     */
    public Type getTypeBound() {
        if ( boundingBase != null ) {
            return boundingBase;
        }

        if ( typeDescriptor != null ) {
            boundingBase = typeFactory.getTypeBound( typeDescriptor );
        }

        return boundingBase;
    }

    public List<Type> getTypeBounds() {
        if ( this.boundTypes != null ) {
            return boundTypes;
        }
        Type bound = getTypeBound();
        if ( bound == null ) {
            this.boundTypes = Collections.emptyList();
        }
        else if ( !bound.isIntersection() ) {
            this.boundTypes = Collections.singletonList( bound );
        }
        else {
            TypeDescriptor boundDescriptor = bound.getTypeDescriptor();
            if ( boundDescriptor != null && boundDescriptor.kind() == LangTypeKind.INTERSECTION ) {
                List<TypeDescriptor> descriptors = boundDescriptor.typeVariableBounds();
                this.boundTypes = new ArrayList<>( descriptors.size() );
                for ( TypeDescriptor descriptor : descriptors ) {
                    boundTypes.add( typeFactory.getType( descriptor ) );
                }
            }
            else {
                this.boundTypes = Collections.singletonList( bound );
            }
        }

        return this.boundTypes;

    }

    public boolean hasAccessibleConstructor() {
        if ( hasAccessibleConstructor == null ) {
            hasAccessibleConstructor = false;
            if ( typeDescriptor != null ) {
                List<ExecutableDescriptor> constructors = typeIntrospector.constructors( typeDescriptor );
                for ( ExecutableDescriptor constructor : constructors ) {
                    if ( !constructor.modifiers().contains( LangModifier.PRIVATE ) ) {
                        hasAccessibleConstructor = true;
                        break;
                    }
                }
            }
        }
        return hasAccessibleConstructor;
    }

    /**
     * Returns the direct supertypes of a type.  The interface types, if any,
     * will appear last in the list.
     *
     * @return the direct supertypes, or an empty list if none
     */
    public List<Type> getDirectSuperTypes() {
        if ( typeDescriptor != null ) {
            List<TypeDescriptor> directSupertypes = langTypes().directSupertypes( typeDescriptor );
            if ( directSupertypes.isEmpty() ) {
                return Collections.emptyList();
            }
            return directSupertypes.stream()
                .map( typeFactory::getType )
                .collect( Collectors.toList() );
        }
        return Collections.emptyList();
    }

    /**
     * Searches for the given superclass and collects all type arguments for the given class
     *
     * @param superclass the superclass or interface the generic type arguments are searched for
     * @return a list of type arguments or null, if superclass was not found
     */
    public List<Type> determineTypeArguments(Class<?> superclass) {
        if ( qualifiedName.equals( superclass.getName() ) ) {
            return getTypeParameters();
        }

        if ( typeDescriptor != null ) {
            List<TypeDescriptor> directSupertypes = langTypes().directSupertypes( typeDescriptor );
            for ( TypeDescriptor descriptor : directSupertypes ) {
                Type supertype = typeFactory.getType( descriptor );
                List<Type> supertypeTypeArguments = supertype.determineTypeArguments( superclass );
                if ( supertypeTypeArguments != null ) {
                    return supertypeTypeArguments;
                }
            }
        }

        return null;
    }

    /**
     * All primitive types and their corresponding boxed types are considered native.
     * @return true when native.
     */
    public boolean isNative() {
        return NativeTypes.isNative( qualifiedName );
    }

    public boolean isLiteral() {
        return isLiteral;
    }

    /**
     * Steps through the declaredType in order to find a match for this typeVar Type. It aligns with
     * the provided parameterized type where this typeVar type is used.<br>
     * <br>
     * For example:<pre>
     * {@code
     * this: T
     * declaredType: JAXBElement<String>
     * parameterizedType: JAXBElement<T>
     * result: String
     *
     *
     * this: T, T[] or ? extends T,
     * declaredType: E.g. Callable<? extends T>
     * parameterizedType: Callable<BigDecimal>
     * return: BigDecimal
     * }
     * </pre>
     *
     * @param declared the type
     * @param parameterized the parameterized type
     *
     * @return - the same type when this is not a type var in the broadest sense (T, T[], or ? extends T)<br>
     *         - the matching parameter in the parameterized type when this is a type var when found<br>
     *         - null in all other cases
     */
    public ResolvedPair resolveParameterToType(Type declared, Type parameterized) {
        if ( isTypeVar() || isArrayTypeVar() || isWildCardBoundByTypeVar() ) {
            if ( parameterized != null
                && parameterized.getTypeDescriptor() != null
                && declared != null
                && declared.getTypeDescriptor() != null
                && getTypeDescriptor() != null ) {
                DescriptorTypeVarMatcher descriptorMatcher = new DescriptorTypeVarMatcher( typeFactory, this );
                ResolvedPair result = descriptorMatcher.visit( parameterized, declared );
                if ( result != null && result.getMatch() != null ) {
                    return result;
                }
            }
            return new ResolvedPair( this, null );
        }
        return new ResolvedPair( this, this );
    }

    /**
     * Resolves generic types using the declared and parameterized types as input.<br>
     * <br>
     * For example:
     * <pre>
     * {@code
     * this: T
     * declaredType: JAXBElement<T>
     * parameterizedType: JAXBElement<Integer>
     * result: Integer
     *
     * this: List<T>
     * declaredType: JAXBElement<T>
     * parameterizedType: JAXBElement<Integer>
     * result: List<Integer>
     *
     * this: List<? extends T>
     * declaredType: JAXBElement<? extends T>
     * parameterizedType: JAXBElement<BigDecimal>
     * result: List<BigDecimal>
     *
     * this: List<Optional<T>>
     * declaredType: JAXBElement<T>
     * parameterizedType: JAXBElement<BigDecimal>
     * result: List<Optional<BigDecimal>>
     * }
     * </pre>
     * It also works for partial matching.<br>
     * <br>
     * For example:
     * <pre>
     * {@code
     * this: Map<K, V>
     * declaredType: JAXBElement<K>
     * parameterizedType: JAXBElement<BigDecimal>
     * result: Map<BigDecimal, V>
     * }
     * </pre>
     * It also works with multiple parameters at both sides.<br>
     * <br>
     * For example when reversing Key/Value for a Map:
     * <pre>
     * {@code
     * this: Map<KEY, VALUE>
     * declaredType: HashMap<VALUE, KEY>
     * parameterizedType: HashMap<BigDecimal, String>
     * result: Map<String, BigDecimal>
     * }
     * </pre>
     *
     * Mismatch result examples:
     * <pre>
     * {@code
     * this: T
     * declaredType: JAXBElement<Y>
     * parameterizedType: JAXBElement<Integer>
     * result: null
     *
     * this: List<T>
     * declaredType: JAXBElement<Y>
     * parameterizedType: JAXBElement<Integer>
     * result: List<T>
     * }
     * </pre>
     *
     * @param declared the type
     * @param parameterized the parameterized type
     *
     * @return - the result of {@link #resolveParameterToType(Type, Type)} when this type itself is a type var.<br>
     *         - the type but then with the matching type parameters replaced.<br>
     *         - the same type when this type does not contain matching type parameters.
     */
    public Type resolveGenericTypeParameters(Type declared, Type parameterized) {
        if ( isTypeVar() || isArrayTypeVar() || isWildCardBoundByTypeVar() ) {
            return resolveParameterToType( declared, parameterized ).getMatch();
        }
        Type resultType = this;
        for ( Type generic : getTypeParameters() ) {
            if ( generic.isTypeVar() || generic.isArrayTypeVar() || generic.isWildCardBoundByTypeVar() ) {
                ResolvedPair resolveParameterToType = generic.resolveParameterToType( declared, parameterized );
                resultType = resultType.replaceGeneric( generic, resolveParameterToType.getMatch() );
            }
            else {
                Type replacementType = generic.resolveParameterToType( declared, parameterized ).getMatch();
                resultType = resultType.replaceGeneric( generic, replacementType );
            }
        }
        return resultType;
    }

    public boolean isWildCardBoundByTypeVar() {
        return ( hasExtendsBound() || hasSuperBound() ) && getTypeBound().isTypeVar();
    }

    public boolean isArrayTypeVar() {
        return  isArrayType() && getComponentType().isTypeVar();
    }

    private static class DescriptorTypeVarMatcher {

        private final TypeFactory typeFactory;
        private final Type typeToMatch;
        private final LangTypes langTypes;
        private final ResolvedPair defaultValue;
        private final Set<String> visitedPairs = new HashSet<>();

        DescriptorTypeVarMatcher(TypeFactory typeFactory, Type typeToMatch) {
            this.typeFactory = typeFactory;
            this.typeToMatch = typeToMatch;
            this.langTypes = typeFactory.langTypes();
            this.defaultValue = new ResolvedPair( typeToMatch, null );
        }

        ResolvedPair visit(Type parameterized, Type declared) {
            if ( parameterized == null || declared == null ) {
                return defaultValue;
            }

            TypeDescriptor parameterizedDescriptor = parameterized.getTypeDescriptor();
            TypeDescriptor declaredDescriptor = declared.getTypeDescriptor();

            String visitKey = visitKey( parameterizedDescriptor, declaredDescriptor );
            if ( visitKey != null && !visitedPairs.add( visitKey ) ) {
                return defaultValue;
            }

            if ( parameterized.isTypeVar() ) {
                return visitTypeVariable( parameterized, declared );
            }

            if ( isWildcard( parameterizedDescriptor ) ) {
                return visitWildcard( parameterized, declared );
            }

            if ( parameterized.isArrayType() ) {
                return visitArray( parameterized, declared );
            }

            if ( parameterizedDescriptor != null && parameterizedDescriptor.kind() == LangTypeKind.DECLARED ) {
                return visitDeclared( parameterized, declared );
            }

            return defaultValue;
        }

        private boolean isWildcard(TypeDescriptor descriptor) {
            return descriptor != null && descriptor.kind() == LangTypeKind.WILDCARD;
        }

        private boolean isDefault(ResolvedPair pair) {
            return pair == null || pair.getMatch() == null;
        }

        private ResolvedPair visitTypeVariable(Type parameterized, Type declared) {
            TypeDescriptor parameterizedDescriptor = parameterized.getTypeDescriptor();
            TypeDescriptor matchDescriptor = typeToMatch.getTypeDescriptor();
            if ( parameterizedDescriptor != null
                && matchDescriptor != null
                && langTypes.isSameType( parameterizedDescriptor, matchDescriptor ) ) {
                return new ResolvedPair( typeFactory.getType( parameterizedDescriptor ), declared );
            }
            return defaultValue;
        }

        private ResolvedPair visitWildcard(Type parameterized, Type declared) {
            TypeDescriptor parameterizedDescriptor = parameterized.getTypeDescriptor();
            TypeDescriptor matchDescriptor = typeToMatch.getTypeDescriptor();
            if ( parameterizedDescriptor == null || matchDescriptor == null ) {
                return defaultValue;
            }

            TypeDescriptor parameterizedExtends = parameterizedDescriptor.wildcardExtendsBound().orElse( null );
            TypeDescriptor parameterizedSuper = parameterizedDescriptor.wildcardSuperBound().orElse( null );
            TypeDescriptor matchExtends = matchDescriptor.wildcardExtendsBound().orElse( null );
            TypeDescriptor matchSuper = matchDescriptor.wildcardSuperBound().orElse( null );

            if ( matchExtends != null && parameterizedExtends != null
                && langTypes.isSameType( parameterizedExtends, matchExtends ) ) {
                return new ResolvedPair( typeToMatch, declared );
            }
            if ( matchSuper != null && parameterizedSuper != null
                && langTypes.isSameType( parameterizedSuper, matchSuper ) ) {
                return new ResolvedPair( typeToMatch, declared );
            }

            if ( parameterizedExtends != null ) {
                Type extendsType = typeFactory.getType( parameterizedExtends );
                ResolvedPair match = visit( extendsType, declared );
                if ( !isDefault( match ) ) {
                    return new ResolvedPair( typeFactory.getType( parameterizedDescriptor ), declared );
                }
            }

            if ( parameterizedSuper != null ) {
                Type superType = typeFactory.getType( parameterizedSuper );
                ResolvedPair match = visit( superType, declared );
                if ( !isDefault( match ) ) {
                    return new ResolvedPair( typeFactory.getType( parameterizedDescriptor ), declared );
                }
            }

            return defaultValue;
        }

        private ResolvedPair visitArray(Type parameterized, Type declared) {
            if ( parameterized == null ) {
                return defaultValue;
            }

            Type component = parameterized.getComponentType();
            if ( typeToMatch.isArrayTypeVar()
                && declared != null
                && declared.isArrayType()
                && component != null ) {
                Type toMatchComponent = typeToMatch.getComponentType();
                Type declaredComponent = declared.getComponentType();
                if ( toMatchComponent != null && declaredComponent != null ) {
                    ResolvedPair componentMatch = toMatchComponent.resolveParameterToType(
                        component,
                        declaredComponent
                    );
                    if ( componentMatch.getMatch() != null ) {
                        Type arrayMatch = typeFactory.getType( parameterized.getTypeDescriptor() );
                        return new ResolvedPair( typeToMatch, arrayMatch );
                    }
                }
            }

            if ( typeToMatch.isTypeVar()
                && declared != null
                && declared.isArrayType()
                && declared.getComponentType() != null
                && declared.getComponentType().getTypeDescriptor() != null
                && typeToMatch.getTypeDescriptor() != null
                && langTypes.isSameType(
                    declared.getComponentType().getTypeDescriptor(),
                    typeToMatch.getTypeDescriptor()
                ) ) {
                if ( component != null ) {
                    return new ResolvedPair( typeToMatch, component );
                }
            }

            if ( declared != null && declared.isArrayType() && component != null ) {
                return visit( component, declared.getComponentType() );
            }

            return defaultValue;
        }

        private ResolvedPair visitDeclared(Type parameterized, Type declared) {
            TypeDescriptor parameterizedDescriptor = parameterized.getTypeDescriptor();
            TypeDescriptor declaredDescriptor = declared.getTypeDescriptor();
            if ( parameterizedDescriptor == null || declaredDescriptor == null ) {
                return defaultValue;
            }

            if ( parameterized.getTypeParameters().isEmpty() ) {
                return defaultValue;
            }

            TypeDescriptor parameterizedErasure = langTypes.erasure( parameterizedDescriptor );
            TypeDescriptor declaredErasure = langTypes.erasure( declaredDescriptor );

            if ( langTypes.isSameType( parameterizedErasure, declaredErasure ) ) {
                if ( parameterized.getTypeParameters().size() != declared.getTypeParameters().size() ) {
                    return defaultValue;
                }
                List<ResolvedPair> results = new ArrayList<>();
                for ( int i = 0; i < parameterized.getTypeParameters().size(); i++ ) {
                    Type parameterizedArg = parameterized.getTypeParameters().get( i );
                    Type declaredArg = declared.getTypeParameters().get( i );
                    ResolvedPair result = visit( parameterizedArg, declaredArg );
                    if ( !isDefault( result ) ) {
                        results.add( result );
                    }
                }
                if ( results.isEmpty() ) {
                    return defaultValue;
                }
                ResolvedPair first = results.get( 0 );
                boolean allEqual = results.stream().allMatch( first::equals );
                return allEqual ? first : defaultValue;
            }

            List<ResolvedPair> results = new ArrayList<>();
            for ( Type declaredSuper : declared.getDirectSuperTypes() ) {
                if ( declaredSuper == null || isJavaLangObject( declaredSuper ) ) {
                    continue;
                }
                ResolvedPair result = visitDeclared( parameterized, declaredSuper );
                if ( !isDefault( result ) ) {
                    results.add( result );
                }
            }

            for ( Type parameterizedSuper : parameterized.getDirectSuperTypes() ) {
                if ( parameterizedSuper == null || isJavaLangObject( parameterizedSuper ) ) {
                    continue;
                }
                ResolvedPair result = visitDeclared( parameterizedSuper, declared );
                if ( !isDefault( result ) ) {
                    results.add( result );
                }
            }

            if ( results.isEmpty() ) {
                return defaultValue;
            }
            ResolvedPair first = results.get( 0 );
            boolean allEqual = results.stream().allMatch( first::equals );
            return allEqual ? first : defaultValue;
        }

        private boolean isJavaLangObject(Type type) {
            return type != null && Object.class.getName().equals( type.getFullyQualifiedName() );
        }

        private String visitKey(TypeDescriptor parameterizedDescriptor, TypeDescriptor declaredDescriptor) {
            if ( parameterizedDescriptor == null || declaredDescriptor == null ) {
                return null;
            }
            return parameterizedDescriptor.id() + "->" + declaredDescriptor.id();
        }
    }

    /**
     * Reflects any Resolved Pair, examples are
     * T, String
     * ? extends T, BigDecimal
     * T[], Integer[]
     */
    public static class ResolvedPair {

        public ResolvedPair(Type parameter, Type match) {
            this.parameter = parameter;
            this.match = match;
        }

        /**
         * parameter, e.g. T, ? extends T or T[]
         */
        private Type parameter;

        /**
         * match, e.g. String, BigDecimal, Integer[]
         */
        private Type match;

        public Type getParameter() {
            return parameter;
        }

        public Type getMatch() {
            return match;
        }

        @Override
        public boolean equals(Object o) {
            if ( this == o ) {
                return true;
            }
            if ( o == null || getClass() != o.getClass() ) {
                return false;
            }
            ResolvedPair that = (ResolvedPair) o;
            return Objects.equals( parameter, that.parameter ) && Objects.equals( match, that.match );
        }

        @Override
        public int hashCode() {
            return Objects.hash( parameter );
        }
    }

    /**
     * Gets the boxed equivalent type if the type is primitive, int will return Integer
     *
     * @return boxed equivalent
     */
    public Type getBoxedEquivalent() {
        if ( boxedEquivalent != null ) {
            return boxedEquivalent;
        }
        else if ( isPrimitive() ) {
            if ( typeDescriptor != null ) {
                TypeDescriptor boxedDescriptor = langTypes().boxed( typeDescriptor );
                if ( boxedDescriptor != null ) {
                    boxedEquivalent = typeFactory.getType( boxedDescriptor );
                }
            }
            return boxedEquivalent != null ? boxedEquivalent : this;
        }
        return this;
    }

    /**
     * It strips all the {@code []} from the {@code className}.
     *
     * E.g.
     * <pre>
     *     trimSimpleClassName("String[][][]") -> "String"
     *     trimSimpleClassName("String[]") -> "String"
     * </pre>
     *
     * @param className that needs to be trimmed
     *
     * @return the trimmed {@code className}, or {@code null} if the {@code className} was {@code null}
     */
    private String trimSimpleClassName(String className) {
        if ( className == null ) {
            return null;
        }
        String trimmedClassName = className;
        while ( trimmedClassName.endsWith( "[]" ) ) {
            trimmedClassName = trimmedClassName.substring( 0, trimmedClassName.length() - 2 );
        }
        return trimmedClassName;
    }

    public boolean isEnumSet() {
        return "java.util.EnumSet".equals( getFullyQualifiedName() );
    }

    /**
     * return true if this type is a java 17+ sealed class
     */
    public boolean isSealed() {
        return metadata != null && metadata.isSealedType();
    }

    /**
     * return the list of permitted subclasses for the java 17+ sealed class
     */
    public List<Type> getPermittedSubclasses() {
        if ( metadata == null ) {
            return Collections.emptyList();
        }
        List<TypeDescriptor> descriptors = metadata.permittedSubclasses();
        if ( descriptors == null || descriptors.isEmpty() ) {
            return Collections.emptyList();
        }
        return descriptors.stream()
            .map( typeFactory::getType )
            .collect( Collectors.toList() );
    }

    private Type resolveTopLevelType(Boolean importHint, Type component) {
        if ( Boolean.TRUE.equals( importHint ) ) {
            return null;
        }
        if ( metadata != null ) {
            Optional<TypeDescriptor> topLevelDescriptor = metadata.topLevelType();
            if ( topLevelDescriptor.isPresent() ) {
                return typeFactory.getType( topLevelDescriptor.get() );
            }
        }
        if ( component != null && component.topLevelType != null ) {
            return component.topLevelType;
        }
        if ( typeElementDescriptor == null ) {
            return null;
        }

        ElementDescriptor current = typeElementDescriptor.enclosingElement().orElse( null );
        while ( current != null && current.kind() != LangElementKind.PACKAGE ) {
            ElementDescriptor parent = current.enclosingElement().orElse( null );
            if ( parent == null || parent.kind() == LangElementKind.PACKAGE ) {
                TypeDescriptor descriptor = current.asType();
                if ( descriptor != null ) {
                    return typeFactory.getType( descriptor );
                }
                break;
            }
            current = parent;
        }
        return null;
    }

    private String resolveNameWithTopLevel(Type component, String simpleName) {
        if ( typeElementDescriptor == null ) {
            if ( component != null ) {
                String componentName = component.nameWithTopLevelTypeName != null
                    ? component.nameWithTopLevelTypeName
                    : component.getName();
                if ( componentName == null ) {
                    return simpleName;
                }

                String suffix = "";
                String componentSimpleName = component.getName();
                if ( simpleName != null && componentSimpleName != null
                    && simpleName.length() >= componentSimpleName.length()
                    && simpleName.startsWith( componentSimpleName ) ) {
                    suffix = simpleName.substring( componentSimpleName.length() );
                }

                return componentName + suffix;
            }
            return simpleName;
        }
        Deque<String> names = new ArrayDeque<>();
        names.addFirst( simpleName );
        ElementDescriptor current = typeElementDescriptor.enclosingElement().orElse( null );
        while ( current != null && current.kind() != LangElementKind.PACKAGE ) {
            names.addFirst( current.simpleName().content() );
            current = current.enclosingElement().orElse( null );
        }
        return String.join( ".", names );
    }

}
