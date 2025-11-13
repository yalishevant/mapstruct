/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

import java.util.Map;

/**
 * Language-neutral view over an annotation instance.
 */
public interface AnnotationDescriptor {

    TypeElementDescriptor annotationType();

    Map<String, AnnotationValueDescriptor> elementValues();

    /**
     * @param elementName the annotation attribute name
     * @return {@code true} when the attribute was explicitly declared on the annotated element, {@code false} when
     * it is absent and the backend supplied a default value
     */
    default boolean hasValue(String elementName) {
        return elementValues().containsKey( elementName );
    }

    /**
     * @return backend-specific annotation handle, primarily for bridging existing infrastructure.
     */
    Object unwrap();
}
