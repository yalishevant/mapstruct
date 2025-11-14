/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;

/**
 * Prints out diagnostics raised by the annotation processor. Messages are Java format strings taking the given
 * arguments for interpolation.
 *
 * @author Sjaak Derksen
 */
public interface FormattingMessager {

    /**
     * Prints a message of the specified kind.
     *
     * @param msg  the message
     * @param args Arguments referenced by the format specifiers in the format string. If there are more arguments
     * than format specifiers, the extra arguments are ignored
     */
    void printMessage(Message msg, Object... args);

    /**
     * Prints a message of the specified kind at the location of the element.
     *
     * @param element the element to use as a position hint
     * @param msg  the message
     * @param args Arguments referenced by the format specifiers in the format string. If there are more arguments
     * than format specifiers, the extra arguments are ignored
     */
    void printMessage(ElementDescriptor element, Message msg, Object... args);

    /**
     * Prints a message of the specified kind at the location of the annotation value inside the annotation position
     * hint of the annotated element.
     *
     * @param element the annotated element
     * @param annotation the annotation containing the annotation value
     * @param value the annotation value to use as a position hint
     * @param msg the message
     * @param args Arguments referenced by the format specifiers in the format string. If there are more arguments than
     * format specifiers, the extra arguments are ignored
     */
    void printMessage(ElementDescriptor element,
                      AnnotationDescriptor annotation,
                      AnnotationValueDescriptor value,
                      Message msg,
                      Object... args);

    default void printMessage(ExecutableDescriptor executable, Message msg, Object... args) {
        if ( executable == null ) {
            printMessage( msg, args );
            return;
        }
        printMessage( (ElementDescriptor) executable, msg, args );
    }

    default void printMessage(ElementDescriptor element,
                              AnnotationDescriptor annotation,
                              Message msg,
                              Object... args) {
        printMessage( element, annotation, null, msg, args );
    }

    default void printMessage(ExecutableDescriptor executable,
                              AnnotationDescriptor annotation,
                              Message msg,
                              Object... args) {
        if ( executable == null ) {
            printMessage( msg, args );
            return;
        }
        printMessage( (ElementDescriptor) executable, annotation, msg, args );
    }

    default void printMessage(ExecutableDescriptor executable,
                              AnnotationDescriptor annotation,
                              AnnotationValueDescriptor value,
                              Message msg,
                              Object... args) {
        if ( executable == null ) {
            printMessage( msg, args );
            return;
        }
        printMessage( (ElementDescriptor) executable, annotation, value, msg, args );
    }

    /**
     * Just log as plain note
     * @param level nesting level
     * @param log the log message
     * @param args the arguments
     */
    void note(int level, Message log, Object... args);

    boolean isErroneous();
}
