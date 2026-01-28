/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.ClassKind;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeArgument;
import com.google.devtools.ksp.symbol.KSTypeParameter;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.Variance;

import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.util.TypeDescriptorDisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * KSP implementation of {@link TypeDescriptor}.
 */
final class KspTypeDescriptor implements TypeDescriptor {

    private final KspDescriptorFactory factory;
    private final KSType type;
    private final String id;
    private final LangTypeKind kind;

    KspTypeDescriptor(KspDescriptorFactory factory, KSType type) {
        this.factory = Objects.requireNonNull( factory, "factory" );
        this.type = Objects.requireNonNull( type, "type" );
        this.kind = KspKindMapper.mapTypeKind( type );
        this.id = computeStableId( type );
    }

    KSType type() {
        return type;
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
        KSDeclaration declaration = type.getDeclaration();
        if ( declaration == null || declaration.getQualifiedName() == null ) {
            return Optional.empty();
        }
        return Optional.of( declaration.getQualifiedName().asString() );
    }

    @Override
    public Optional<String> packageName() {
        KSDeclaration declaration = type.getDeclaration();
        if ( declaration == null || declaration.getPackageName() == null ) {
            return Optional.empty();
        }
        return Optional.of( declaration.getPackageName().asString() );
    }

    @Override
    public Optional<TypeElementDescriptor> typeElement() {
        KSDeclaration declaration = type.getDeclaration();
        if ( declaration instanceof KSClassDeclaration ) {
            return Optional.ofNullable( factory.typeElementDescriptor( declaration ) );
        }
        return Optional.empty();
    }

    @Override
    public Optional<TypeDescriptor> componentType() {
        // Handle Kotlin arrays
        String qName = qualifiedName().orElse( "" );
        if ( "kotlin.Array".equals( qName ) ) {
            List<KSTypeArgument> args = type.getArguments();
            if ( !args.isEmpty() ) {
                KSTypeReference typeRef = args.get( 0 ).getType();
                if ( typeRef != null ) {
                    return Optional.ofNullable( factory.typeDescriptor( typeRef.resolve() ) );
                }
            }
        }
        // Handle primitive arrays like IntArray, ByteArray, etc.
        if ( qName.startsWith( "kotlin." ) && qName.endsWith( "Array" ) && !"kotlin.Array".equals( qName ) ) {
            String primitiveType = mapPrimitiveArrayComponent( qName );
            if ( primitiveType != null ) {
                KSClassDeclaration primitiveDecl = factory.resolver().getClassDeclarationByName(
                    factory.resolver().getKSNameFromString( primitiveType )
                );
                if ( primitiveDecl != null ) {
                    return Optional.ofNullable( factory.typeDescriptor( primitiveDecl.asStarProjectedType() ) );
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<TypeDescriptor> typeArguments() {
        List<KSTypeArgument> arguments = type.getArguments();
        if ( arguments.isEmpty() ) {
            return Collections.emptyList();
        }

        List<TypeDescriptor> result = new ArrayList<>( arguments.size() );
        for ( KSTypeArgument arg : arguments ) {
            TypeDescriptor descriptor = factory.typeArgumentDescriptor( arg );
            if ( descriptor != null ) {
                result.add( descriptor );
            }
        }
        return Collections.unmodifiableList( result );
    }

    @Override
    public boolean isPrimitive() {
        return kind == LangTypeKind.PRIMITIVE;
    }

    @Override
    public boolean isVoid() {
        String qName = qualifiedName().orElse( "" );
        return "kotlin.Unit".equals( qName ) || "void".equals( qName );
    }

    @Override
    public boolean isEnum() {
        KSDeclaration declaration = type.getDeclaration();
        if ( declaration instanceof KSClassDeclaration ) {
            return ( (KSClassDeclaration) declaration ).getClassKind() == ClassKind.ENUM_CLASS;
        }
        return false;
    }

    @Override
    public boolean isInterface() {
        KSDeclaration declaration = type.getDeclaration();
        if ( declaration instanceof KSClassDeclaration ) {
            return ( (KSClassDeclaration) declaration ).getClassKind() == ClassKind.INTERFACE;
        }
        return false;
    }

    @Override
    public Optional<TypeDescriptor> wildcardExtendsBound() {
        // KSP handles wildcards differently via KSTypeArgument variance
        // This is called on the TypeDescriptor representing a type argument
        return Optional.empty();
    }

    @Override
    public Optional<TypeDescriptor> wildcardSuperBound() {
        // KSP handles wildcards differently via KSTypeArgument variance
        return Optional.empty();
    }

    @Override
    public Optional<String> typeVariableName() {
        KSDeclaration declaration = type.getDeclaration();
        if ( declaration instanceof KSTypeParameter ) {
            return Optional.of( declaration.getSimpleName().asString() );
        }
        return Optional.empty();
    }

    @Override
    public List<TypeDescriptor> typeVariableBounds() {
        KSDeclaration declaration = type.getDeclaration();
        if ( !( declaration instanceof KSTypeParameter ) ) {
            return Collections.emptyList();
        }

        KSTypeParameter typeParam = (KSTypeParameter) declaration;
        List<KSTypeReference> bounds = KspSequenceUtils.toList( typeParam.getBounds() );
        if ( bounds.isEmpty() ) {
            return Collections.emptyList();
        }

        List<TypeDescriptor> result = new ArrayList<>( bounds.size() );
        for ( KSTypeReference bound : bounds ) {
            KSType boundType = bound.resolve();
            TypeDescriptor descriptor = factory.typeDescriptor( boundType );
            if ( descriptor != null ) {
                result.add( descriptor );
            }
        }
        return Collections.unmodifiableList( result );
    }

    @Override
    public Optional<TypeDescriptor> typeVariableUpperBound() {
        List<TypeDescriptor> bounds = typeVariableBounds();
        if ( bounds.isEmpty() ) {
            return Optional.empty();
        }
        return Optional.of( bounds.get( 0 ) );
    }

    @Override
    public Optional<TypeDescriptor> typeVariableLowerBound() {
        // Kotlin doesn't have lower bounds on type parameters like Java's `? super T`
        return Optional.empty();
    }

    @Override
    public TypeDescriptor erasure() {
        KSType erased = type.starProjection();
        // For proper erasure, we need to get the raw type
        KSDeclaration declaration = type.getDeclaration();
        if ( declaration instanceof KSClassDeclaration ) {
            KSType rawType = ( (KSClassDeclaration) declaration ).asStarProjectedType();
            TypeDescriptor descriptor = factory.typeDescriptor( rawType );
            return descriptor != null ? descriptor : this;
        }
        TypeDescriptor descriptor = factory.typeDescriptor( erased );
        return descriptor != null ? descriptor : this;
    }

    @Override
    public Object unwrap() {
        return type;
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
        if ( !( obj instanceof KspTypeDescriptor ) ) {
            return false;
        }
        KspTypeDescriptor other = (KspTypeDescriptor) obj;
        return Objects.equals( id, other.id );
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "KspTypeDescriptor[" + displayName() + "]";
    }

    private String computeStableId(KSType ksType) {
        StringBuilder builder = new StringBuilder();

        KSDeclaration declaration = ksType.getDeclaration();
        if ( declaration == null ) {
            return "unknown:" + ksType.toString();
        }

        if ( declaration instanceof KSTypeParameter ) {
            builder.append( "typevar:" );
            KSTypeParameter typeParam = (KSTypeParameter) declaration;
            KSDeclaration parent = typeParam.getParentDeclaration();
            if ( parent != null && parent.getQualifiedName() != null ) {
                builder.append( parent.getQualifiedName().asString() );
            }
            builder.append( ":" ).append( typeParam.getSimpleName().asString() );
            return builder.toString();
        }

        if ( declaration.getQualifiedName() != null ) {
            builder.append( "declared:" ).append( declaration.getQualifiedName().asString() );
        }
        else {
            builder.append( "declared:" ).append( declaration.getSimpleName().asString() );
        }

        List<KSTypeArgument> arguments = ksType.getArguments();
        if ( !arguments.isEmpty() ) {
            builder.append( '<' );
            for ( int i = 0; i < arguments.size(); i++ ) {
                if ( i > 0 ) {
                    builder.append( ',' );
                }
                builder.append( computeTypeArgumentId( arguments.get( i ) ) );
            }
            builder.append( '>' );
        }

        if ( ksType.isMarkedNullable() ) {
            builder.append( '?' );
        }

        return builder.toString();
    }

    private String computeTypeArgumentId(KSTypeArgument argument) {
        Variance variance = argument.getVariance();

        if ( variance == Variance.STAR ) {
            return "*";
        }

        KSTypeReference typeRef = argument.getType();
        if ( typeRef == null ) {
            return "?";
        }

        KSType argType = typeRef.resolve();
        String typeId = computeStableId( argType );

        switch ( variance ) {
            case COVARIANT:
                return "out:" + typeId;
            case CONTRAVARIANT:
                return "in:" + typeId;
            case INVARIANT:
            default:
                return typeId;
        }
    }

    private String mapPrimitiveArrayComponent(String arrayTypeName) {
        switch ( arrayTypeName ) {
            case "kotlin.IntArray":
                return "kotlin.Int";
            case "kotlin.ByteArray":
                return "kotlin.Byte";
            case "kotlin.ShortArray":
                return "kotlin.Short";
            case "kotlin.LongArray":
                return "kotlin.Long";
            case "kotlin.FloatArray":
                return "kotlin.Float";
            case "kotlin.DoubleArray":
                return "kotlin.Double";
            case "kotlin.CharArray":
                return "kotlin.Char";
            case "kotlin.BooleanArray":
                return "kotlin.Boolean";
            default:
                return null;
        }
    }
}
