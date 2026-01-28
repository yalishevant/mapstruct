/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.ksp;

import com.google.devtools.ksp.processing.KSPLogger;

import org.mapstruct.ap.internal.util.DiagnosticReporter;

import java.util.Objects;

/**
 * KSP implementation of diagnostic reporting.
 */
final class KspDiagnosticReporter implements DiagnosticReporter {

    private final KSPLogger logger;

    KspDiagnosticReporter(KSPLogger logger) {
        this.logger = Objects.requireNonNull( logger, "logger" );
    }

    @Override
    public void note(String message) {
        logger.info( message, null );
    }

    @Override
    public void warning(String message) {
        logger.warn( message, null );
    }

    @Override
    public void error(String message) {
        logger.error( message, null );
    }
}
