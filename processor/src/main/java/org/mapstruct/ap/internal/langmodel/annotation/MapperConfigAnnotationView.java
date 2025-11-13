/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.annotation;

import java.util.List;
import java.util.Objects;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.gem.BuilderGem;

/**
 * Backend-neutral representation of {@code @MapperConfig}.
 */
public final class MapperConfigAnnotationView {

    private final AnnotationDescriptor descriptor;
    private final AnnotationAttribute<String> implementationName;
    private final AnnotationAttribute<String> implementationPackage;
    private final AnnotationAttribute<List<TypeDescriptor>> uses;
    private final AnnotationAttribute<List<TypeDescriptor>> imports;
    private final AnnotationAttribute<String> unmappedTargetPolicy;
    private final AnnotationAttribute<String> unmappedSourcePolicy;
    private final AnnotationAttribute<String> typeConversionPolicy;
    private final AnnotationAttribute<String> componentModel;
    private final AnnotationAttribute<Boolean> suppressTimestampInGenerated;
    private final AnnotationAttribute<String> mappingInheritanceStrategy;
    private final AnnotationAttribute<String> injectionStrategy;
    private final AnnotationAttribute<Boolean> disableSubMappingMethodsGeneration;
    private final AnnotationAttribute<String> collectionMappingStrategy;
    private final AnnotationAttribute<String> nullValueCheckStrategy;
    private final AnnotationAttribute<String> nullValuePropertyMappingStrategy;
    private final AnnotationAttribute<String> nullValueMappingStrategy;
    private final AnnotationAttribute<String> subclassExhaustiveStrategy;
    private final AnnotationAttribute<TypeDescriptor> subclassExhaustiveException;
    private final AnnotationAttribute<String> nullValueIterableMappingStrategy;
    private final AnnotationAttribute<String> nullValueMapMappingStrategy;
    private final AnnotationAttribute<BuilderGem> builder;
    private final AnnotationAttribute<TypeDescriptor> mappingControl;
    private final AnnotationAttribute<TypeDescriptor> unexpectedValueMappingException;

    private MapperConfigAnnotationView(AnnotationDescriptor descriptor,
                                       AnnotationAttribute<String> implementationName,
                                       AnnotationAttribute<String> implementationPackage,
                                       AnnotationAttribute<List<TypeDescriptor>> uses,
                                       AnnotationAttribute<List<TypeDescriptor>> imports,
                                       AnnotationAttribute<String> unmappedTargetPolicy,
                                       AnnotationAttribute<String> unmappedSourcePolicy,
                                       AnnotationAttribute<String> typeConversionPolicy,
                                       AnnotationAttribute<String> componentModel,
                                       AnnotationAttribute<Boolean> suppressTimestampInGenerated,
                                       AnnotationAttribute<String> mappingInheritanceStrategy,
                                       AnnotationAttribute<String> injectionStrategy,
                                       AnnotationAttribute<Boolean> disableSubMappingMethodsGeneration,
                                       AnnotationAttribute<String> collectionMappingStrategy,
                                       AnnotationAttribute<String> nullValueCheckStrategy,
                                       AnnotationAttribute<String> nullValuePropertyMappingStrategy,
                                       AnnotationAttribute<String> nullValueMappingStrategy,
                                       AnnotationAttribute<String> subclassExhaustiveStrategy,
                                       AnnotationAttribute<TypeDescriptor> subclassExhaustiveException,
                                       AnnotationAttribute<String> nullValueIterableMappingStrategy,
                                       AnnotationAttribute<String> nullValueMapMappingStrategy,
                                       AnnotationAttribute<BuilderGem> builder,
                                       AnnotationAttribute<TypeDescriptor> mappingControl,
                                       AnnotationAttribute<TypeDescriptor> unexpectedValueMappingException) {
        this.descriptor = descriptor;
        this.implementationName = Objects.requireNonNull( implementationName );
        this.implementationPackage = Objects.requireNonNull( implementationPackage );
        this.uses = Objects.requireNonNull( uses );
        this.imports = Objects.requireNonNull( imports );
        this.unmappedTargetPolicy = Objects.requireNonNull( unmappedTargetPolicy );
        this.unmappedSourcePolicy = Objects.requireNonNull( unmappedSourcePolicy );
        this.typeConversionPolicy = Objects.requireNonNull( typeConversionPolicy );
        this.componentModel = Objects.requireNonNull( componentModel );
        this.suppressTimestampInGenerated = Objects.requireNonNull( suppressTimestampInGenerated );
        this.mappingInheritanceStrategy = Objects.requireNonNull( mappingInheritanceStrategy );
        this.injectionStrategy = Objects.requireNonNull( injectionStrategy );
        this.disableSubMappingMethodsGeneration = Objects.requireNonNull( disableSubMappingMethodsGeneration );
        this.collectionMappingStrategy = Objects.requireNonNull( collectionMappingStrategy );
        this.nullValueCheckStrategy = Objects.requireNonNull( nullValueCheckStrategy );
        this.nullValuePropertyMappingStrategy = Objects.requireNonNull( nullValuePropertyMappingStrategy );
        this.nullValueMappingStrategy = Objects.requireNonNull( nullValueMappingStrategy );
        this.subclassExhaustiveStrategy = Objects.requireNonNull( subclassExhaustiveStrategy );
        this.subclassExhaustiveException = Objects.requireNonNull( subclassExhaustiveException );
        this.nullValueIterableMappingStrategy = Objects.requireNonNull( nullValueIterableMappingStrategy );
        this.nullValueMapMappingStrategy = Objects.requireNonNull( nullValueMapMappingStrategy );
        this.builder = Objects.requireNonNull( builder );
        this.mappingControl = Objects.requireNonNull( mappingControl );
        this.unexpectedValueMappingException = Objects.requireNonNull( unexpectedValueMappingException );
    }

    public static MapperConfigAnnotationView create(AnnotationDescriptor descriptor,
                                                    AnnotationAttribute<String> implementationName,
                                                    AnnotationAttribute<String> implementationPackage,
                                                    AnnotationAttribute<List<TypeDescriptor>> uses,
                                                    AnnotationAttribute<List<TypeDescriptor>> imports,
                                                    AnnotationAttribute<String> unmappedTargetPolicy,
                                                    AnnotationAttribute<String> unmappedSourcePolicy,
                                                    AnnotationAttribute<String> typeConversionPolicy,
                                                    AnnotationAttribute<String> componentModel,
                                                    AnnotationAttribute<Boolean> suppressTimestampInGenerated,
                                                    AnnotationAttribute<String> mappingInheritanceStrategy,
                                                    AnnotationAttribute<String> injectionStrategy,
                                                    AnnotationAttribute<Boolean> disableSubMappingMethodsGeneration,
                                                    AnnotationAttribute<String> collectionMappingStrategy,
                                                    AnnotationAttribute<String> nullValueCheckStrategy,
                                                    AnnotationAttribute<String> nullValuePropertyMappingStrategy,
                                                    AnnotationAttribute<String> nullValueMappingStrategy,
                                                    AnnotationAttribute<String> subclassExhaustiveStrategy,
                                                    AnnotationAttribute<TypeDescriptor> subclassExhaustiveException,
                                                    AnnotationAttribute<String> nullValueIterableMappingStrategy,
                                                    AnnotationAttribute<String> nullValueMapMappingStrategy,
                                                    AnnotationAttribute<BuilderGem> builder,
                                                    AnnotationAttribute<TypeDescriptor> mappingControl,
                                                    AnnotationAttribute<TypeDescriptor> unexpectedValueMappingException) {
        return new MapperConfigAnnotationView(
            descriptor,
            implementationName,
            implementationPackage,
            uses,
            imports,
            unmappedTargetPolicy,
            unmappedSourcePolicy,
            typeConversionPolicy,
            componentModel,
            suppressTimestampInGenerated,
            mappingInheritanceStrategy,
            injectionStrategy,
            disableSubMappingMethodsGeneration,
            collectionMappingStrategy,
            nullValueCheckStrategy,
            nullValuePropertyMappingStrategy,
            nullValueMappingStrategy,
            subclassExhaustiveStrategy,
            subclassExhaustiveException,
            nullValueIterableMappingStrategy,
            nullValueMapMappingStrategy,
            builder,
            mappingControl,
            unexpectedValueMappingException
        );
    }

    public AnnotationDescriptor descriptor() {
        return descriptor;
    }

    public AnnotationAttribute<String> implementationName() {
        return implementationName;
    }

    public AnnotationAttribute<String> implementationPackage() {
        return implementationPackage;
    }

    public AnnotationAttribute<List<TypeDescriptor>> uses() {
        return uses;
    }

    public AnnotationAttribute<List<TypeDescriptor>> imports() {
        return imports;
    }

    public AnnotationAttribute<String> unmappedTargetPolicy() {
        return unmappedTargetPolicy;
    }

    public AnnotationAttribute<String> unmappedSourcePolicy() {
        return unmappedSourcePolicy;
    }

    public AnnotationAttribute<String> typeConversionPolicy() {
        return typeConversionPolicy;
    }

    public AnnotationAttribute<String> componentModel() {
        return componentModel;
    }

    public AnnotationAttribute<Boolean> suppressTimestampInGenerated() {
        return suppressTimestampInGenerated;
    }

    public AnnotationAttribute<String> mappingInheritanceStrategy() {
        return mappingInheritanceStrategy;
    }

    public AnnotationAttribute<String> injectionStrategy() {
        return injectionStrategy;
    }

    public AnnotationAttribute<Boolean> disableSubMappingMethodsGeneration() {
        return disableSubMappingMethodsGeneration;
    }

    public AnnotationAttribute<String> collectionMappingStrategy() {
        return collectionMappingStrategy;
    }

    public AnnotationAttribute<String> nullValueCheckStrategy() {
        return nullValueCheckStrategy;
    }

    public AnnotationAttribute<String> nullValuePropertyMappingStrategy() {
        return nullValuePropertyMappingStrategy;
    }

    public AnnotationAttribute<String> nullValueMappingStrategy() {
        return nullValueMappingStrategy;
    }

    public AnnotationAttribute<String> subclassExhaustiveStrategy() {
        return subclassExhaustiveStrategy;
    }

    public AnnotationAttribute<TypeDescriptor> subclassExhaustiveException() {
        return subclassExhaustiveException;
    }

    public AnnotationAttribute<String> nullValueIterableMappingStrategy() {
        return nullValueIterableMappingStrategy;
    }

    public AnnotationAttribute<String> nullValueMapMappingStrategy() {
        return nullValueMapMappingStrategy;
    }

    public AnnotationAttribute<BuilderGem> builder() {
        return builder;
    }

    public AnnotationAttribute<TypeDescriptor> mappingControl() {
        return mappingControl;
    }

    public AnnotationAttribute<TypeDescriptor> unexpectedValueMappingException() {
        return unexpectedValueMappingException;
    }
}

