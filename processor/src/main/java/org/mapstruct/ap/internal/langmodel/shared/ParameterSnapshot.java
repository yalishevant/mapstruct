/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.shared;

import java.util.List;
import java.util.Objects;

public final class ParameterSnapshot {

    private final String name;
    private final String type;
    private final boolean varArgs;
    private final List<String> modifiers;
    private final List<AnnotationSnapshot> annotations;

    public ParameterSnapshot(String name,
                             String type,
                             boolean varArgs,
                             List<String> modifiers,
                             List<AnnotationSnapshot> annotations) {
        this.name = name;
        this.type = type;
        this.varArgs = varArgs;
        this.modifiers = modifiers;
        this.annotations = annotations;
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public boolean varArgs() {
        return varArgs;
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
        if ( !( o instanceof ParameterSnapshot ) ) {
            return false;
        }
        ParameterSnapshot that = (ParameterSnapshot) o;
        return varArgs == that.varArgs
            && Objects.equals( name, that.name )
            && Objects.equals( type, that.type )
            && Objects.equals( modifiers, that.modifiers )
            && Objects.equals( annotations, that.annotations );
    }

    @Override
    public int hashCode() {
        return Objects.hash( name, type, varArgs, modifiers, annotations );
    }
}
