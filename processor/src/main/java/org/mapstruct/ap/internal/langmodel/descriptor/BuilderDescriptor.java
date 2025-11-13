/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Descriptor holding metadata about a builder as discovered by the compiler-specific back end.
 */
public final class BuilderDescriptor {

    private final org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor creationMethod;
    private final Collection<org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor> buildMethods;

    public BuilderDescriptor(org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor creationMethod,
                             Collection<org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor> buildMethods) {
        this.creationMethod = Objects.requireNonNull( creationMethod, "creationMethod" );
        this.buildMethods = Collections.unmodifiableCollection(
            Objects.requireNonNull( buildMethods, "buildMethods" )
        );
    }

    public org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor creationMethod() {
        return creationMethod;
    }

    public Collection<org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor> buildMethods() {
        return buildMethods;
    }
}
