/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

import java.util.Optional;
import java.util.Set;

/**
 * Base contract for language-neutral element descriptors.
 */
public interface ElementDescriptor extends Comparable<ElementDescriptor> {

    String id();

    LangElementKind kind();

    NameDescriptor simpleName();

    Optional<ElementDescriptor> enclosingElement();

    Set<LangModifier> modifiers();

    TypeDescriptor asType();

    /**
     * @return backend-specific element handle, primarily for bridging existing infrastructure.
     */
    Object unwrap();

    @Override
    default int compareTo(ElementDescriptor other) {
        return simpleName().content().compareTo( other.simpleName().content() );
    }
}
