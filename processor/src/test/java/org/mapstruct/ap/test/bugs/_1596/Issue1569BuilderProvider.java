/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.bugs._1596;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.spi.BuilderInfo;
import org.mapstruct.ap.spi.BuilderProvider;
import org.mapstruct.ap.spi.ImmutablesBuilderProvider;

public class Issue1569BuilderProvider extends ImmutablesBuilderProvider implements BuilderProvider {

    @Override
    public BuilderInfo findBuilderInfo(TypeDescriptor type) {
        TypeElementDescriptor descriptor = toTypeElement( type );
        if ( descriptor == null ) {
            return null;
        }
        return findBuilderInfo( descriptor );
    }

    @Override
    protected BuilderInfo findBuilderInfo(TypeElementDescriptor typeElement) {
        Element nativeElement = unwrapTypeElement( typeElement );
        if ( nativeElement instanceof TypeElement ) {
            TypeElement typeElementHandle = (TypeElement) nativeElement;
            if ( typeElementHandle.getQualifiedName().toString().endsWith( ".Item" ) ) {
                BuilderInfo info = findBuilderInfoForImmutables( typeElement );
                if ( info != null ) {
                    return info;
                }
            }
        }

        return super.findBuilderInfo( typeElement );
    }

    @Override
    protected BuilderInfo findBuilderInfoForImmutables(TypeElementDescriptor typeElement) {
        TypeElementDescriptor immutableElement = asImmutableElement( typeElement );
        if ( immutableElement != null ) {
            return super.findBuilderInfo( immutableElement );
        }
        return null;
    }
}
