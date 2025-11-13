/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

import java.util.List;

/**
 * Descriptor for field elements.
 */
public interface FieldDescriptor extends ElementDescriptor {

    TypeDescriptor fieldType();

    boolean isStatic();

    boolean isFinal();

    List<AnnotationDescriptor> annotations();
}
