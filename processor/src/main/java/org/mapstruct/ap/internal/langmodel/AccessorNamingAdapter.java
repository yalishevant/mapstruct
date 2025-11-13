/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.spi.MethodType;

/**
 * Backend-specific bridge translating descriptor-based accessor requests into concrete language model operations.
 */
public interface AccessorNamingAdapter {

    MethodType methodType(ExecutableDescriptor executable);

    String propertyName(ExecutableDescriptor executable);

    String elementName(ElementDescriptor element);
}
