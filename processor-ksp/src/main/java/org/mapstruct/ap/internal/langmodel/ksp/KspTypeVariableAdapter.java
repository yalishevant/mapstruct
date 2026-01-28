/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSTypeParameter;
import com.google.devtools.ksp.symbol.KSTypeReference;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * Adapter that wraps a KSP {@link KSTypeParameter} as a javax {@link TypeVariable}.
 */
final class KspTypeVariableAdapter implements TypeVariable {

    private final KSTypeParameter typeParam;
    private final KspTypeAdapterFactory adapterFactory;

    KspTypeVariableAdapter(KSTypeParameter typeParam, KspTypeAdapterFactory adapterFactory) {
        this.typeParam = typeParam;
        this.adapterFactory = adapterFactory;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.TYPEVAR;
    }

    @Override
    public Element asElement() {
        return new KspTypeParameterElementAdapter( typeParam, adapterFactory );
    }

    @Override
    public TypeMirror getUpperBound() {
        KSTypeReference firstBound = KspSequenceUtils.firstOrNull( typeParam.getBounds() );
        if ( firstBound != null ) {
            return adapterFactory.typeMirror( firstBound.resolve() );
        }
        return new KspNoTypeAdapter( TypeKind.NONE );
    }

    @Override
    public TypeMirror getLowerBound() {
        return new KspNoTypeAdapter( TypeKind.NULL );
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitTypeVariable( this, p );
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
}
