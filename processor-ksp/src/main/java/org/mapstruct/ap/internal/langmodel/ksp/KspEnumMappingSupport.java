/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSType;

import javax.lang.model.element.TypeElement;

import org.mapstruct.ap.internal.langmodel.api.EnumMappingSupport;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.spi.SpiBridgeCapability;
import org.mapstruct.ap.spi.EnumMappingStrategy;
import org.mapstruct.ap.spi.MapStructProcessingEnvironment;

/**
 * KSP implementation of {@link EnumMappingSupport}.
 */
final class KspEnumMappingSupport implements EnumMappingSupport {

    private final KspLangModelContext context;
    private final EnumMappingStrategy strategy;
    private final KspTypeAdapterFactory adapterFactory;

    KspEnumMappingSupport(KspLangModelContext context, EnumMappingStrategy strategy) {
        this.context = context;
        this.strategy = strategy;
        this.adapterFactory = new KspTypeAdapterFactory( context.resolver() );
        initializeStrategy( context, strategy );
    }

    @Override
    public String defaultNullEnumConstant(TypeDescriptor enumType) {
        TypeElement typeElement = toTypeElement( enumType );
        return typeElement != null ? strategy.getDefaultNullEnumConstant( typeElement ) : null;
    }

    @Override
    public String enumConstant(TypeDescriptor enumType, String enumConstant) {
        TypeElement typeElement = toTypeElement( enumType );
        return typeElement != null ? strategy.getEnumConstant( typeElement, enumConstant ) : null;
    }

    @Override
    public TypeDescriptor unexpectedValueMappingExceptionType() {
        TypeElement typeElement = strategy.getUnexpectedValueMappingExceptionType();
        if ( typeElement == null ) {
            return null;
        }
        // The TypeElement returned by the strategy is an adapted one, we need to handle this
        // For now, return null since KSP strategies should be configured differently
        // In the future, we may need to resolve this properly
        return null;
    }

    private static void initializeStrategy(KspLangModelContext context, EnumMappingStrategy strategy) {
        context.optional( SpiBridgeCapability.class ).ifPresent( bridge -> {
            MapStructProcessingEnvironment environment =
                bridge.spiEnvironment( context.processorOptions() );
            strategy.init( environment );
        } );
    }

    private TypeElement toTypeElement(TypeDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        TypeElementDescriptor descriptorElement = descriptor.typeElement().orElse( null );
        if ( descriptorElement == null ) {
            return null;
        }
        Object unwrapped = descriptorElement.unwrap();
        if ( unwrapped instanceof KSClassDeclaration ) {
            // Adapt KSClassDeclaration to TypeElement
            return adapterFactory.typeElement( (KSClassDeclaration) unwrapped );
        }
        return null;
    }
}
