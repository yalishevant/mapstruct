/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.annotation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.langmodel.AnnotationAttribute;
import org.mapstruct.ap.internal.langmodel.MapperAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperConfigAnnotation;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * Adapts {@link MapperAnnotationView} instances to the thin {@link MapperAnnotation} contract.
 */
public final class MapperAnnotationAdapter {

    private MapperAnnotationAdapter() {
    }

    public static MapperAnnotation wrap(MapperAnnotationView view) {
        if ( view == null ) {
            return null;
        }
        return new ViewBackedMapperAnnotation( view );
    }

    public static MapperConfigAnnotation wrap(MapperConfigAnnotationView view) {
        if ( view == null ) {
            return null;
        }
        return new ViewBackedMapperConfigAnnotation( view );
    }

    private static final class ViewBackedMapperAnnotation implements MapperAnnotation {

        private final MapperAnnotationView view;

        private ViewBackedMapperAnnotation(MapperAnnotationView view) {
            this.view = Objects.requireNonNull( view, "view" );
        }

        @Override
        public AnnotationDescriptor descriptor() {
            return view.descriptor();
        }

        @Override
        public boolean isValid() {
            return view.isValid();
        }

        @Override
        public AnnotationAttribute<String> implementationName() {
            return view.implementationName();
        }

        @Override
        public AnnotationAttribute<String> implementationPackage() {
            return view.implementationPackage();
        }

        @Override
        public AnnotationAttribute<List<TypeDescriptor>> uses() {
            return view.uses();
        }

        @Override
        public AnnotationAttribute<List<TypeDescriptor>> imports() {
            return view.imports();
        }

        @Override
        public AnnotationAttribute<String> unmappedTargetPolicy() {
            return view.unmappedTargetPolicy();
        }

        @Override
        public AnnotationAttribute<String> unmappedSourcePolicy() {
            return view.unmappedSourcePolicy();
        }

        @Override
        public AnnotationAttribute<String> typeConversionPolicy() {
            return view.typeConversionPolicy();
        }

        @Override
        public AnnotationAttribute<String> componentModel() {
            return view.componentModel();
        }

        @Override
        public AnnotationAttribute<Boolean> suppressTimestampInGenerated() {
            return view.suppressTimestampInGenerated();
        }

        @Override
        public AnnotationAttribute<String> mappingInheritanceStrategy() {
            return view.mappingInheritanceStrategy();
        }

        @Override
        public AnnotationAttribute<String> injectionStrategy() {
            return view.injectionStrategy();
        }

        @Override
        public AnnotationAttribute<Boolean> disableSubMappingMethodsGeneration() {
            return view.disableSubMappingMethodsGeneration();
        }

        @Override
        public AnnotationAttribute<String> collectionMappingStrategy() {
            return view.collectionMappingStrategy();
        }

        @Override
        public AnnotationAttribute<String> nullValueCheckStrategy() {
            return view.nullValueCheckStrategy();
        }

        @Override
        public AnnotationAttribute<String> nullValuePropertyMappingStrategy() {
            return view.nullValuePropertyMappingStrategy();
        }

        @Override
        public AnnotationAttribute<String> nullValueMappingStrategy() {
            return view.nullValueMappingStrategy();
        }

        @Override
        public AnnotationAttribute<String> subclassExhaustiveStrategy() {
            return view.subclassExhaustiveStrategy();
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> subclassExhaustiveException() {
            return view.subclassExhaustiveException();
        }

        @Override
        public AnnotationAttribute<String> nullValueIterableMappingStrategy() {
            return view.nullValueIterableMappingStrategy();
        }

        @Override
        public AnnotationAttribute<String> nullValueMapMappingStrategy() {
            return view.nullValueMapMappingStrategy();
        }

        @Override
        public AnnotationAttribute<BuilderGem> builder() {
            return view.builder();
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> mappingControl() {
            return view.mappingControl();
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> unexpectedValueMappingException() {
            return view.unexpectedValueMappingException();
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> config() {
            return view.config();
        }

        @Override
        public Optional<MapperConfigAnnotation> mapperConfig() {
            return view.mapperConfig().map( MapperAnnotationAdapter::wrap );
        }
    }

    private static final class ViewBackedMapperConfigAnnotation implements MapperConfigAnnotation {

        private final MapperConfigAnnotationView view;

        private ViewBackedMapperConfigAnnotation(MapperConfigAnnotationView view) {
            this.view = Objects.requireNonNull( view, "view" );
        }

        @Override
        public AnnotationDescriptor descriptor() {
            return view.descriptor();
        }

        @Override
        public AnnotationAttribute<String> implementationName() {
            return view.implementationName();
        }

        @Override
        public AnnotationAttribute<String> implementationPackage() {
            return view.implementationPackage();
        }

        @Override
        public AnnotationAttribute<List<TypeDescriptor>> uses() {
            return view.uses();
        }

        @Override
        public AnnotationAttribute<List<TypeDescriptor>> imports() {
            return view.imports();
        }

        @Override
        public AnnotationAttribute<String> unmappedTargetPolicy() {
            return view.unmappedTargetPolicy();
        }

        @Override
        public AnnotationAttribute<String> unmappedSourcePolicy() {
            return view.unmappedSourcePolicy();
        }

        @Override
        public AnnotationAttribute<String> typeConversionPolicy() {
            return view.typeConversionPolicy();
        }

        @Override
        public AnnotationAttribute<String> componentModel() {
            return view.componentModel();
        }

        @Override
        public AnnotationAttribute<Boolean> suppressTimestampInGenerated() {
            return view.suppressTimestampInGenerated();
        }

        @Override
        public AnnotationAttribute<String> mappingInheritanceStrategy() {
            return view.mappingInheritanceStrategy();
        }

        @Override
        public AnnotationAttribute<String> injectionStrategy() {
            return view.injectionStrategy();
        }

        @Override
        public AnnotationAttribute<Boolean> disableSubMappingMethodsGeneration() {
            return view.disableSubMappingMethodsGeneration();
        }

        @Override
        public AnnotationAttribute<String> collectionMappingStrategy() {
            return view.collectionMappingStrategy();
        }

        @Override
        public AnnotationAttribute<String> nullValueCheckStrategy() {
            return view.nullValueCheckStrategy();
        }

        @Override
        public AnnotationAttribute<String> nullValuePropertyMappingStrategy() {
            return view.nullValuePropertyMappingStrategy();
        }

        @Override
        public AnnotationAttribute<String> nullValueMappingStrategy() {
            return view.nullValueMappingStrategy();
        }

        @Override
        public AnnotationAttribute<String> subclassExhaustiveStrategy() {
            return view.subclassExhaustiveStrategy();
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> subclassExhaustiveException() {
            return view.subclassExhaustiveException();
        }

        @Override
        public AnnotationAttribute<String> nullValueIterableMappingStrategy() {
            return view.nullValueIterableMappingStrategy();
        }

        @Override
        public AnnotationAttribute<String> nullValueMapMappingStrategy() {
            return view.nullValueMapMappingStrategy();
        }

        @Override
        public AnnotationAttribute<BuilderGem> builder() {
            return view.builder();
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> mappingControl() {
            return view.mappingControl();
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> unexpectedValueMappingException() {
            return view.unexpectedValueMappingException();
        }
    }
}
