/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.spi;

import org.mapstruct.ap.internal.langmodel.api.EnumMappingSupport;
import org.mapstruct.ap.spi.EnumMappingStrategy;

/**
 * Optional capability exposing {@link EnumMappingSupport}.
 */
public interface EnumMappingCapability {

    /**
     * @param strategy active enum mapping strategy
     *
     * @return enum mapping support for the supplied strategy
     */
    EnumMappingSupport enumMappingSupport(EnumMappingStrategy strategy);
}
