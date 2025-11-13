/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.api.EnumMappingSupport;
import org.mapstruct.ap.spi.EnumMappingStrategy;
import org.mapstruct.ap.spi.MapStructProcessingEnvironment;
import org.mapstruct.ap.internal.langmodel.spi.SpiBridgeCapability;

final class JavaxEnumMappingSupport implements EnumMappingSupport {

    private final EnumMappingStrategy strategy;

    JavaxEnumMappingSupport(JavaxLangModelContext context, EnumMappingStrategy strategy) {
        this.strategy = strategy;
        initializeStrategy( context, strategy );
    }

    @Override
    public String defaultNullEnumConstant(TypeDescriptor enumType) {
        return strategy.getDefaultNullEnumConstant( enumType );
    }

    @Override
    public String enumConstant(TypeDescriptor enumType, String enumConstant) {
        return strategy.getEnumConstant( enumType, enumConstant );
    }

    @Override
    public TypeDescriptor unexpectedValueMappingExceptionType() {
        return strategy.getUnexpectedValueMappingExceptionType();
    }

    private static void initializeStrategy(JavaxLangModelContext context, EnumMappingStrategy strategy) {
        context.optional( SpiBridgeCapability.class ).ifPresent( bridge -> {
            MapStructProcessingEnvironment environment =
                bridge.spiEnvironment( context.processingEnvironment().getOptions() );
            strategy.init( environment );
        } );
    }
}
