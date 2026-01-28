/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.Nullability;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * Adapter that wraps a KSP {@link KSType} as a javax {@link TypeMirror}.
 */
class KspTypeMirrorAdapter implements TypeMirror {

    protected final KSType type;
    protected final KspTypeAdapterFactory adapterFactory;

    KspTypeMirrorAdapter(KSType type, KspTypeAdapterFactory adapterFactory) {
        this.type = type;
        this.adapterFactory = adapterFactory;
    }

    @Override
    public TypeKind getKind() {
        KSDeclaration declaration = type.getDeclaration();
        if ( declaration == null ) {
            return TypeKind.NONE;
        }

        String qualifiedName = declaration.getQualifiedName() != null
            ? declaration.getQualifiedName().asString()
            : "";

        // Check for primitive types (boxed in Kotlin)
        switch ( qualifiedName ) {
            case "kotlin.Boolean":
            case "java.lang.Boolean":
                return TypeKind.BOOLEAN;
            case "kotlin.Byte":
            case "java.lang.Byte":
                return TypeKind.BYTE;
            case "kotlin.Short":
            case "java.lang.Short":
                return TypeKind.SHORT;
            case "kotlin.Int":
            case "java.lang.Integer":
                return TypeKind.INT;
            case "kotlin.Long":
            case "java.lang.Long":
                return TypeKind.LONG;
            case "kotlin.Char":
            case "java.lang.Character":
                return TypeKind.CHAR;
            case "kotlin.Float":
            case "java.lang.Float":
                return TypeKind.FLOAT;
            case "kotlin.Double":
            case "java.lang.Double":
                return TypeKind.DOUBLE;
            case "kotlin.Unit":
            case "java.lang.Void":
                return TypeKind.VOID;
            default:
                if ( declaration instanceof KSClassDeclaration ) {
                    return TypeKind.DECLARED;
                }
                return TypeKind.OTHER;
        }
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        TypeKind kind = getKind();
        if ( kind == TypeKind.DECLARED ) {
            return v.visitDeclared( (javax.lang.model.type.DeclaredType) this, p );
        }
        return v.visitUnknown( this, p );
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return Collections.emptyList();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return null;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        return (A[]) java.lang.reflect.Array.newInstance( annotationType, 0 );
    }

    KSType unwrap() {
        return type;
    }

    @Override
    public String toString() {
        KSDeclaration declaration = type.getDeclaration();
        if ( declaration != null && declaration.getQualifiedName() != null ) {
            return declaration.getQualifiedName().asString();
        }
        return type.toString();
    }
}
