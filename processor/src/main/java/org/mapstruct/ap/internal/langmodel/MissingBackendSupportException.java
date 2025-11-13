/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import java.util.Locale;
import java.util.Objects;

/**
 * Signals that the selected language model backend cannot operate in the active processing environment.
 */
public final class MissingBackendSupportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private MissingBackendSupportException(String message) {
        super( message );
    }

    /**
     * Creates an exception describing that the supplied backend cannot be used because a required runtime component is
     * missing.
     *
     * @param backendId backend identifier (may be {@code null})
     * @param requirement textual description of the missing component
     *
     * @return exception instance carrying a descriptive message
     */
    public static MissingBackendSupportException unsupported(String backendId, String requirement) {
        Objects.requireNonNull( requirement, "requirement" );
        String backendLabel = backendId == null || backendId.trim().isEmpty()
            ? "Selected"
            : backendId.trim().toUpperCase( Locale.ROOT );
        String message = backendLabel + " backend requires " + requirement;
        return new MissingBackendSupportException( message );
    }
}
