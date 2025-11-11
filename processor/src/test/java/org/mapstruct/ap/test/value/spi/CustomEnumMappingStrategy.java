/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.value.spi;

import org.mapstruct.MappingConstants;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.gem.MappingConstantsGem;
import org.mapstruct.ap.spi.DefaultEnumMappingStrategy;
import org.mapstruct.ap.spi.EnumMappingStrategy;
import org.mapstruct.ap.test.value.CustomIllegalArgumentException;

/**
 * @author Filip Hrisafov
 */
public class CustomEnumMappingStrategy extends DefaultEnumMappingStrategy implements EnumMappingStrategy {

    @Override
    public String getDefaultNullEnumConstant(TypeDescriptor enumType) {
        if ( isCustomThrowingEnum( enumType ) ) {
            return MappingConstants.THROW_EXCEPTION;
        }

        if ( isCustomEnum( enumType ) ) {
            return "UNSPECIFIED";
        }

        return super.getDefaultNullEnumConstant( enumType );
    }

    @Override
    public String getEnumConstant(TypeDescriptor enumType, String enumConstant) {
        if ( isCustomThrowingEnum( enumType ) ) {
            return getCustomEnumConstant( enumConstant );
        }

        if ( isCustomEnum( enumType ) ) {
            return getCustomEnumConstant( enumConstant );
        }
        return super.getEnumConstant( enumType, enumConstant );
    }

    protected String getCustomEnumConstant(String enumConstant) {
        if ( "UNRECOGNIZED".equals( enumConstant ) || "UNSPECIFIED".equals( enumConstant ) ) {
            return MappingConstantsGem.NULL;
        }

        return enumConstant.replace( "CUSTOM_", "" );
    }

    protected boolean isCustomEnum(TypeDescriptor enumType) {
        return hasMarkerInterface( enumType, "org.mapstruct.ap.test.value.spi.CustomEnumMarker" );
    }

    protected boolean isCustomThrowingEnum(TypeDescriptor enumType) {
        return hasMarkerInterface( enumType, "org.mapstruct.ap.test.value.spi.CustomThrowingEnumMarker" );
    }

    @Override
    protected Class<? extends Exception> getUnexpectedValueMappingExceptionClass() {
        return CustomIllegalArgumentException.class;
    }

    private boolean hasMarkerInterface(TypeDescriptor enumType, String markerQualifiedName) {
        if ( enumType == null || types == null ) {
            return false;
        }

        for ( TypeDescriptor superType : types.directSupertypes( enumType ) ) {
            if ( superType == null ) {
                continue;
            }

            if ( superType.qualifiedName().filter( markerQualifiedName::equals ).isPresent() ) {
                return true;
            }

            TypeElementDescriptor descriptor = types.asElement( superType );
            if ( descriptor != null && markerQualifiedName.equals( descriptor.qualifiedName() ) ) {
                return true;
            }
        }

        return false;
    }
}
