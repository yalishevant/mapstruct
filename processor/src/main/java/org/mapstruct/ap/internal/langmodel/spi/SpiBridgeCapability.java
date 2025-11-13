/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.spi;

import java.util.Map;

import org.mapstruct.ap.spi.MapStructProcessingEnvironment;

/**
 * Optional capability bridging MapStruct SPI integrations to the backend.
 */
public interface SpiBridgeCapability {

    /**
     * @param options resolved processor options
     *
     * @return backend-specific processing environment view
     */
    MapStructProcessingEnvironment spiEnvironment(Map<String, String> options);
}
