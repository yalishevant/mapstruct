/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.shared;

import java.util.List;
import java.util.Objects;

public final class ExecutableSnapshot {

    private final String name;
    private final String kind;
    private final String returnType;
    private final List<ParameterSnapshot> parameters;
    private final List<String> typeParameters;
    private final List<String> thrownTypes;
    private final List<String> modifiers;
    private final boolean defaultMethod;
    private final List<AnnotationSnapshot> annotations;

    public ExecutableSnapshot(String name,
                              String kind,
                              String returnType,
                              List<ParameterSnapshot> parameters,
                              List<String> typeParameters,
                              List<String> thrownTypes,
                              List<String> modifiers,
                              boolean defaultMethod,
                              List<AnnotationSnapshot> annotations) {
        this.name = name;
        this.kind = kind;
        this.returnType = returnType;
        this.parameters = parameters;
        this.typeParameters = typeParameters;
        this.thrownTypes = thrownTypes;
        this.modifiers = modifiers;
        this.defaultMethod = defaultMethod;
        this.annotations = annotations;
    }

    public String name() {
        return name;
    }

    public String kind() {
        return kind;
    }

    public String returnType() {
        return returnType;
    }

    public List<ParameterSnapshot> parameters() {
        return parameters;
    }

    public List<String> typeParameters() {
        return typeParameters;
    }

    public List<String> thrownTypes() {
        return thrownTypes;
    }

    public List<String> modifiers() {
        return modifiers;
    }

    public boolean defaultMethod() {
        return defaultMethod;
    }

    public List<AnnotationSnapshot> annotations() {
        return annotations;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof ExecutableSnapshot ) ) {
            return false;
        }
        ExecutableSnapshot that = (ExecutableSnapshot) o;
        return defaultMethod == that.defaultMethod
            && Objects.equals( name, that.name )
            && Objects.equals( kind, that.kind )
            && Objects.equals( returnType, that.returnType )
            && Objects.equals( parameters, that.parameters )
            && Objects.equals( typeParameters, that.typeParameters )
            && Objects.equals( thrownTypes, that.thrownTypes )
            && Objects.equals( modifiers, that.modifiers )
            && Objects.equals( annotations, that.annotations );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            name,
            kind,
            returnType,
            parameters,
            typeParameters,
            thrownTypes,
            modifiers,
            defaultMethod,
            annotations
        );
    }

    @Override
    public String toString() {
        return name + parameters;
    }
}
