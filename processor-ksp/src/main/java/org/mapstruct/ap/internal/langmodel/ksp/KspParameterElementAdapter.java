/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.KSValueParameter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Adapter that wraps a KSP {@link KSValueParameter} as a javax {@link VariableElement}.
 */
final class KspParameterElementAdapter implements VariableElement {

    private final KSValueParameter parameter;
    private final KspTypeAdapterFactory adapterFactory;

    KspParameterElementAdapter(KSValueParameter parameter, KspTypeAdapterFactory adapterFactory) {
        this.parameter = parameter;
        this.adapterFactory = adapterFactory;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.PARAMETER;
    }

    @Override
    public TypeMirror asType() {
        KSTypeReference typeRef = parameter.getType();
        if ( typeRef == null ) {
            return new KspNoTypeAdapter( javax.lang.model.type.TypeKind.NONE );
        }
        return adapterFactory.typeMirror( typeRef.resolve() );
    }

    @Override
    public Object getConstantValue() {
        return null;
    }

    @Override
    public Set<javax.lang.model.element.Modifier> getModifiers() {
        return Collections.emptySet();
    }

    @Override
    public Name getSimpleName() {
        String name = parameter.getName() != null ? parameter.getName().asString() : "";
        return new KspNameAdapter( name );
    }

    @Override
    public Element getEnclosingElement() {
        return null; // Simplified
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        List<KSAnnotation> annotations = KspSequenceUtils.toList( parameter.getAnnotations() );
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
        return v.visitVariable( this, p );
    }
}
