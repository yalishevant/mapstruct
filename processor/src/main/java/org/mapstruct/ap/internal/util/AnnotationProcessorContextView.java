/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

import java.util.Map;

import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.api.BuilderIntrospectorContext;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.spi.EnumMappingStrategy;
import org.mapstruct.ap.spi.EnumTransformationStrategy;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;

/**
 * Minimal view on {@link AnnotationProcessorContext} required by utility classes to avoid depending on the full
 * processor package.
 */
public interface AnnotationProcessorContextView extends BuilderIntrospectorContext {

    AccessorNamingUtils getAccessorNaming();

    Map<String, EnumTransformationStrategy> getEnumTransformationStrategies();

    EnumMappingStrategy getEnumMappingStrategy();

    default void prepare(@SuppressWarnings("unused") LangModelContext context) {
    }

    boolean isTypeComplete(TypeDescriptor descriptor);

    TypeHierarchyErroneousException typeHierarchyErroneousException(TypeDescriptor descriptor);
}
