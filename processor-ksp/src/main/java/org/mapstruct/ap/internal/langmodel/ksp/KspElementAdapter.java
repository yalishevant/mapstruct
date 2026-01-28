/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.Modifier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base adapter for wrapping KSP {@link KSDeclaration} as javax {@link Element}.
 */
class KspElementAdapter implements Element {

    protected final KSDeclaration declaration;
    protected final KspTypeAdapterFactory adapterFactory;

    KspElementAdapter(KSDeclaration declaration, KspTypeAdapterFactory adapterFactory) {
        this.declaration = declaration;
        this.adapterFactory = adapterFactory;
    }

    @Override
    public TypeMirror asType() {
        return null; // Subclasses override
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.OTHER;
    }

    @Override
    public Set<javax.lang.model.element.Modifier> getModifiers() {
        Set<javax.lang.model.element.Modifier> result = new HashSet<>();
        for ( Modifier mod : declaration.getModifiers() ) {
            javax.lang.model.element.Modifier javaxMod = mapModifier( mod );
            if ( javaxMod != null ) {
                result.add( javaxMod );
            }
        }
        return result;
    }

    @Override
    public Name getSimpleName() {
        String name = declaration.getSimpleName().asString();
        return new KspNameAdapter( name );
    }

    @Override
    public Element getEnclosingElement() {
        KSDeclaration parent = declaration.getParentDeclaration();
        if ( parent != null ) {
            return adapterFactory.element( parent );
        }
        return null;
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return Collections.emptyList(); // Subclasses override
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        List<KSAnnotation> annotations = KspSequenceUtils.toList( declaration.getAnnotations() );
        List<AnnotationMirror> result = new ArrayList<>( annotations.size() );
        for ( KSAnnotation ann : annotations ) {
            result.add( adapterFactory.annotationMirror( ann ) );
        }
        return result;
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return null; // Not supported in adapter
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        return (A[]) java.lang.reflect.Array.newInstance( annotationType, 0 );
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitUnknown( this, p );
    }

    KSDeclaration unwrap() {
        return declaration;
    }

    private static javax.lang.model.element.Modifier mapModifier(Modifier kspMod) {
        switch ( kspMod ) {
            case PUBLIC:
                return javax.lang.model.element.Modifier.PUBLIC;
            case PRIVATE:
                return javax.lang.model.element.Modifier.PRIVATE;
            case PROTECTED:
                return javax.lang.model.element.Modifier.PROTECTED;
            case INTERNAL:
                return null; // No direct javax equivalent
            case ABSTRACT:
                return javax.lang.model.element.Modifier.ABSTRACT;
            case FINAL:
                return javax.lang.model.element.Modifier.FINAL;
            case OPEN:
                return null; // No direct javax equivalent
            default:
                return null;
        }
    }
}
