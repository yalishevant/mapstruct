/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.additionalsupportedoptions;

// tag::documentation[]
import org.mapstruct.ap.spi.DefaultEnumMappingStrategy;
import org.mapstruct.ap.spi.MapStructProcessingEnvironment;
import org.mapstruct.ap.descriptor.TypeDescriptor;

public class UnknownEnumMappingStrategy extends DefaultEnumMappingStrategy {

    private String defaultNullEnumConstant;

    @Override
    public void init(MapStructProcessingEnvironment processingEnvironment) {
        super.init( processingEnvironment );
        defaultNullEnumConstant = processingEnvironment.options().get( "myorg.custom.defaultNullEnumConstant" );
    }

    @Override
    public String getDefaultNullEnumConstant(TypeDescriptor enumType) {
        return defaultNullEnumConstant;
    }
}
// end::documentation[]
