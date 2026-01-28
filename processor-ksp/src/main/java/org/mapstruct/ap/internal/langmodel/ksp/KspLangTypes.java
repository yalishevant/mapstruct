/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.processing.Resolver;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSFunctionDeclaration;
import com.google.devtools.ksp.symbol.KSPropertyDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeArgument;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.Variance;

import org.mapstruct.ap.internal.langmodel.api.LangTypes;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * KSP implementation of {@link LangTypes}.
 */
final class KspLangTypes implements LangTypes {

    private final KspLangModelContext context;
    private final KspDescriptorFactory factory;
    private final Resolver resolver;

    KspLangTypes(KspLangModelContext context, KspDescriptorFactory factory, Resolver resolver) {
        this.context = Objects.requireNonNull( context, "context" );
        this.factory = Objects.requireNonNull( factory, "factory" );
        this.resolver = Objects.requireNonNull( resolver, "resolver" );
    }

    @Override
    public TypeDescriptor declaredType(TypeElementDescriptor element, List<TypeDescriptor> arguments) {
        KSClassDeclaration classDecl = unwrapTypeElement( element );
        if ( classDecl == null ) {
            return null;
        }

        if ( arguments.isEmpty() ) {
            return factory.typeDescriptor( classDecl.asStarProjectedType() );
        }

        // Create type arguments
        List<KSTypeArgument> typeArgs = new ArrayList<>( arguments.size() );
        for ( TypeDescriptor arg : arguments ) {
            KSType argType = unwrapType( arg );
            if ( argType != null ) {
                typeArgs.add( resolver.getTypeArgument( resolver.createKSTypeReferenceFromKSType( argType ),
                    Variance.INVARIANT ) );
            }
        }

        KSType parameterizedType = classDecl.asType( typeArgs );
        return factory.typeDescriptor( parameterizedType );
    }

    @Override
    public TypeDescriptor erasure(TypeDescriptor type) {
        KSType ksType = unwrapType( type );
        if ( ksType == null ) {
            return type;
        }

        KSDeclaration declaration = ksType.getDeclaration();
        if ( declaration instanceof KSClassDeclaration ) {
            KSType erased = ( (KSClassDeclaration) declaration ).asStarProjectedType();
            TypeDescriptor result = factory.typeDescriptor( erased );
            return result != null ? result : type;
        }

        return type;
    }

    @Override
    public TypeDescriptor boxed(TypeDescriptor type) {
        // In Kotlin, primitives are automatically boxed when nullable
        // The primitive types like Int already map to java.lang.Integer in JVM
        return type;
    }

    @Override
    public TypeDescriptor unboxed(TypeDescriptor type) {
        // Kotlin handles boxing/unboxing automatically
        return type;
    }

    @Override
    public boolean isAssignable(TypeDescriptor source, TypeDescriptor target) {
        KSType sourceType = unwrapType( source );
        KSType targetType = unwrapType( target );

        if ( sourceType == null || targetType == null ) {
            return false;
        }

        return sourceType.isAssignableFrom( targetType );
    }

    @Override
    public boolean isSameType(TypeDescriptor a, TypeDescriptor b) {
        if ( a == b ) {
            return true;
        }
        if ( a == null || b == null ) {
            return false;
        }

        KSType typeA = unwrapType( a );
        KSType typeB = unwrapType( b );

        if ( typeA == null || typeB == null ) {
            return false;
        }

        // Compare declarations and nullability
        KSDeclaration declA = typeA.getDeclaration();
        KSDeclaration declB = typeB.getDeclaration();

        if ( declA == null || declB == null ) {
            return false;
        }

        // Compare qualified names
        String nameA = declA.getQualifiedName() != null ? declA.getQualifiedName().asString() : null;
        String nameB = declB.getQualifiedName() != null ? declB.getQualifiedName().asString() : null;

        if ( !Objects.equals( nameA, nameB ) ) {
            return false;
        }

        // Compare type arguments
        List<KSTypeArgument> argsA = typeA.getArguments();
        List<KSTypeArgument> argsB = typeB.getArguments();

        if ( argsA.size() != argsB.size() ) {
            return false;
        }

        for ( int i = 0; i < argsA.size(); i++ ) {
            KSTypeArgument argA = argsA.get( i );
            KSTypeArgument argB = argsB.get( i );

            if ( argA.getVariance() != argB.getVariance() ) {
                return false;
            }

            KSTypeReference refA = argA.getType();
            KSTypeReference refB = argB.getType();

            if ( refA != null && refB != null ) {
                TypeDescriptor descA = factory.typeDescriptor( refA.resolve() );
                TypeDescriptor descB = factory.typeDescriptor( refB.resolve() );
                if ( !isSameType( descA, descB ) ) {
                    return false;
                }
            }
            else if ( refA != refB ) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isSubtype(TypeDescriptor sub, TypeDescriptor sup) {
        KSType subType = unwrapType( sub );
        KSType supType = unwrapType( sup );

        if ( subType == null || supType == null ) {
            return false;
        }

        return supType.isAssignableFrom( subType );
    }

    @Override
    public boolean isSubtypeErased(TypeDescriptor sub, TypeDescriptor rawSuper) {
        TypeDescriptor erasedSub = erasure( sub );
        TypeDescriptor erasedSuper = erasure( rawSuper );
        return isSubtype( erasedSub, erasedSuper );
    }

    @Override
    public boolean contains(TypeDescriptor container, TypeDescriptor contained) {
        // Type containment check - used for generic bounds
        // In KSP, we check if the contained type is within the bounds of the container
        KSType containerType = unwrapType( container );
        KSType containedType = unwrapType( contained );

        if ( containerType == null || containedType == null ) {
            return false;
        }

        // Simple containment: check if same or subtype
        return containerType.isAssignableFrom( containedType );
    }

    @Override
    public List<TypeDescriptor> directSupertypes(TypeDescriptor type) {
        KSType ksType = unwrapType( type );
        if ( ksType == null ) {
            return Collections.emptyList();
        }

        KSDeclaration declaration = ksType.getDeclaration();
        if ( !( declaration instanceof KSClassDeclaration ) ) {
            return Collections.emptyList();
        }

        KSClassDeclaration classDecl = (KSClassDeclaration) declaration;
        List<KSTypeReference> superTypes = new ArrayList<>();
        for ( KSTypeReference superType : KspSequenceUtils.toIterable( classDecl.getSuperTypes() ) ) {
            superTypes.add( superType );
        }

        if ( superTypes.isEmpty() ) {
            return Collections.emptyList();
        }

        List<TypeDescriptor> result = new ArrayList<>( superTypes.size() );
        for ( KSTypeReference superTypeRef : superTypes ) {
            KSType superType = superTypeRef.resolve();
            TypeDescriptor descriptor = factory.typeDescriptor( superType );
            if ( descriptor != null ) {
                result.add( descriptor );
            }
        }

        return Collections.unmodifiableList( result );
    }

    @Override
    public TypeDescriptor asMemberOf(TypeDescriptor containingType, ElementDescriptor member) {
        KSType container = unwrapType( containingType );
        if ( container == null ) {
            return member.asType();
        }

        // Get the member's type in the context of the containing type
        // This handles type variable substitution
        Object unwrapped = member.unwrap();

        if ( unwrapped instanceof KSPropertyDeclaration ) {
            KSPropertyDeclaration property = (KSPropertyDeclaration) unwrapped;
            KSTypeReference typeRef = property.getType();
            if ( typeRef != null ) {
                KSType memberType = typeRef.resolve();
                // TODO: Implement proper type substitution for generics
                return factory.typeDescriptor( memberType );
            }
        }

        if ( unwrapped instanceof KSFunctionDeclaration ) {
            KSFunctionDeclaration function = (KSFunctionDeclaration) unwrapped;
            KSTypeReference returnTypeRef = function.getReturnType();
            if ( returnTypeRef != null ) {
                KSType returnType = returnTypeRef.resolve();
                // TODO: Implement proper type substitution for generics
                return factory.typeDescriptor( returnType );
            }
        }

        return member.asType();
    }

    @Override
    public TypeElementDescriptor asElement(TypeDescriptor type) {
        KSType ksType = unwrapType( type );
        if ( ksType == null ) {
            return null;
        }

        KSDeclaration declaration = ksType.getDeclaration();
        if ( declaration instanceof KSClassDeclaration ) {
            return factory.typeElementDescriptor( declaration );
        }

        return null;
    }

    @Override
    public TypeDescriptor primitive(String primitiveName) {
        String kotlinType = mapPrimitiveName( primitiveName );
        if ( kotlinType == null ) {
            return voidType();
        }

        KSClassDeclaration primitiveDecl = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString( kotlinType )
        );

        if ( primitiveDecl != null ) {
            return factory.typeDescriptor( primitiveDecl.asStarProjectedType() );
        }

        return null;
    }

    @Override
    public TypeDescriptor voidType() {
        KSClassDeclaration unitDecl = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString( "kotlin.Unit" )
        );

        if ( unitDecl != null ) {
            return factory.typeDescriptor( unitDecl.asStarProjectedType() );
        }

        return null;
    }

    private KSType unwrapType(TypeDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }

        Object unwrapped = descriptor.unwrap();
        if ( unwrapped instanceof KSType ) {
            return (KSType) unwrapped;
        }

        if ( unwrapped instanceof KSTypeArgument ) {
            KSTypeArgument arg = (KSTypeArgument) unwrapped;
            KSTypeReference typeRef = arg.getType();
            return typeRef != null ? typeRef.resolve() : null;
        }

        return null;
    }

    private KSClassDeclaration unwrapTypeElement(TypeElementDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }

        Object unwrapped = descriptor.unwrap();
        if ( unwrapped instanceof KSClassDeclaration ) {
            return (KSClassDeclaration) unwrapped;
        }

        return null;
    }

    private String mapPrimitiveName(String javaPrimitive) {
        if ( javaPrimitive == null ) {
            return null;
        }
        switch ( javaPrimitive.toLowerCase() ) {
            case "boolean":
                return "kotlin.Boolean";
            case "byte":
                return "kotlin.Byte";
            case "short":
                return "kotlin.Short";
            case "int":
                return "kotlin.Int";
            case "long":
                return "kotlin.Long";
            case "float":
                return "kotlin.Float";
            case "double":
                return "kotlin.Double";
            case "char":
                return "kotlin.Char";
            case "void":
                return "kotlin.Unit";
            default:
                return null;
        }
    }
}
