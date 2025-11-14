/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.Optional;

import javax.lang.model.element.TypeElement;

import org.mapstruct.ap.internal.langmodel.MapperAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperConfigAnnotation;
import org.mapstruct.ap.internal.langmodel.annotation.MapperAnnotationAdapter;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * Factory for {@link MapperAnnotationView} backed by {@code javax.lang.model}.
 */
public final class JavaxMapperAnnotations {

    private JavaxMapperAnnotations() {
    }

    public static MapperAnnotation mapper(JavaxLangModelContext context, TypeElement element) {
        return MapperAnnotationAdapter.wrap( JavaxMapperAnnotation.from( context, element ) );
    }

    public static Optional<MapperConfigAnnotation> mapperConfig(JavaxLangModelContext context,
                                                                TypeDescriptor config) {
        return JavaxMapperConfigAnnotation.from( context, config )
            .map( MapperAnnotationAdapter::wrap );
    }
}
