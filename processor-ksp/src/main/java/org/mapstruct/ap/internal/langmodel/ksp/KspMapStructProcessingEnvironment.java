/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import org.mapstruct.ap.spi.MapStructProcessingEnvironment;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Map;
import java.util.Objects;

/**
 * KSP implementation of {@link MapStructProcessingEnvironment}.
 * <p>
 * Note: This is a minimal implementation. The {@link Elements} and {@link Types}
 * utilities return stub implementations since KSP uses a different API model.
 * SPI implementations that require full javax.lang.model support may not work correctly.
 */
final class KspMapStructProcessingEnvironment implements MapStructProcessingEnvironment {

    private final Map<String, String> options;
    private final Elements stubElements;
    private final Types stubTypes;

    KspMapStructProcessingEnvironment(Map<String, String> options) {
        this.options = Objects.requireNonNull( options, "options" );
        this.stubElements = new KspStubElements();
        this.stubTypes = new KspStubTypes();
    }

    @Override
    public Elements getElementUtils() {
        return stubElements;
    }

    @Override
    public Types getTypeUtils() {
        return stubTypes;
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }
}
