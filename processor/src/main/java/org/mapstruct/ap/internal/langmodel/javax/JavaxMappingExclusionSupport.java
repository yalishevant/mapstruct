/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import org.mapstruct.ap.internal.langmodel.api.MappingExclusionSupport;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.spi.MappingExclusionProvider;

final class JavaxMappingExclusionSupport implements MappingExclusionSupport {

    private final MappingExclusionProvider provider;

    JavaxMappingExclusionSupport(MappingExclusionProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean isExcluded(TypeDescriptor type) {
        return provider.isExcluded( type );
    }
}
