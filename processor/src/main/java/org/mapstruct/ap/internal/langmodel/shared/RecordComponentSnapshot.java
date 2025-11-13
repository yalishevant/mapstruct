/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.shared;

import java.util.List;
import java.util.Objects;

public final class RecordComponentSnapshot {

    private final String name;
    private final String type;
    private final List<AnnotationSnapshot> annotations;

    public RecordComponentSnapshot(String name, String type, List<AnnotationSnapshot> annotations) {
        this.name = name;
        this.type = type;
        this.annotations = annotations;
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public List<AnnotationSnapshot> annotations() {
        return annotations;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof RecordComponentSnapshot ) ) {
            return false;
        }
        RecordComponentSnapshot that = (RecordComponentSnapshot) o;
        return Objects.equals( name, that.name )
            && Objects.equals( type, that.type )
            && Objects.equals( annotations, that.annotations );
    }

    @Override
    public int hashCode() {
        return Objects.hash( name, type, annotations );
    }
}
