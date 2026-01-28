/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.processing.Resolver;
import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSFunctionDeclaration;
import com.google.devtools.ksp.symbol.KSPropertyDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSValueParameter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Factory for creating javax.lang.model adapters from KSP symbols.
 */
final class KspTypeAdapterFactory {

    private final Resolver resolver;

    KspTypeAdapterFactory(Resolver resolver) {
        this.resolver = resolver;
    }

    AnnotationMirror annotationMirror(KSAnnotation annotation) {
        if ( annotation == null ) {
            return null;
        }
        return new KspAnnotationMirrorAdapter( annotation, this );
    }

    TypeMirror typeMirror(KSType type) {
        if ( type == null ) {
            return null;
        }
        return new KspTypeMirrorAdapter( type, this );
    }

    DeclaredType declaredType(KSType type) {
        if ( type == null ) {
            return null;
        }
        return new KspDeclaredTypeAdapter( type, this );
    }

    Element element(KSDeclaration declaration) {
        if ( declaration == null ) {
            return null;
        }
        if ( declaration instanceof KSClassDeclaration ) {
            return new KspTypeElementAdapter( (KSClassDeclaration) declaration, this );
        }
        if ( declaration instanceof KSFunctionDeclaration ) {
            return new KspExecutableElementAdapter( (KSFunctionDeclaration) declaration, this );
        }
        if ( declaration instanceof KSPropertyDeclaration ) {
            return new KspVariableElementAdapter( (KSPropertyDeclaration) declaration, this );
        }
        return new KspElementAdapter( declaration, this );
    }

    TypeElement typeElement(KSClassDeclaration classDecl) {
        if ( classDecl == null ) {
            return null;
        }
        return new KspTypeElementAdapter( classDecl, this );
    }

    Resolver resolver() {
        return resolver;
    }
}
