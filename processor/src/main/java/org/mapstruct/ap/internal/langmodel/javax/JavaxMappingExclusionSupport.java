/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.lang.model.element.TypeElement;

import org.mapstruct.ap.internal.langmodel.api.MappingExclusionSupport;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.spi.MappingExclusionProvider;

final class JavaxMappingExclusionSupport implements MappingExclusionSupport {

    private final MappingExclusionProvider provider;

    JavaxMappingExclusionSupport(MappingExclusionProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean isExcluded(TypeDescriptor type) {
        TypeElement element = toTypeElement( type );
        return element != null && provider.isExcluded( element );
    }

    private TypeElement toTypeElement(TypeDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        TypeElementDescriptor elementDescriptor = descriptor.typeElement().orElse( null );
        if ( elementDescriptor == null ) {
            return null;
        }
        Object unwrapped = elementDescriptor.unwrap();
        return unwrapped instanceof TypeElement ? (TypeElement) unwrapped : null;
    }
}
