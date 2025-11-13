/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.shared;

import java.util.List;
import java.util.Objects;

public final class FieldSnapshot {

    private final String name;
    private final String kind;
    private final String type;
    private final List<String> modifiers;
    private final List<AnnotationSnapshot> annotations;

    public FieldSnapshot(String name,
                         String kind,
                         String type,
                         List<String> modifiers,
                         List<AnnotationSnapshot> annotations) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.modifiers = modifiers;
        this.annotations = annotations;
    }

    public String name() {
        return name;
    }

    public String kind() {
        return kind;
    }

    public String type() {
        return type;
    }

    public List<String> modifiers() {
        return modifiers;
    }

    public List<AnnotationSnapshot> annotations() {
        return annotations;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof FieldSnapshot ) ) {
            return false;
        }
        FieldSnapshot that = (FieldSnapshot) o;
        return Objects.equals( name, that.name )
            && Objects.equals( kind, that.kind )
            && Objects.equals( type, that.type )
            && Objects.equals( modifiers, that.modifiers )
            && Objects.equals( annotations, that.annotations );
    }

    @Override
    public int hashCode() {
        return Objects.hash( name, kind, type, modifiers, annotations );
    }
}
