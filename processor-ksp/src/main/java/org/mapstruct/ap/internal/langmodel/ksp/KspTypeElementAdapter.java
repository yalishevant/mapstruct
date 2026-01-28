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
import com.google.devtools.ksp.symbol.KSTypeParameter;
import com.google.devtools.ksp.symbol.KSTypeReference;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter that wraps a KSP {@link KSClassDeclaration} as a javax {@link TypeElement}.
 */
final class KspTypeElementAdapter extends KspElementAdapter implements TypeElement {

    private final KSClassDeclaration classDecl;

    KspTypeElementAdapter(KSClassDeclaration classDecl, KspTypeAdapterFactory adapterFactory) {
        super( classDecl, adapterFactory );
        this.classDecl = classDecl;
    }

    @Override
    public TypeMirror asType() {
        return adapterFactory.declaredType( classDecl.asStarProjectedType() );
    }

    @Override
    public ElementKind getKind() {
        ClassKind classKind = classDecl.getClassKind();
        switch ( classKind ) {
            case INTERFACE:
                return ElementKind.INTERFACE;
            case CLASS:
                return ElementKind.CLASS;
            case ENUM_CLASS:
                return ElementKind.ENUM;
            case ENUM_ENTRY:
                return ElementKind.ENUM_CONSTANT;
            case ANNOTATION_CLASS:
                return ElementKind.ANNOTATION_TYPE;
            case OBJECT:
                return ElementKind.CLASS;
            default:
                return ElementKind.CLASS;
        }
    }

    @Override
    public Name getQualifiedName() {
        String qName = classDecl.getQualifiedName() != null
            ? classDecl.getQualifiedName().asString()
            : classDecl.getSimpleName().asString();
        return new KspNameAdapter( qName );
    }

    @Override
    public TypeMirror getSuperclass() {
        for ( KSTypeReference superTypeRef : KspSequenceUtils.toIterable( classDecl.getSuperTypes() ) ) {
            KSDeclaration superDecl = superTypeRef.resolve().getDeclaration();
            if ( superDecl instanceof KSClassDeclaration ) {
                KSClassDeclaration superClass = (KSClassDeclaration) superDecl;
                if ( superClass.getClassKind() == ClassKind.CLASS ) {
                    return adapterFactory.typeMirror( superTypeRef.resolve() );
                }
            }
        }
        return new KspNoTypeAdapter( javax.lang.model.type.TypeKind.NONE );
    }

    @Override
    public List<? extends TypeMirror> getInterfaces() {
        List<TypeMirror> interfaces = new ArrayList<>();
        for ( KSTypeReference superTypeRef : KspSequenceUtils.toIterable( classDecl.getSuperTypes() ) ) {
            KSDeclaration superDecl = superTypeRef.resolve().getDeclaration();
            if ( superDecl instanceof KSClassDeclaration ) {
                KSClassDeclaration superClass = (KSClassDeclaration) superDecl;
                if ( superClass.getClassKind() == ClassKind.INTERFACE ) {
                    interfaces.add( adapterFactory.typeMirror( superTypeRef.resolve() ) );
                }
            }
        }
        return interfaces;
    }

    @Override
    public List<? extends TypeParameterElement> getTypeParameters() {
        List<KSTypeParameter> typeParams = classDecl.getTypeParameters();
        if ( typeParams.isEmpty() ) {
            return Collections.emptyList();
        }
        List<TypeParameterElement> result = new ArrayList<>( typeParams.size() );
        for ( KSTypeParameter param : typeParams ) {
            result.add( new KspTypeParameterElementAdapter( param, adapterFactory ) );
        }
        return result;
    }

    @Override
    public NestingKind getNestingKind() {
        KSDeclaration parent = classDecl.getParentDeclaration();
        if ( parent == null ) {
            return NestingKind.TOP_LEVEL;
        }
        if ( parent instanceof KSClassDeclaration ) {
            return NestingKind.MEMBER;
        }
        return NestingKind.LOCAL;
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        List<Element> result = new ArrayList<>();

        // Add nested classes
        for ( KSDeclaration decl : KspSequenceUtils.toIterable( classDecl.getDeclarations() ) ) {
            if ( decl instanceof KSClassDeclaration ) {
                result.add( adapterFactory.typeElement( (KSClassDeclaration) decl ) );
            }
            else if ( decl instanceof KSFunctionDeclaration ) {
                result.add( new KspExecutableElementAdapter( (KSFunctionDeclaration) decl, adapterFactory ) );
            }
            else if ( decl instanceof KSPropertyDeclaration ) {
                result.add( new KspVariableElementAdapter( (KSPropertyDeclaration) decl, adapterFactory ) );
            }
        }

        return result;
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitType( this, p );
    }

    KSClassDeclaration unwrapClass() {
        return classDecl;
    }
}
