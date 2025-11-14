/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapter;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.spi.AccessorNamingStrategy;
import org.mapstruct.ap.spi.MethodType;

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
        ExecutableElement element = unwrapExecutable( executable );
        return element != null ? delegate.getMethodType( element ) : null;
    }

    @Override
    public String propertyName(ExecutableDescriptor executable) {
        ExecutableElement element = unwrapExecutable( executable );
        return element != null ? delegate.getPropertyName( element ) : null;
    }

    @Override
    public String elementName(ElementDescriptor element) {
        ExecutableElement executableElement = null;
        if ( element instanceof ExecutableDescriptor ) {
            executableElement = unwrapExecutable( (ExecutableDescriptor) element );
        }
        else if ( element != null ) {
            Object unwrapped = element.unwrap();
            if ( unwrapped instanceof ExecutableElement ) {
                executableElement = (ExecutableElement) unwrapped;
            }
        }
        return executableElement != null ? delegate.getElementName( executableElement ) : null;
    }

    private ExecutableElement unwrapExecutable(ExecutableDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        Object unwrapped = descriptor.unwrap();
        return unwrapped instanceof ExecutableElement ? (ExecutableElement) unwrapped : null;
    }
}
