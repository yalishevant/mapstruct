/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

import java.util.Objects;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

/**
 * {@link DiagnosticReporter} backed by the annotation processing {@link Messager}.
 */
public final class MessagerDiagnosticReporter implements DiagnosticReporter {

    private final Messager messager;

    public MessagerDiagnosticReporter(Messager messager) {
        this.messager = Objects.requireNonNull( messager, "messager" );
    }

    @Override
    public void note(String message) {
        messager.printMessage( Diagnostic.Kind.NOTE, message );
    }

    @Override
    public void warning(String message) {
        messager.printMessage( Diagnostic.Kind.WARNING, message );
    }

    @Override
    public void error(String message) {
        messager.printMessage( Diagnostic.Kind.ERROR, message );
    }
}
