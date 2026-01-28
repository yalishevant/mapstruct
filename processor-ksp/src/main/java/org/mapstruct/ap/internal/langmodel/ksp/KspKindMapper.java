/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.ClassKind;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSFunctionDeclaration;
import com.google.devtools.ksp.symbol.KSPropertyDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeArgument;
import com.google.devtools.ksp.symbol.KSTypeParameter;
import com.google.devtools.ksp.symbol.KSValueParameter;
import com.google.devtools.ksp.symbol.Modifier;
import com.google.devtools.ksp.symbol.Variance;

import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;

import java.util.EnumSet;
import java.util.Set;

/**
 * Maps KSP type and element kinds to MapStruct's language-neutral kind enumerations.
 */
final class KspKindMapper {

    private KspKindMapper() {
    }

    /**
     * Maps a KSP type to the corresponding {@link LangTypeKind}.
     */
    static LangTypeKind mapTypeKind(KSType type) {
        if ( type == null ) {
            return LangTypeKind.ERROR;
        }

        KSDeclaration declaration = type.getDeclaration();

        // Check for type parameters first
        if ( declaration instanceof KSTypeParameter ) {
            return LangTypeKind.TYPE_PARAMETER;
        }

        // Check for error types
        if ( type.isError() ) {
            return LangTypeKind.ERROR;
        }

        // Check arguments for wildcards
        if ( !type.getArguments().isEmpty() ) {
            for ( KSTypeArgument arg : type.getArguments() ) {
                if ( arg.getVariance() == Variance.STAR ||
                     arg.getVariance() == Variance.COVARIANT ||
                     arg.getVariance() == Variance.CONTRAVARIANT ) {
                    // The type itself is declared, wildcards are in arguments
                    break;
                }
            }
        }

        // Check for primitive types (mapped from Kotlin)
        String qualifiedName = getQualifiedName( declaration );
        if ( isPrimitiveType( qualifiedName ) ) {
            return LangTypeKind.PRIMITIVE;
        }

        // Check for void/Unit
        if ( "kotlin.Unit".equals( qualifiedName ) || "void".equals( qualifiedName ) ) {
            return LangTypeKind.VOID;
        }

        // Check for arrays
        if ( qualifiedName != null && qualifiedName.endsWith( "Array" ) &&
             ( qualifiedName.startsWith( "kotlin." ) || "kotlin.Array".equals( qualifiedName ) ) ) {
            return LangTypeKind.ARRAY;
        }

        // Default to declared type
        return LangTypeKind.DECLARED;
    }

    /**
     * Maps a KSP type argument (with variance) to {@link LangTypeKind}.
     */
    static LangTypeKind mapTypeArgumentKind(KSTypeArgument argument) {
        if ( argument == null ) {
            return LangTypeKind.ERROR;
        }

        Variance variance = argument.getVariance();
        if ( variance == Variance.STAR ||
             variance == Variance.COVARIANT ||
             variance == Variance.CONTRAVARIANT ) {
            return LangTypeKind.WILDCARD;
        }

        KSType type = argument.getType() != null ? argument.getType().resolve() : null;
        return type != null ? mapTypeKind( type ) : LangTypeKind.ERROR;
    }

    /**
     * Maps a KSP declaration to the corresponding {@link LangElementKind}.
     */
    static LangElementKind mapElementKind(KSDeclaration declaration) {
        if ( declaration == null ) {
            return LangElementKind.CLASS;
        }

        if ( declaration instanceof KSClassDeclaration ) {
            KSClassDeclaration classDecl = (KSClassDeclaration) declaration;
            ClassKind classKind = classDecl.getClassKind();

            switch ( classKind ) {
                case INTERFACE:
                    return LangElementKind.INTERFACE;
                case ENUM_CLASS:
                    return LangElementKind.ENUM;
                case ENUM_ENTRY:
                    return LangElementKind.ENUM_CONSTANT;
                case ANNOTATION_CLASS:
                    return LangElementKind.ANNOTATION_TYPE;
                case CLASS:
                case OBJECT:
                default:
                    // Check if it's a data class (Kotlin's equivalent of record)
                    if ( classDecl.getModifiers().contains( Modifier.DATA ) ) {
                        return LangElementKind.RECORD;
                    }
                    return LangElementKind.CLASS;
            }
        }

        if ( declaration instanceof KSFunctionDeclaration ) {
            KSFunctionDeclaration func = (KSFunctionDeclaration) declaration;
            // Check if it's a constructor
            if ( "<init>".equals( func.getSimpleName().asString() ) ) {
                return LangElementKind.CONSTRUCTOR;
            }
            return LangElementKind.METHOD;
        }

        if ( declaration instanceof KSPropertyDeclaration ) {
            return LangElementKind.FIELD;
        }

        if ( declaration instanceof KSTypeParameter ) {
            return LangElementKind.CLASS; // Type parameters don't have a direct mapping
        }

        return LangElementKind.CLASS;
    }

    /**
     * Maps a KSP value parameter to {@link LangElementKind}.
     */
    static LangElementKind mapParameterKind(KSValueParameter parameter) {
        return LangElementKind.PARAMETER;
    }

    /**
     * Maps KSP modifiers to MapStruct's {@link LangModifier} set.
     */
    static Set<LangModifier> mapModifiers(Set<Modifier> kspModifiers) {
        Set<LangModifier> result = EnumSet.noneOf( LangModifier.class );

        if ( kspModifiers == null || kspModifiers.isEmpty() ) {
            return result;
        }

        for ( Modifier mod : kspModifiers ) {
            switch ( mod ) {
                case PUBLIC:
                    result.add( LangModifier.PUBLIC );
                    break;
                case PRIVATE:
                    result.add( LangModifier.PRIVATE );
                    break;
                case PROTECTED:
                    result.add( LangModifier.PROTECTED );
                    break;
                case ABSTRACT:
                    result.add( LangModifier.ABSTRACT );
                    break;
                case FINAL:
                    result.add( LangModifier.FINAL );
                    break;
                // Note: STATIC doesn't exist in Kotlin the same way, but companion objects/top-level exist
                default:
                    // Ignore other modifiers
                    break;
            }
        }

        return result;
    }

    /**
     * Maps KSP modifiers for a function to MapStruct's {@link LangModifier} set.
     * In Kotlin interfaces, methods without a body are implicitly abstract but KSP doesn't always
     * mark them with the ABSTRACT modifier. This method handles that case.
     * Also adds PUBLIC modifier for interface methods as they are public by default in Kotlin.
     */
    static Set<LangModifier> mapModifiers(KSFunctionDeclaration function) {
        Set<LangModifier> result = mapModifiers( function.getModifiers() );

        // In Kotlin interfaces, methods are implicitly public and abstract (if no body)
        KSDeclaration parent = function.getParentDeclaration();
        if ( parent instanceof KSClassDeclaration ) {
            KSClassDeclaration classDecl = (KSClassDeclaration) parent;
            if ( classDecl.getClassKind() == ClassKind.INTERFACE ) {
                result = EnumSet.copyOf( result );

                // Add PUBLIC if not already present (interface methods are public by default)
                if ( !result.contains( LangModifier.PUBLIC ) &&
                     !result.contains( LangModifier.PRIVATE ) &&
                     !result.contains( LangModifier.PROTECTED ) ) {
                    result.add( LangModifier.PUBLIC );
                }

                // Add ABSTRACT if method has no implementation
                if ( !result.contains( LangModifier.ABSTRACT ) && !isDefaultMethod( function ) ) {
                    result.add( LangModifier.ABSTRACT );
                }
            }
        }

        return result;
    }

    /**
     * Checks if a function is a default interface method.
     */
    static boolean isDefaultMethod(KSFunctionDeclaration function) {
        if ( function == null ) {
            return false;
        }
        // In Kotlin, interface methods with bodies are considered default
        // Use isAbstract() to check if the method has no implementation
        KSDeclaration parent = function.getParentDeclaration();
        if ( parent instanceof KSClassDeclaration ) {
            KSClassDeclaration classDecl = (KSClassDeclaration) parent;
            if ( classDecl.getClassKind() == ClassKind.INTERFACE ) {
                // If it's in an interface and has an implementation (not abstract), it's a default method
                return !function.isAbstract();
            }
        }
        return false;
    }

    private static String getQualifiedName(KSDeclaration declaration) {
        if ( declaration == null || declaration.getQualifiedName() == null ) {
            return null;
        }
        return declaration.getQualifiedName().asString();
    }

    private static boolean isPrimitiveType(String qualifiedName) {
        if ( qualifiedName == null ) {
            return false;
        }
        switch ( qualifiedName ) {
            case "kotlin.Boolean":
            case "kotlin.Byte":
            case "kotlin.Short":
            case "kotlin.Int":
            case "kotlin.Long":
            case "kotlin.Float":
            case "kotlin.Double":
            case "kotlin.Char":
                return true;
            default:
                return false;
        }
    }
}
