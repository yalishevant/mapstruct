/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * Factory responsible for caching and creating descriptor instances.
 */
final class JavaxDescriptorFactory implements LangDescriptorFactory {

    private final JavaxLangModelContext context;
    private final Map<TypeMirror, JavaxTypeDescriptor> typeCache = new IdentityHashMap<>();
    private final Map<Element, JavaxElementDescriptor> elementCache = new IdentityHashMap<>();
    private final Map<AnnotationMirror, JavaxAnnotationDescriptor> annotationCache = new IdentityHashMap<>();
    private final Map<AnnotationValue, JavaxAnnotationValueDescriptor> annotationValueCache = new IdentityHashMap<>();

    JavaxDescriptorFactory(JavaxLangModelContext context) {
        this.context = Objects.requireNonNull( context );
    }

    @Override
    public TypeDescriptor typeDescriptor(Object nativeType) {
        if ( nativeType == null ) {
            return null;
        }
        if ( !( nativeType instanceof TypeMirror ) ) {
            throw unsupportedHandle( "type", nativeType );
        }
        TypeMirror mirror = (TypeMirror) nativeType;
        if ( mirror.getKind() == TypeKind.NONE ) {
            return null;
        }
        return typeCache.computeIfAbsent( mirror, m -> new JavaxTypeDescriptor( this, m ) );
    }

    @Override
    public TypeElementDescriptor typeElementDescriptor(Object nativeElement) {
        Element element = expectElement( nativeElement );
        return (TypeElementDescriptor) elementDescriptor( element );
    }

    @Override
    public ElementDescriptor elementDescriptor(Object nativeElement) {
        Element element = expectElement( nativeElement );
        if ( element == null ) {
            return null;
        }
        JavaxElementDescriptor cached = elementCache.get( element );
        if ( cached != null ) {
            return cached;
        }
        JavaxElementDescriptor created = JavaxElementDescriptor.create( context, this, element );
        if ( created != null ) {
            elementCache.put( element, created );
        }
        return created;
    }

    @Override
    public AnnotationDescriptor annotationDescriptor(Object nativeAnnotation) {
        if ( nativeAnnotation == null ) {
            return null;
        }
        if ( !( nativeAnnotation instanceof AnnotationMirror ) ) {
            throw unsupportedHandle( "annotation", nativeAnnotation );
        }
        AnnotationMirror annotation = (AnnotationMirror) nativeAnnotation;
        return annotationCache.computeIfAbsent(
            annotation,
            mirror -> JavaxAnnotationFactory.annotationDescriptor( context, this, mirror )
        );
    }

    @Override
    public AnnotationValueDescriptor annotationValueDescriptor(Object nativeAnnotationValue) {
        if ( nativeAnnotationValue == null ) {
            return null;
        }
        if ( !( nativeAnnotationValue instanceof AnnotationValue ) ) {
            throw unsupportedHandle( "annotation value", nativeAnnotationValue );
        }
        AnnotationValue value = (AnnotationValue) nativeAnnotationValue;
        return annotationValueCache.computeIfAbsent(
            value,
            v -> new JavaxAnnotationValueDescriptor( this, v )
        );
    }

    private Element expectElement(Object nativeElement) {
        if ( nativeElement == null ) {
            return null;
        }
        if ( nativeElement instanceof Element ) {
            return (Element) nativeElement;
        }
        throw unsupportedHandle( "element", nativeElement );
    }

    private IllegalArgumentException unsupportedHandle(String kind, Object value) {
        String type = value == null ? "null" : value.getClass().getName();
        return new IllegalArgumentException( "Unsupported " + kind + " handle: " + type );
    }

    JavaxLangModelContext context() {
        return context;
    }
}
