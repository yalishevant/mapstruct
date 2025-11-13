/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.Map;

import javax.lang.model.element.AnnotationMirror;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

import java.util.Set;

final class JavaxAnnotationDescriptor implements AnnotationDescriptor {

    private final AnnotationMirror mirror;
    private final TypeElementDescriptor type;
    private final Map<String, AnnotationValueDescriptor> values;
    private final Set<String> declaredElements;

    JavaxAnnotationDescriptor(AnnotationMirror mirror,
                              TypeElementDescriptor type,
                              Map<String, AnnotationValueDescriptor> values,
                              Set<String> declaredElements) {
        this.mirror = mirror;
        this.type = type;
        this.values = values;
        this.declaredElements = declaredElements;
    }

    @Override
    public TypeElementDescriptor annotationType() {
        return type;
    }

    @Override
    public Map<String, AnnotationValueDescriptor> elementValues() {
        return values;
    }

    @Override
    public boolean hasValue(String elementName) {
        return declaredElements.contains( elementName );
    }

    AnnotationMirror mirror() {
        return mirror;
    }
}
