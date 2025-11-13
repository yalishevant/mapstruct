/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

import java.util.Collections;
import java.util.List;

/**
 * Language-neutral representation of an annotation value.
 */
public interface AnnotationValueDescriptor {

    /**
     * @return raw value exposed by the backend.
     */
    Object value();

    /**
     * @return descriptor view when the value represents a type, {@code null} otherwise.
     */
    default TypeDescriptor asType() {
        return null;
    }

    /**
     * @return immutable list of nested annotation values when the attribute represents an array, empty otherwise.
     */
    default List<AnnotationValueDescriptor> asList() {
        return Collections.emptyList();
    }

    /**
     * @return descriptor view when the value represents an annotation, {@code null} otherwise.
     */
    default AnnotationDescriptor asAnnotation() {
        return null;
    }

    /**
     * @return backend-specific annotation value handle.
     */
    Object unwrap();
}
