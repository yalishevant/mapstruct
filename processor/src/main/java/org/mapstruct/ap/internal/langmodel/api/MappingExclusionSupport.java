/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.api;

import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * Abstraction for checking whether a type should be excluded from automatic sub-mapping generation.
 */
public interface MappingExclusionSupport {

    boolean isExcluded(TypeDescriptor type);
}
