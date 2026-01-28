/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSTypeParameter;
import com.google.devtools.ksp.symbol.KSTypeReference;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Adapter that wraps a KSP {@link KSTypeParameter} as a javax {@link TypeParameterElement}.
 */
final class KspTypeParameterElementAdapter implements TypeParameterElement {

    private final KSTypeParameter typeParam;
    private final KspTypeAdapterFactory adapterFactory;

    KspTypeParameterElementAdapter(KSTypeParameter typeParam, KspTypeAdapterFactory adapterFactory) {
        this.typeParam = typeParam;
        this.adapterFactory = adapterFactory;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.TYPE_PARAMETER;
    }

    @Override
    public TypeMirror asType() {
        // Type parameter as type variable
        return new KspTypeVariableAdapter( typeParam, adapterFactory );
    }

    @Override
    public Element getGenericElement() {
        // The element that declared this type parameter
        return null; // Simplified
    }

    @Override
    public List<? extends TypeMirror> getBounds() {
        List<KSTypeReference> bounds = KspSequenceUtils.toList( typeParam.getBounds() );
        if ( bounds.isEmpty() ) {
            return Collections.emptyList();
        }
        List<TypeMirror> result = new ArrayList<>( bounds.size() );
        for ( KSTypeReference bound : bounds ) {
            result.add( adapterFactory.typeMirror( bound.resolve() ) );
        }
        return result;
    }

    @Override
    public Set<javax.lang.model.element.Modifier> getModifiers() {
        return Collections.emptySet();
    }

    @Override
    public Name getSimpleName() {
        return new KspNameAdapter( typeParam.getName().asString() );
    }

    @Override
    public Element getEnclosingElement() {
        return null;
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        List<KSAnnotation> annotations = KspSequenceUtils.toList( typeParam.getAnnotations() );
        List<AnnotationMirror> result = new ArrayList<>( annotations.size() );
        for ( KSAnnotation ann : annotations ) {
            result.add( adapterFactory.annotationMirror( ann ) );
        }
        return result;
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

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitTypeParameter( this, p );
    }
}
