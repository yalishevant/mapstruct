/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class for converting Kotlin Sequences to Java Iterables.
 * KSP API returns Kotlin Sequences which cannot be directly iterated in Java.
 */
public final class KspSequenceUtils {

    private KspSequenceUtils() {
    }

    /**
     * Converts a Kotlin Sequence to a Java List.
     *
     * @param sequence the Kotlin Sequence
     * @param <T> the element type
     * @return a List containing all elements from the sequence
     */
    public static <T> List<T> toList(Sequence<T> sequence) {
        if ( sequence == null ) {
            return new ArrayList<>();
        }
        return SequencesKt.toList( sequence );
    }

    /**
     * Converts a Kotlin Sequence to a Java Iterable.
     *
     * @param sequence the Kotlin Sequence
     * @param <T> the element type
     * @return an Iterable over the sequence elements
     */
    public static <T> Iterable<T> toIterable(Sequence<T> sequence) {
        return toList( sequence );
    }

    /**
     * Checks if a Kotlin Sequence is empty.
     *
     * @param sequence the Kotlin Sequence
     * @param <T> the element type
     * @return true if the sequence has no elements
     */
    public static <T> boolean isEmpty(Sequence<T> sequence) {
        if ( sequence == null ) {
            return true;
        }
        Iterator<T> iterator = sequence.iterator();
        return !iterator.hasNext();
    }

    /**
     * Gets the first element of a Kotlin Sequence, or null if empty.
     *
     * @param sequence the Kotlin Sequence
     * @param <T> the element type
     * @return the first element, or null if the sequence is empty
     */
    public static <T> T firstOrNull(Sequence<T> sequence) {
        if ( sequence == null ) {
            return null;
        }
        return SequencesKt.firstOrNull( sequence );
    }
}
