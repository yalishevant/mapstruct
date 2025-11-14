/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * Signals issues encountered while discovering builder metadata.
 */
public class BuilderIntrospectionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final TypeDescriptor type;
    private final Collection<ExecutableDescriptor> conflictingCreationMethods;

    public BuilderIntrospectionException(TypeDescriptor type,
                                         Collection<ExecutableDescriptor> conflictingCreationMethods) {
        this.type = type;
        this.conflictingCreationMethods = Collections.unmodifiableCollection(
            Objects.requireNonNull( conflictingCreationMethods, "conflictingCreationMethods" )
        );
    }

    public TypeDescriptor getType() {
        return type;
    }

    public Collection<ExecutableDescriptor> getConflictingCreationMethods() {
        return conflictingCreationMethods;
    }
}
