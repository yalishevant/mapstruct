/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeArgument;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.Variance;

import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.util.TypeDescriptorDisplay;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * KSP implementation of {@link TypeDescriptor} for type arguments (including wildcards).
 */
final class KspTypeArgumentDescriptor implements TypeDescriptor {

    private final KspDescriptorFactory factory;
    private final KSTypeArgument argument;
    private final String id;
    private final LangTypeKind kind;

    KspTypeArgumentDescriptor(KspDescriptorFactory factory, KSTypeArgument argument) {
        this.factory = Objects.requireNonNull( factory, "factory" );
        this.argument = Objects.requireNonNull( argument, "argument" );
        this.kind = KspKindMapper.mapTypeArgumentKind( argument );
        this.id = computeId( argument );
    }

    KSTypeArgument argument() {
        return argument;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public LangTypeKind kind() {
        return kind;
    }

    @Override
    public String displayName() {
        return TypeDescriptorDisplay.displayName( this );
    }

    @Override
    public Optional<String> qualifiedName() {
        if ( kind == LangTypeKind.WILDCARD ) {
            return Optional.empty();
        }
        KSType type = resolveType();
        if ( type == null || type.getDeclaration() == null || type.getDeclaration().getQualifiedName() == null ) {
            return Optional.empty();
        }
        return Optional.of( type.getDeclaration().getQualifiedName().asString() );
    }

    @Override
    public Optional<String> packageName() {
        KSType type = resolveType();
        if ( type == null || type.getDeclaration() == null || type.getDeclaration().getPackageName() == null ) {
            return Optional.empty();
        }
        return Optional.of( type.getDeclaration().getPackageName().asString() );
    }

    @Override
    public Optional<TypeElementDescriptor> typeElement() {
        if ( kind == LangTypeKind.WILDCARD ) {
            return Optional.empty();
        }
        KSType type = resolveType();
        if ( type == null ) {
            return Optional.empty();
        }
        TypeDescriptor typeDescriptor = factory.typeDescriptor( type );
        return typeDescriptor != null ? typeDescriptor.typeElement() : Optional.empty();
    }

    @Override
    public Optional<TypeDescriptor> componentType() {
        return Optional.empty();
    }

    @Override
    public List<TypeDescriptor> typeArguments() {
        if ( kind == LangTypeKind.WILDCARD ) {
            return Collections.emptyList();
        }
        KSType type = resolveType();
        if ( type == null ) {
            return Collections.emptyList();
        }
        TypeDescriptor typeDescriptor = factory.typeDescriptor( type );
        return typeDescriptor != null ? typeDescriptor.typeArguments() : Collections.emptyList();
    }

    @Override
    public boolean isPrimitive() {
        return kind == LangTypeKind.PRIMITIVE;
    }

    @Override
    public boolean isVoid() {
        return kind == LangTypeKind.VOID;
    }

    @Override
    public boolean isEnum() {
        if ( kind == LangTypeKind.WILDCARD ) {
            return false;
        }
        KSType type = resolveType();
        if ( type == null ) {
            return false;
        }
        TypeDescriptor typeDescriptor = factory.typeDescriptor( type );
        return typeDescriptor != null && typeDescriptor.isEnum();
    }

    @Override
    public boolean isInterface() {
        if ( kind == LangTypeKind.WILDCARD ) {
            return false;
        }
        KSType type = resolveType();
        if ( type == null ) {
            return false;
        }
        TypeDescriptor typeDescriptor = factory.typeDescriptor( type );
        return typeDescriptor != null && typeDescriptor.isInterface();
    }

    @Override
    public Optional<TypeDescriptor> wildcardExtendsBound() {
        // Kotlin's `out T` corresponds to Java's `? extends T`
        if ( argument.getVariance() == Variance.COVARIANT ) {
            KSType type = resolveType();
            if ( type != null ) {
                return Optional.ofNullable( factory.typeDescriptor( type ) );
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<TypeDescriptor> wildcardSuperBound() {
        // Kotlin's `in T` corresponds to Java's `? super T`
        if ( argument.getVariance() == Variance.CONTRAVARIANT ) {
            KSType type = resolveType();
            if ( type != null ) {
                return Optional.ofNullable( factory.typeDescriptor( type ) );
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> typeVariableName() {
        if ( kind == LangTypeKind.WILDCARD ) {
            return Optional.empty();
        }
        KSType type = resolveType();
        if ( type == null ) {
            return Optional.empty();
        }
        TypeDescriptor typeDescriptor = factory.typeDescriptor( type );
        return typeDescriptor != null ? typeDescriptor.typeVariableName() : Optional.empty();
    }

    @Override
    public List<TypeDescriptor> typeVariableBounds() {
        if ( kind == LangTypeKind.WILDCARD ) {
            return Collections.emptyList();
        }
        KSType type = resolveType();
        if ( type == null ) {
            return Collections.emptyList();
        }
        TypeDescriptor typeDescriptor = factory.typeDescriptor( type );
        return typeDescriptor != null ? typeDescriptor.typeVariableBounds() : Collections.emptyList();
    }

    @Override
    public TypeDescriptor erasure() {
        if ( kind == LangTypeKind.WILDCARD ) {
            // For wildcards, erasure should be the bound or Object
            KSType type = resolveType();
            if ( type != null ) {
                TypeDescriptor typeDescriptor = factory.typeDescriptor( type );
                return typeDescriptor != null ? typeDescriptor.erasure() : this;
            }
            return this;
        }
        KSType type = resolveType();
        if ( type == null ) {
            return this;
        }
        TypeDescriptor typeDescriptor = factory.typeDescriptor( type );
        return typeDescriptor != null ? typeDescriptor.erasure() : this;
    }

    @Override
    public Object unwrap() {
        return argument;
    }

    @Override
    public int compareTo(TypeDescriptor other) {
        return displayName().compareTo( other.displayName() );
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof KspTypeArgumentDescriptor ) ) {
            return false;
        }
        KspTypeArgumentDescriptor other = (KspTypeArgumentDescriptor) obj;
        return Objects.equals( id, other.id );
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "KspTypeArgumentDescriptor[" + displayName() + "]";
    }

    private KSType resolveType() {
        KSTypeReference typeRef = argument.getType();
        return typeRef != null ? typeRef.resolve() : null;
    }

    private String computeId(KSTypeArgument arg) {
        Variance variance = arg.getVariance();

        if ( variance == Variance.STAR ) {
            return "wildcard:unbounded";
        }

        KSTypeReference typeRef = arg.getType();
        String typeId = "unknown";
        if ( typeRef != null ) {
            KSType type = typeRef.resolve();
            if ( type != null ) {
                TypeDescriptor desc = factory.typeDescriptor( type );
                if ( desc != null ) {
                    typeId = desc.id();
                }
            }
        }

        switch ( variance ) {
            case COVARIANT:
                return "wildcard:extends=" + typeId;
            case CONTRAVARIANT:
                return "wildcard:super=" + typeId;
            case INVARIANT:
            default:
                return typeId;
        }
    }
}
