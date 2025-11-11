/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Set;

import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.gem.CollectionMappingStrategyGem;
import org.mapstruct.ap.internal.gem.InjectionStrategyGem;
import org.mapstruct.ap.internal.gem.MappingInheritanceStrategyGem;
import org.mapstruct.ap.internal.gem.NullValueCheckStrategyGem;
import org.mapstruct.ap.internal.gem.NullValueMappingStrategyGem;
import org.mapstruct.ap.internal.gem.NullValuePropertyMappingStrategyGem;
import org.mapstruct.ap.internal.gem.ReportingPolicyGem;
import org.mapstruct.ap.internal.gem.SubclassExhaustiveStrategyGem;
import org.mapstruct.ap.langmodel.AnnotationAttribute;
import org.mapstruct.ap.langmodel.MapperConfigAnnotation;
import org.mapstruct.ap.descriptor.TypeDescriptor;

public class MapperConfigOptions extends DelegatingOptions {

    private final MapperConfigAnnotation mapperConfig;

    MapperConfigOptions(MapperConfigAnnotation mapperConfig, DelegatingOptions next ) {
        super( next );
        this.mapperConfig = mapperConfig;
    }

    @Override
    public String implementationName() {
        return mapperConfig.implementationName().hasValue() ?
            mapperConfig.implementationName().value().orElse( null ) :
            next().implementationName();
    }

    @Override
    public String implementationPackage() {
        return mapperConfig.implementationPackage().hasValue() ?
            mapperConfig.implementationPackage().value().orElse( null ) :
            next().implementationPackage();
    }

    @Override
    public Set<TypeDescriptor> uses() {
        return mapperConfig.uses().hasValue() ?
            mergeTypeDescriptors( mapperConfig.uses().valueOrDefault(), next().uses() ) :
            next().uses();
    }

    @Override
    public Set<TypeDescriptor> imports() {
        return mapperConfig.imports().hasValue() ?
            mergeTypeDescriptors( mapperConfig.imports().valueOrDefault(), next().imports() ) :
            next().imports();
    }

    @Override
    public ReportingPolicyGem unmappedTargetPolicy() {
        return mapperConfig.unmappedTargetPolicy().hasValue() ?
            ReportingPolicyGem.valueOf( mapperConfig.unmappedTargetPolicy().value().orElse( null ) ) :
            next().unmappedTargetPolicy();

    }

    @Override
    public ReportingPolicyGem unmappedSourcePolicy() {
        return mapperConfig.unmappedSourcePolicy().hasValue() ?
            ReportingPolicyGem.valueOf( mapperConfig.unmappedSourcePolicy().value().orElse( null ) ) :
            next().unmappedSourcePolicy();
    }

    @Override
    public ReportingPolicyGem typeConversionPolicy() {
        return mapperConfig.typeConversionPolicy().hasValue() ?
            ReportingPolicyGem.valueOf( mapperConfig.typeConversionPolicy().value().orElse( null ) ) :
            next().typeConversionPolicy();
    }

    @Override
    public String componentModel() {
        return mapperConfig.componentModel().hasValue() ?
            mapperConfig.componentModel().value().orElse( null ) :
            next().componentModel();
    }

    @Override
    public boolean suppressTimestampInGenerated() {
        AnnotationAttribute<Boolean> attribute = mapperConfig.suppressTimestampInGenerated();
        return attribute.hasValue() ?
            Boolean.TRUE.equals( attribute.value().orElse( null ) ) :
            next().suppressTimestampInGenerated();
    }

    @Override
    public MappingInheritanceStrategyGem getMappingInheritanceStrategy() {
        return mapperConfig.mappingInheritanceStrategy().hasValue() ?
            MappingInheritanceStrategyGem.valueOf(
                mapperConfig.mappingInheritanceStrategy().value().orElse( null ) ) :
            next().getMappingInheritanceStrategy();
    }

    @Override
    public InjectionStrategyGem getInjectionStrategy() {
        return mapperConfig.injectionStrategy().hasValue() ?
            InjectionStrategyGem.valueOf( mapperConfig.injectionStrategy().value().orElse( null ) ) :
            next().getInjectionStrategy();
    }

    @Override
    public Boolean isDisableSubMappingMethodsGeneration() {
        return mapperConfig.disableSubMappingMethodsGeneration().hasValue() ?
            mapperConfig.disableSubMappingMethodsGeneration().value().orElse( null ) :
            next().isDisableSubMappingMethodsGeneration();
    }

    // @Mapping, @BeanMapping

    @Override
    public CollectionMappingStrategyGem getCollectionMappingStrategy() {
        return mapperConfig.collectionMappingStrategy().hasValue() ?
            CollectionMappingStrategyGem.valueOf(
                mapperConfig.collectionMappingStrategy().value().orElse( null ) ) :
            next().getCollectionMappingStrategy();
    }

    @Override
    public NullValueCheckStrategyGem getNullValueCheckStrategy() {
        return mapperConfig.nullValueCheckStrategy().hasValue() ?
            NullValueCheckStrategyGem.valueOf(
                mapperConfig.nullValueCheckStrategy().value().orElse( null ) ) :
            next().getNullValueCheckStrategy();
    }

    @Override
    public NullValuePropertyMappingStrategyGem getNullValuePropertyMappingStrategy() {
        return mapperConfig.nullValuePropertyMappingStrategy().hasValue() ?
            NullValuePropertyMappingStrategyGem.valueOf(
                mapperConfig.nullValuePropertyMappingStrategy().value().orElse( null ) ) :
            next().getNullValuePropertyMappingStrategy();
    }

    @Override
    public NullValueMappingStrategyGem getNullValueMappingStrategy() {
        return mapperConfig.nullValueMappingStrategy().hasValue() ?
            NullValueMappingStrategyGem.valueOf(
                mapperConfig.nullValueMappingStrategy().value().orElse( null ) ) :
            next().getNullValueMappingStrategy();
    }

    @Override
    public SubclassExhaustiveStrategyGem getSubclassExhaustiveStrategy() {
        return mapperConfig.subclassExhaustiveStrategy().hasValue() ?
            SubclassExhaustiveStrategyGem.valueOf(
                mapperConfig.subclassExhaustiveStrategy().value().orElse( null ) ) :
            next().getSubclassExhaustiveStrategy();
    }

    public TypeDescriptor getSubclassExhaustiveException() {
        return mapperConfig.subclassExhaustiveException().hasValue() ?
            mapperConfig.subclassExhaustiveException().value().orElse( null ) :
            next().getSubclassExhaustiveException();
    }

    @Override
    public NullValueMappingStrategyGem getNullValueIterableMappingStrategy() {
        if ( mapperConfig.nullValueIterableMappingStrategy().hasValue() ) {
            return NullValueMappingStrategyGem.valueOf(
                mapperConfig.nullValueIterableMappingStrategy().value().orElse( null ) );
        }
        if ( mapperConfig.nullValueMappingStrategy().hasValue() ) {
            return NullValueMappingStrategyGem.valueOf(
                mapperConfig.nullValueMappingStrategy().value().orElse( null ) );
        }
        return next().getNullValueIterableMappingStrategy();
    }

    @Override
    public NullValueMappingStrategyGem getNullValueMapMappingStrategy() {
        if ( mapperConfig.nullValueMapMappingStrategy().hasValue() ) {
            return NullValueMappingStrategyGem.valueOf(
                mapperConfig.nullValueMapMappingStrategy().value().orElse( null ) );
        }
        if ( mapperConfig.nullValueMappingStrategy().hasValue() ) {
            return NullValueMappingStrategyGem.valueOf(
                mapperConfig.nullValueMappingStrategy().value().orElse( null ) );
        }
        return next().getNullValueMapMappingStrategy();
    }

    @Override
    public BuilderGem getBuilder() {
        return mapperConfig.builder().hasValue() ?
            mapperConfig.builder().value().orElse( null ) :
            next().getBuilder();
    }

    @Override
    public MappingControl getMappingControl(TypeFactory typeFactory) {
        if ( mapperConfig.mappingControl().hasValue() ) {
            return MappingControl.fromTypeDescriptor(
                mapperConfig.mappingControl().value().orElse( null ),
                typeFactory.langElements()
            );
        }
        return next().getMappingControl( typeFactory );
    }

    @Override
    public TypeDescriptor getUnexpectedValueMappingException() {
        return mapperConfig.unexpectedValueMappingException().hasValue() ?
            mapperConfig.unexpectedValueMappingException().value().orElse( null ) :
            next().getUnexpectedValueMappingException();
    }

    @Override
    public boolean hasAnnotation() {
        return mapperConfig != null;
    }

}
