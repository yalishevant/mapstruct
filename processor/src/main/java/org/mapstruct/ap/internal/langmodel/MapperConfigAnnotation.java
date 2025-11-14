/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import java.util.List;

import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * Descriptor-style view over {@code @MapperConfig}.
 */
public interface MapperConfigAnnotation {

    AnnotationDescriptor descriptor();

    AnnotationAttribute<String> implementationName();

    AnnotationAttribute<String> implementationPackage();

    AnnotationAttribute<List<TypeDescriptor>> uses();

    AnnotationAttribute<List<TypeDescriptor>> imports();

    AnnotationAttribute<String> unmappedTargetPolicy();

    AnnotationAttribute<String> unmappedSourcePolicy();

    AnnotationAttribute<String> typeConversionPolicy();

    AnnotationAttribute<String> componentModel();

    AnnotationAttribute<Boolean> suppressTimestampInGenerated();

    AnnotationAttribute<String> mappingInheritanceStrategy();

    AnnotationAttribute<String> injectionStrategy();

    AnnotationAttribute<Boolean> disableSubMappingMethodsGeneration();

    AnnotationAttribute<String> collectionMappingStrategy();

    AnnotationAttribute<String> nullValueCheckStrategy();

    AnnotationAttribute<String> nullValuePropertyMappingStrategy();

    AnnotationAttribute<String> nullValueMappingStrategy();

    AnnotationAttribute<String> subclassExhaustiveStrategy();

    AnnotationAttribute<TypeDescriptor> subclassExhaustiveException();

    AnnotationAttribute<String> nullValueIterableMappingStrategy();

    AnnotationAttribute<String> nullValueMapMappingStrategy();

    AnnotationAttribute<BuilderGem> builder();

    AnnotationAttribute<TypeDescriptor> mappingControl();

    AnnotationAttribute<TypeDescriptor> unexpectedValueMappingException();
}
