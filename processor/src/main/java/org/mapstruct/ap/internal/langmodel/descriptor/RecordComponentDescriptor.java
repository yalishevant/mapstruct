/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

import java.util.List;

/**
 * Descriptor representing a record component.
 */
public interface RecordComponentDescriptor extends ElementDescriptor {

    TypeDescriptor componentType();

    List<AnnotationDescriptor> annotations();
}
