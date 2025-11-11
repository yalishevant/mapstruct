/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.common;

import org.mapstruct.ap.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.descriptor.ElementDescriptor;

/**
 *
 * @author Sjaak Derksen
 */
public class FormattingParameters {

    public static final FormattingParameters EMPTY = new FormattingParameters( null, null, null, null, null, null );

    private final String date;
    private final String number;
    private final AnnotationDescriptor annotation;
    private final AnnotationValueDescriptor dateAnnotationValue;
    private final ElementDescriptor element;
    private final String locale;

    public FormattingParameters(String date,
                                String number,
                                AnnotationDescriptor annotation,
                                AnnotationValueDescriptor dateAnnotationValue,
                                ElementDescriptor element,
                                String locale) {
        this.date = date;
        this.number = number;
        this.annotation = annotation;
        this.dateAnnotationValue = dateAnnotationValue;
        this.element = element;
        this.locale = locale;
    }

    public String getDate() {
        return date;
    }

    public String getNumber() {
        return number;
    }

    public AnnotationDescriptor getAnnotation() {
        return annotation;
    }

    public AnnotationValueDescriptor getDateAnnotationValue() {
        return dateAnnotationValue;
    }

    public ElementDescriptor getElement() {
        return element;
    }

    public String getLocale() {
        return locale;
    }
}
