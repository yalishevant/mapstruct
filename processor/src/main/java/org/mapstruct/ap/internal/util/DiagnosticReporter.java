/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

/**
 * Minimal diagnostic reporter abstraction used to bridge compiler-specific messagers with the descriptor-first
 * service layer.
 */
public interface DiagnosticReporter {

    /**
     * Emits an informational note. Callers are expected to gate verbose messages themselves.
     *
     * @param message note payload
     */
    void note(String message);

    /**
     * Emits a warning diagnostic.
     *
     * @param message warning payload
     */
    void warning(String message);

    /**
     * Emits an error diagnostic.
     *
     * @param message error payload
     */
    void error(String message);
}
