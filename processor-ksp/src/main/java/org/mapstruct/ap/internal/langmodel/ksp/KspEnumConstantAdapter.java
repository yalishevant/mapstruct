/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSClassDeclaration;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Adapter for KSP enum constants as javax {@link VariableElement}.
 */
final class KspEnumConstantAdapter implements VariableElement {

    private final KSClassDeclaration enumEntry;
    private final KspTypeAdapterFactory adapterFactory;

    KspEnumConstantAdapter(KSClassDeclaration enumEntry, KspTypeAdapterFactory adapterFactory) {
        this.enumEntry = enumEntry;
        this.adapterFactory = adapterFactory;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.ENUM_CONSTANT;
    }

    @Override
    public TypeMirror asType() {
        return adapterFactory.declaredType( enumEntry.asStarProjectedType() );
    }

    @Override
    public Object getConstantValue() {
        return null;
    }

    @Override
    public Set<javax.lang.model.element.Modifier> getModifiers() {
        return Collections.singleton( javax.lang.model.element.Modifier.PUBLIC );
    }

    @Override
    public Name getSimpleName() {
        return new KspNameAdapter( enumEntry.getSimpleName().asString() );
    }

    @Override
    public Element getEnclosingElement() {
        return adapterFactory.element( enumEntry.getParentDeclaration() );
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return Collections.emptyList();
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

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitVariable( this, p );
    }
}
