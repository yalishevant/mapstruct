/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;

/**
 * Factory for creating {@link LangModelContext} instances backed by a specific annotation processing environment.
 *
 * <p>This indirection allows the core processor to obtain language-model abstractions without depending directly on
 * javac-specific implementations.
 */
public interface LangModelContextFactory {

    /**
     * Creates a new {@link LangModelContext} bound to the supplied mapper element.
     *
     * @param entryPoint neutral mapper entry point describing the processing round and mapper under inspection
     *
     * @return a language-model context for the supplied mapper element
     */
    LangModelContext create(MapperEntryPoint entryPoint);

    /**
     * Provides a backend-specific helper capable of unwrapping descriptors to native compiler handles.
     *
     * @return descriptor unwrapper
     */
    DescriptorUnwrapper descriptorUnwrapper();

    /**
     * Provides a backend-specific adapter factory for wiring {@link org.mapstruct.ap.spi.AccessorNamingStrategy}
     * implementations into descriptor-based utilities.
     *
     * @return accessor naming adapter factory
     */
    AccessorNamingAdapterFactory accessorNamingAdapterFactory();

    /**
     * Identifier of the backend implementation. Used for configuration when multiple factories are available.
     *
     * @return backend identifier (lower-case, non-empty) or {@code null} when the implementation does not expose an id
     */
    String backendId();

}
