/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.ksp;

import com.google.devtools.ksp.processing.KSPLogger;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;

import java.util.Objects;

/**
 * KSP implementation of {@link FormattingMessager}.
 */
final class KspFormattingMessager implements FormattingMessager {

    private final KSPLogger logger;
    private final boolean verbose;
    private boolean erroneous = false;

    KspFormattingMessager(KSPLogger logger, boolean verbose) {
        this.logger = Objects.requireNonNull( logger, "logger" );
        this.verbose = verbose;
    }

    @Override
    public void printMessage(Message msg, Object... args) {
        String formattedMessage = formatMessage( msg, args );

        switch ( msg.getDiagnosticKind() ) {
            case ERROR:
                erroneous = true;
                logger.error( formattedMessage, null );
                break;
            case WARNING:
            case MANDATORY_WARNING:
                logger.warn( formattedMessage, null );
                break;
            case NOTE:
                if ( verbose || msg.getDiagnosticKind() != javax.tools.Diagnostic.Kind.NOTE ) {
                    logger.info( formattedMessage, null );
                }
                break;
            default:
                logger.info( formattedMessage, null );
                break;
        }
    }

    @Override
    public void printMessage(ElementDescriptor element, Message msg, Object... args) {
        // For KSP, we just print the message without element context for now
        // Full implementation would need to track the element for error reporting
        printMessage( msg, args );
    }

    @Override
    public void printMessage(ElementDescriptor element,
                             AnnotationDescriptor annotation,
                             AnnotationValueDescriptor annotationValue,
                             Message msg, Object... args) {
        printMessage( msg, args );
    }

    @Override
    public void note(int level, Message log, Object... args) {
        if ( verbose ) {
            String indent = "  ".repeat( level );
            String formattedMessage = indent + formatMessage( log, args );
            logger.info( formattedMessage, null );
        }
    }

    @Override
    public boolean isErroneous() {
        return erroneous;
    }

    private String formatMessage(Message msg, Object[] args) {
        String description = msg.getDescription();
        if ( args == null || args.length == 0 ) {
            return description;
        }
        try {
            return String.format( description, args );
        }
        catch ( Exception e ) {
            // If formatting fails, return the raw message
            return description;
        }
    }
}
