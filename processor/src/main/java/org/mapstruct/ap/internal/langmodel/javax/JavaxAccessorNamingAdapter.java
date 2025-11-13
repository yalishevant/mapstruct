/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.spi.AccessorNamingStrategy;
import org.mapstruct.ap.spi.MethodType;
import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapter;

/**
 * Bridges descriptor-based accessor naming calls to the provided {@link AccessorNamingStrategy}.
 */
public final class JavaxAccessorNamingAdapter implements AccessorNamingAdapter {

    private final AccessorNamingStrategy delegate;

    public JavaxAccessorNamingAdapter(AccessorNamingStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public MethodType methodType(ExecutableDescriptor executable) {
        return delegate.getMethodType( executable );
    }

    @Override
    public String propertyName(ExecutableDescriptor executable) {
        return delegate.getPropertyName( executable );
    }

    @Override
    public String elementName(ElementDescriptor element) {
        if ( element instanceof ExecutableDescriptor ) {
            return delegate.getElementName( (ExecutableDescriptor) element );
        }
        return null;
    }
}
