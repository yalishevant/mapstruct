/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSAnnotation;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Adapter representing an annotation method (for use as key in annotation element values map).
 */
final class KspAnnotationMethodAdapter implements ExecutableElement {

    private final String methodName;
    private final KSAnnotation annotation;
    private final KspTypeAdapterFactory adapterFactory;

    KspAnnotationMethodAdapter(String methodName, KSAnnotation annotation, KspTypeAdapterFactory adapterFactory) {
        this.methodName = methodName;
        this.annotation = annotation;
        this.adapterFactory = adapterFactory;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.METHOD;
    }

    @Override
    public TypeMirror asType() {
        return new KspNoTypeAdapter( javax.lang.model.type.TypeKind.EXECUTABLE );
    }

    @Override
    public List<? extends TypeParameterElement> getTypeParameters() {
        return Collections.emptyList();
    }

    @Override
    public TypeMirror getReturnType() {
        return new KspNoTypeAdapter( javax.lang.model.type.TypeKind.OTHER );
    }

    @Override
    public List<? extends VariableElement> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public TypeMirror getReceiverType() {
        return new KspNoTypeAdapter( javax.lang.model.type.TypeKind.NONE );
    }

    @Override
    public boolean isVarArgs() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public List<? extends TypeMirror> getThrownTypes() {
        return Collections.emptyList();
    }

    @Override
    public AnnotationValue getDefaultValue() {
        return null;
    }

    @Override
    public Set<javax.lang.model.element.Modifier> getModifiers() {
        return Collections.singleton( javax.lang.model.element.Modifier.PUBLIC );
    }

    @Override
    public Name getSimpleName() {
        return new KspNameAdapter( methodName );
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
        return v.visitExecutable( this, p );
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof ExecutableElement ) ) {
            return false;
        }
        ExecutableElement other = (ExecutableElement) obj;
        return methodName.equals( other.getSimpleName().toString() );
    }

    @Override
    public int hashCode() {
        return methodName.hashCode();
    }
}
