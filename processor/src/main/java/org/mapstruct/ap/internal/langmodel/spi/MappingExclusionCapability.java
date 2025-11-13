/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.spi;

import org.mapstruct.ap.internal.langmodel.api.MappingExclusionSupport;
import org.mapstruct.ap.spi.MappingExclusionProvider;

/**
 * Optional capability bridging {@link MappingExclusionProvider} implementations to descriptor-based lookups.
 */
public interface MappingExclusionCapability {

    MappingExclusionSupport mappingExclusionSupport(MappingExclusionProvider provider);
}
