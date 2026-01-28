/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import javax.lang.model.element.Name;

/**
 * Simple implementation of {@link Name} for KSP adapters.
 */
final class KspNameAdapter implements Name {

    private final String value;

    KspNameAdapter(String value) {
        this.value = value != null ? value : "";
    }

    @Override
    public boolean contentEquals(CharSequence cs) {
        return value.contentEquals( cs );
    }

    @Override
    public int length() {
        return value.length();
    }

    @Override
    public char charAt(int index) {
        return value.charAt( index );
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return value.subSequence( start, end );
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( obj instanceof Name ) {
            return contentEquals( (Name) obj );
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
