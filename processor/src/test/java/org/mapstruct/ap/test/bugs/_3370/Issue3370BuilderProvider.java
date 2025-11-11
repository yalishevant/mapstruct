/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.bugs._3370;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.spi.BuilderInfo;
import org.mapstruct.ap.spi.BuilderProvider;
import org.mapstruct.ap.spi.ImmutablesBuilderProvider;

public class Issue3370BuilderProvider extends ImmutablesBuilderProvider implements BuilderProvider {

    @Override
    public BuilderInfo findBuilderInfo(TypeDescriptor type) {
        return super.findBuilderInfo( type );
    }

    @Override
    protected BuilderInfo findBuilderInfoForImmutables(TypeElementDescriptor typeElement) {
        Element nativeElement = unwrapTypeElement( typeElement );
        if ( nativeElement instanceof TypeElement ) {
            TypeElement typeElementHandle = (TypeElement) nativeElement;
            if ( typeElementHandle.getQualifiedName().toString().endsWith( ".Item" ) ) {
                return super.findBuilderInfo( typeElement, false );
            }
        }
        return super.findBuilderInfoForImmutables( typeElement );
    }
}
