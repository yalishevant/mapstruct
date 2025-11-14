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
 * Base accessor implementation that forwards all operations to a delegate {@link Accessor}.
 *
 * @author Filip Hrisafov
 */
public abstract class DelegateAccessor implements Accessor {

    protected final Accessor delegate;

    protected DelegateAccessor(Accessor delegate) {
        this.delegate = delegate;
    }

    @Override
    public TypeDescriptor getAccessedType() {
        return delegate.getAccessedType();
    }

    @Override
    public String getSimpleName() {
        return delegate.getSimpleName();
    }

    @Override
    public Set<LangModifier> getModifiers() {
        return delegate.getModifiers();
    }

    @Override
    public ElementDescriptor getElement() {
        return delegate.getElement();
    }

    @Override
    public AccessorType getAccessorType() {
        return delegate.getAccessorType();
    }
}
