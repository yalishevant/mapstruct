/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

import java.util.Collections;
import java.util.List;

/**
 * Descriptor for type elements (classes, interfaces, enums, records).
 */
public interface TypeElementDescriptor extends ElementDescriptor {

    String qualifiedName();

    /**
     * @return descriptor representing this element as a declared type.
     */
    TypeDescriptor asType();

    boolean isRecord();

    boolean isSealed();

    /**
     * @return descriptors of permitted subclasses when this type is sealed, or an empty list otherwise.
     */
    default List<TypeDescriptor> permittedSubclasses() {
        return Collections.emptyList();
    }
}
