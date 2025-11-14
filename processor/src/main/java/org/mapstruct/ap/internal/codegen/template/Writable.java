/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen.template;

import java.io.Writer;

/**
 * An element that can render itself into a {@link Writer} using a templating engine.
 */
public interface Writable {

    /**
     * Context passed to {@link Writable}s, providing access to shared objects required during rendering.
     */
    interface Context {

        /**
         * Retrieves the object with the given type from this context.
         *
         * @param type the type of the object to retrieve from this context
         * @param <T> the type parameter
         *
         * @return the object with the given type from this context, or {@code null} if it is not present
         */
        <T> T get(Class<T> type);
    }

    /**
     * Renders this element to the provided {@link Writer}.
     *
     * @param context provides shared objects specific to the rendering mechanism
     * @param writer the writer to write this element to; implementors must not close it
     *
     * @throws Exception in case of an error during rendering
     */
    void write(Context context, Writer writer) throws Exception;
}
