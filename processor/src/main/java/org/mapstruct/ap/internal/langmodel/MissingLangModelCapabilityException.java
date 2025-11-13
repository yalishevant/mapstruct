/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import java.util.Objects;

/**
 * Signals that a mandatory language model capability was not provided by the active backend.
 */
public final class MissingLangModelCapabilityException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Class<?> capabilityType;

    private MissingLangModelCapabilityException(Class<?> capabilityType, String message) {
        super( message );
        this.capabilityType = capabilityType;
    }

    /**
     * Creates an exception indicating that the supplied capability is required by the thin API.
     *
     * @param capabilityType missing capability contract
     *
     * @return exception instance with a descriptive message
     */
    public static MissingLangModelCapabilityException required(Class<?> capabilityType) {
        Objects.requireNonNull( capabilityType, "capabilityType" );
        String simpleName = capabilityType.getSimpleName();
        String message = simpleName
            + " is required by MapStruct Thin API. Configure a backend that provides this capability "
            + "(for example -Amapstruct.langModelBackend=javax).";
        return new MissingLangModelCapabilityException( capabilityType, message );
    }

    /**
     * @return the missing capability contract that triggered the failure
     */
    public Class<?> capabilityType() {
        return capabilityType;
    }
}
