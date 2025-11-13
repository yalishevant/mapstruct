/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.codegen;

/**
 * Mandatory facade exposing access to generated file emission facilities.
 */
public interface GeneratedFileAccess {

    /**
     * @return backend-specific sink used for emitting generated files.
     */
    GeneratedFileSink generatedFileSink();
}
