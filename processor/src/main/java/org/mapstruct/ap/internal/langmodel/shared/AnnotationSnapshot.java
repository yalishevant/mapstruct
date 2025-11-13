/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.shared;

import java.util.Map;
import java.util.Objects;

/**
 * Snapshot representation of an annotation used by descriptor parity tests.
 */
public final class AnnotationSnapshot {

    private final String type;
    private final Map<String, String> values;

    public AnnotationSnapshot(String type, Map<String, String> values) {
        this.type = type;
        this.values = values;
    }

    public String type() {
        return type;
    }

    public Map<String, String> values() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof AnnotationSnapshot ) ) {
            return false;
        }
        AnnotationSnapshot that = (AnnotationSnapshot) o;
        return Objects.equals( type, that.type ) && Objects.equals( values, that.values );
    }

    @Override
    public int hashCode() {
        return Objects.hash( type, values );
    }

    @Override
    public String toString() {
        return "@" + type + values;
    }
}
