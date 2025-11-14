/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util.accessor;

import java.util.Collections;
import java.util.Set;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * An {@link Accessor} that wraps a Map value.
 *
 * @author Christian Kosmowski
 */
public class MapValueAccessor implements ReadAccessor {

    private final TypeDescriptor valueType;
    private final String simpleName;
    private final ElementDescriptor element;

    public MapValueAccessor(ElementDescriptor element, TypeDescriptor valueType, String simpleName) {
        this.element = element;
        this.valueType = valueType;
        this.simpleName = simpleName;
    }

    @Override
    public TypeDescriptor getAccessedType() {
        return valueType;
    }

    @Override
    public String getSimpleName() {
        return this.simpleName;
    }

    @Override
    public Set<LangModifier> getModifiers() {
        return Collections.emptySet();
    }

    @Override
    public ElementDescriptor getElement() {
        return this.element;
    }

    @Override
    public AccessorType getAccessorType() {
        return AccessorType.GETTER;
    }

    @Override
    public String getReadValueSource() {
        return "get( \"" + getSimpleName() + "\" )";
    }
}
