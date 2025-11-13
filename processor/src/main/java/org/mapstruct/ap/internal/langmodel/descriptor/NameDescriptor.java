/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

/**
 * Wrapper around element and type names.
 */
public interface NameDescriptor {

    /**
     * @return the textual representation of the name.
     */
    String content();

    /**
     * @param other the sequence to compare with
     * @return true when the contents are equal
     */
    boolean contentEquals(CharSequence other);
}
