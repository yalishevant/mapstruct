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
 * {@link Accessor} implementation backed by a descriptor element (methods, fields, parameters, etc.).
 *
 * @author Filip Hrisafov
 * @author Tang Yang
 */
public class ElementAccessor implements Accessor {

    private final ElementDescriptor element;
    private final String name;
    private final AccessorType accessorType;
    private final TypeDescriptor accessedType;

    public ElementAccessor(ElementDescriptor element, TypeDescriptor accessedType) {
        this( element, accessedType, AccessorType.FIELD, null );
    }

    public ElementAccessor(ElementDescriptor element, TypeDescriptor accessedType, String name) {
        this( element, accessedType, AccessorType.PARAMETER, name );
    }

    public ElementAccessor(ElementDescriptor element, TypeDescriptor accessedType, AccessorType accessorType) {
        this( element, accessedType, accessorType, null );
    }

    private ElementAccessor(ElementDescriptor element, TypeDescriptor accessedType,
                            AccessorType accessorType, String name) {
        this.element = element;
        this.accessedType = accessedType;
        this.accessorType = accessorType;
        this.name = name;
    }

    @Override
    public TypeDescriptor getAccessedType() {
        if ( accessedType != null ) {
            return accessedType;
        }
        return element != null ? element.asType() : null;
    }

    @Override
    public String getSimpleName() {
        if ( name != null ) {
            return name;
        }
        return element != null && element.simpleName() != null ? element.simpleName().content() : "";
    }

    @Override
    public Set<LangModifier> getModifiers() {
        return element != null ? element.modifiers() : java.util.Collections.emptySet();
    }

    @Override
    public ElementDescriptor getElement() {
        return element;
    }

    @Override
    public String toString() {
        if ( element == null ) {
            return "null";
        }
        return element.simpleName().content() + "(" + element.kind() + ")";
    }

    @Override
    public AccessorType getAccessorType() {
        return accessorType;
    }
}
