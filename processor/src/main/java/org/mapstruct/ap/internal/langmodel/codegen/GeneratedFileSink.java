/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.codegen;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Abstraction responsible for creating writers used to emit generated sources or resources.
 */
public interface GeneratedFileSink {

    /**
     * Opens a writer for emitting a Java source file.
     *
     * @param packageName package of the generated type (may be {@code null} or empty)
     * @param simpleName simple name of the generated type
     * @param originatingElements originating elements (either native compiler handles or descriptors)
     *
     * @return writer connected to the generated source file
     *
     * @throws IOException when the underlying compiler infrastructure cannot create the file
     */
    Writer createJavaSourceWriter(String packageName, String simpleName, List<Object> originatingElements)
        throws IOException;

    /**
     * Opens a writer for emitting a resource file.
     *
     * @param resourceName resource identifier relative to the class output location
     * @param originatingElements originating elements (either native compiler handles or descriptors)
     *
     * @return writer connected to the generated resource
     *
     * @throws IOException when the underlying compiler infrastructure cannot create the resource
     */
    Writer createResourceWriter(String resourceName, List<Object> originatingElements) throws IOException;
}
