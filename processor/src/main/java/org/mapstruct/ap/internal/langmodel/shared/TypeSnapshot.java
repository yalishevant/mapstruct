/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.shared;

import java.util.List;
import java.util.Objects;

public final class TypeSnapshot {

    private final String qualifiedName;
    private final String id;
    private final String kind;
    private final List<String> modifiers;
    private final boolean recordType;
    private final boolean sealedType;
    private final List<String> permittedSubclasses;
    private final List<AnnotationSnapshot> annotations;
    private final List<FieldSnapshot> fields;
    private final List<RecordComponentSnapshot> recordComponents;
    private final List<ExecutableSnapshot> constructors;
    private final List<ExecutableSnapshot> methods;

    public TypeSnapshot(String qualifiedName,
                        String id,
                        String kind,
                        List<String> modifiers,
                        boolean recordType,
                        boolean sealedType,
                        List<String> permittedSubclasses,
                        List<AnnotationSnapshot> annotations,
                        List<FieldSnapshot> fields,
                        List<RecordComponentSnapshot> recordComponents,
                        List<ExecutableSnapshot> constructors,
                        List<ExecutableSnapshot> methods) {
        this.qualifiedName = qualifiedName;
        this.id = id;
        this.kind = kind;
        this.modifiers = modifiers;
        this.recordType = recordType;
        this.sealedType = sealedType;
        this.permittedSubclasses = permittedSubclasses;
        this.annotations = annotations;
        this.fields = fields;
        this.recordComponents = recordComponents;
        this.constructors = constructors;
        this.methods = methods;
    }

    public String qualifiedName() {
        return qualifiedName;
    }

    public String id() {
        return id;
    }

    public String kind() {
        return kind;
    }

    public List<String> modifiers() {
        return modifiers;
    }

    public boolean recordType() {
        return recordType;
    }

    public boolean sealedType() {
        return sealedType;
    }

    public List<String> permittedSubclasses() {
        return permittedSubclasses;
    }

    public List<AnnotationSnapshot> annotations() {
        return annotations;
    }

    public List<FieldSnapshot> fields() {
        return fields;
    }

    public List<RecordComponentSnapshot> recordComponents() {
        return recordComponents;
    }

    public List<ExecutableSnapshot> constructors() {
        return constructors;
    }

    public List<ExecutableSnapshot> methods() {
        return methods;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof TypeSnapshot ) ) {
            return false;
        }
        TypeSnapshot that = (TypeSnapshot) o;
        return recordType == that.recordType
            && sealedType == that.sealedType
            && Objects.equals( qualifiedName, that.qualifiedName )
            && Objects.equals( id, that.id )
            && Objects.equals( kind, that.kind )
            && Objects.equals( modifiers, that.modifiers )
            && Objects.equals( permittedSubclasses, that.permittedSubclasses )
            && Objects.equals( annotations, that.annotations )
            && Objects.equals( fields, that.fields )
            && Objects.equals( recordComponents, that.recordComponents )
            && Objects.equals( constructors, that.constructors )
            && Objects.equals( methods, that.methods );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            qualifiedName,
            id,
            kind,
            modifiers,
            recordType,
            sealedType,
            permittedSubclasses,
            annotations,
            fields,
            recordComponents,
            constructors,
            methods
        );
    }
}
