/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapter;
import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapterFactory;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.spi.AccessorNamingStrategy;

/**
 * {@link AccessorNamingAdapterFactory} that bridges descriptor calls to {@link AccessorNamingStrategy} implementations
 * using KSP handles.
 */
final class KspAccessorNamingAdapterFactory implements AccessorNamingAdapterFactory {

    @Override
    public AccessorNamingAdapter create(AccessorNamingStrategy accessorNamingStrategy,
                                        DescriptorUnwrapper descriptorUnwrapper,
                                        LangModelContext langModelContext) {
        return new KspAccessorNamingAdapter( accessorNamingStrategy );
    }
}
