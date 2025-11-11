/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.nestedbeans.exclusions.custom;

// tag::documentation[]

import java.util.regex.Pattern;

import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.spi.MappingExclusionProvider;

// end::documentation[]
/**
 * @author Filip Hrisafov
 */
// tag::documentation[]
public class CustomMappingExclusionProvider implements MappingExclusionProvider {
    private static final Pattern JAVA_JAVAX_PACKAGE = Pattern.compile( "^javax?\\..*" );

    @Override
    public boolean isExcluded(TypeDescriptor type) {
        // end::documentation[]
        //For some reason the eclipse compiler does not work when you try to do NestedTarget.class
        // tag::documentation[]
        if ( type == null ) {
            return false;
        }
        String name = type.qualifiedName().orElse( null );
        if ( name == null ) {
            name = type.displayName();
        }
        return name != null && !name.isEmpty() && ( JAVA_JAVAX_PACKAGE.matcher( name ).matches() ||
            "org.mapstruct.ap.test.nestedbeans.exclusions.custom.Target.NestedTarget".equals( name ) );
    }
}
// end::documentation[]
