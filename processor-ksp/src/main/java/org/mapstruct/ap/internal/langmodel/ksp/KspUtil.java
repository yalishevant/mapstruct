/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for working with KSP and Kotlin types from Java.
 */
final class KspUtil {

    private KspUtil() {
    }

    /**
     * Converts a Kotlin Sequence to a Java List.
     */
    static <T> List<T> toList(Sequence<T> sequence) {
        if ( sequence == null ) {
            return Collections.emptyList();
        }
        return SequencesKt.toList( sequence );
    }

    /**
     * Converts a Kotlin Sequence to a Java Iterable.
     */
    static <T> Iterable<T> toIterable(Sequence<T> sequence) {
        if ( sequence == null ) {
            return Collections.emptyList();
        }
        return SequencesKt.asIterable( sequence );
    }

    /**
     * Gets the first element of a Sequence or null if empty.
     */
    static <T> T firstOrNull(Sequence<T> sequence) {
        if ( sequence == null ) {
            return null;
        }
        return SequencesKt.firstOrNull( sequence );
    }

    /**
     * Checks if a Sequence is empty.
     */
    static <T> boolean isEmpty(Sequence<T> sequence) {
        if ( sequence == null ) {
            return true;
        }
        return SequencesKt.none( sequence );
    }
}
