/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util.accessor;

import java.util.Set;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * Descriptor-based accessor abstraction used for reading/writing bean properties.
 *
 * @author Filip Hrisafov
 */
public interface Accessor {

    /**
     * @return the descriptor of the accessed type (return type for methods, declared type for fields)
     */
    TypeDescriptor getAccessedType();

    /**
     * @return the simple name of the accessor
     */
    String getSimpleName();

    /**
     * @return the set of modifiers that the accessor has
     */
    Set<LangModifier> getModifiers();

    /**
     * @return the underlying descriptor of the accessor element
     */
    ElementDescriptor getElement();

    /**
     * @return type of the accessor
     */
    AccessorType getAccessorType();
}
