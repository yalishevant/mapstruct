/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

/**
 * Extension point to execute {@link LangModelContractProcessor} as part of the SPI descriptor contract test.
 *
 * <p>
 * The base processor already verifies that SPI implementations receive descriptor-based inputs, so this subclass
 * merely exists as a dedicated entry point for the new regression test.
 * </p>
 */
public final class LangModelSpiDescriptorContractProcessor extends LangModelContractProcessor {
}
