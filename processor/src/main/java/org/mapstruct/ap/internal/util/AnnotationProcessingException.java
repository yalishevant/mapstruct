/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;

/**
 * Indicates an error during annotation processing. Should only be thrown in non-recoverable situations such as errors
 * due to incomplete compilations etc. Expected errors to be propagated to the user of the annotation processor should
 * be raised using the {@link FormattingMessager} API instead.
 *
 * @author Gunnar Morling
 */
@SuppressWarnings("serial")
public class AnnotationProcessingException extends RuntimeException {

    private final ElementDescriptor element;
    private final AnnotationDescriptor annotation;
    private final AnnotationValueDescriptor annotationValue;

    public AnnotationProcessingException(String message) {
        this( message, null, null, null );
    }

    public AnnotationProcessingException(String message, ElementDescriptor element) {
        this( message, element, null, null );
    }

    public AnnotationProcessingException(String message,
                                         ElementDescriptor element,
                                         AnnotationDescriptor annotation) {
        this( message, element, annotation, null );
    }

    public AnnotationProcessingException(String message,
                                         ElementDescriptor element,
                                         AnnotationDescriptor annotation,
                                         AnnotationValueDescriptor annotationValue) {
        super( message );
        this.element = element;
        this.annotation = annotation;
        this.annotationValue = annotationValue;
    }

    public ElementDescriptor getElement() {
        return element;
    }

    public AnnotationDescriptor getAnnotation() {
        return annotation;
    }

    public AnnotationValueDescriptor getAnnotationValue() {
        return annotationValue;
    }
}
