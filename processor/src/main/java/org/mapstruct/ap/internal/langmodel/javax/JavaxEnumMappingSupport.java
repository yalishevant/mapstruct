/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.lang.model.element.TypeElement;

import org.mapstruct.ap.internal.langmodel.api.EnumMappingSupport;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.spi.SpiBridgeCapability;
import org.mapstruct.ap.spi.EnumMappingStrategy;
import org.mapstruct.ap.spi.MapStructProcessingEnvironment;

final class JavaxEnumMappingSupport implements EnumMappingSupport {

    private final JavaxLangModelContext context;
    private final EnumMappingStrategy strategy;

    JavaxEnumMappingSupport(JavaxLangModelContext context, EnumMappingStrategy strategy) {
        this.context = context;
        this.strategy = strategy;
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
        return context.descriptorFactory().typeDescriptor( typeElement.asType() );
    }

    private static void initializeStrategy(JavaxLangModelContext context, EnumMappingStrategy strategy) {
        context.optional( SpiBridgeCapability.class ).ifPresent( bridge -> {
            MapStructProcessingEnvironment environment =
                bridge.spiEnvironment( context.processingEnvironment().getOptions() );
            strategy.init( environment );
        } );
    }

    private static TypeElement toTypeElement(TypeDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        TypeElementDescriptor descriptorElement = descriptor.typeElement().orElse( null );
        if ( descriptorElement == null ) {
            return null;
        }
        Object unwrapped = descriptorElement.unwrap();
        if ( unwrapped instanceof TypeElement ) {
            return (TypeElement) unwrapped;
        }
        return null;
    }
}
