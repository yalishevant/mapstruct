/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.spi.AccessorNamingStrategy;

/**
 * Factory responsible for creating backend-specific adapters that bridge {@link AccessorNamingStrategy} implementations
 * with MapStruct's descriptor-first utilities.
 *
 * <p>
 * Implementations are free to inspect the supplied {@link LangModelContext} to adapt to compiler specific behaviour
 * (e.g. javac vs. Kotlin KSP) when translating descriptor invocations to native model handles.
 * </p>
 */
public interface AccessorNamingAdapterFactory {

    /**
     * Creates a descriptor-aware adapter for the provided {@link AccessorNamingStrategy}.
     *
     * @param accessorNamingStrategy the user-visible accessor naming strategy implementation
     * @param descriptorUnwrapper utility capable of converting descriptors into native compiler handles
     * @param langModelContext language-model context corresponding to the current processing round
     *
     * @return adapter used by descriptor-based accessor utilities
     */
    AccessorNamingAdapter create(AccessorNamingStrategy accessorNamingStrategy,
                                 DescriptorUnwrapper descriptorUnwrapper,
                                 LangModelContext langModelContext);
}
