/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Collections;
import java.util.Set;

import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.gem.CollectionMappingStrategyGem;
import org.mapstruct.ap.internal.gem.InjectionStrategyGem;
import org.mapstruct.ap.internal.gem.MappingInheritanceStrategyGem;
import org.mapstruct.ap.internal.gem.NullValueCheckStrategyGem;
import org.mapstruct.ap.internal.gem.NullValueMappingStrategyGem;
import org.mapstruct.ap.internal.gem.NullValuePropertyMappingStrategyGem;
import org.mapstruct.ap.internal.gem.ReportingPolicyGem;
import org.mapstruct.ap.internal.gem.SubclassExhaustiveStrategyGem;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.option.Options;
import org.mapstruct.ap.internal.langmodel.AnnotationAttribute;
import org.mapstruct.ap.internal.langmodel.MapperAnnotation;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

public class DefaultOptions extends DelegatingOptions {

    private final MapperAnnotation mapper;
    private final Options options;

    DefaultOptions(MapperAnnotation mapper, Options options) {
        super( null );
        this.mapper = mapper;
        this.options = options;
    }

    @Override
    public String implementationName() {
        return mapper.implementationName().defaultValue();
    }

    @Override
    public String implementationPackage() {
        return mapper.implementationPackage().defaultValue();
    }

    @Override
    public Set<TypeDescriptor> uses() {
        return Collections.emptySet();
    }

    @Override
    public Set<TypeDescriptor> imports() {
        return Collections.emptySet();
    }

    @Override
    public ReportingPolicyGem unmappedTargetPolicy() {
        ReportingPolicyGem unmappedTargetPolicy = options.getUnmappedTargetPolicy();
        if ( unmappedTargetPolicy != null ) {
            return unmappedTargetPolicy;
        }
        return ReportingPolicyGem.valueOf( mapper.unmappedTargetPolicy().defaultValue() );
    }

    @Override
    public ReportingPolicyGem unmappedSourcePolicy() {
        ReportingPolicyGem unmappedSourcePolicy = options.getUnmappedSourcePolicy();
        if ( unmappedSourcePolicy != null ) {
            return unmappedSourcePolicy;
        }
        return ReportingPolicyGem.valueOf( mapper.unmappedSourcePolicy().defaultValue() );
    }

    @Override
    public ReportingPolicyGem typeConversionPolicy() {
        return ReportingPolicyGem.valueOf( mapper.typeConversionPolicy().defaultValue() );
    }

    @Override
    public String componentModel() {
        String defaultComponentModel = options.getDefaultComponentModel();
        if ( defaultComponentModel != null ) {
            return defaultComponentModel;
        }
        return mapper.componentModel().defaultValue();
    }

    public boolean suppressTimestampInGenerated() {
        AnnotationAttribute<Boolean> attribute = mapper.suppressTimestampInGenerated();
        if ( attribute.hasValue() ) {
            return Boolean.TRUE.equals( attribute.value().orElse( null ) );
        }
        return options.isSuppressGeneratorTimestamp();
    }

    @Override
    public MappingInheritanceStrategyGem getMappingInheritanceStrategy() {
        return MappingInheritanceStrategyGem.valueOf( mapper.mappingInheritanceStrategy().defaultValue() );
    }

    @Override
    public InjectionStrategyGem getInjectionStrategy() {
        String defaultInjectionStrategy = options.getDefaultInjectionStrategy();
        if ( defaultInjectionStrategy != null ) {
            return InjectionStrategyGem.valueOf( defaultInjectionStrategy.toUpperCase() );
        }
        return InjectionStrategyGem.valueOf( mapper.injectionStrategy().defaultValue() );
    }

    @Override
    public Boolean isDisableSubMappingMethodsGeneration() {
        return mapper.disableSubMappingMethodsGeneration().defaultValue();
    }

    // BeanMapping and Mapping

    public CollectionMappingStrategyGem getCollectionMappingStrategy() {
        return CollectionMappingStrategyGem.valueOf( mapper.collectionMappingStrategy().defaultValue() );
    }

    public NullValueCheckStrategyGem getNullValueCheckStrategy() {
        return NullValueCheckStrategyGem.valueOf( mapper.nullValueCheckStrategy().defaultValue() );
    }

    public NullValuePropertyMappingStrategyGem getNullValuePropertyMappingStrategy() {
        return NullValuePropertyMappingStrategyGem.valueOf(
            mapper.nullValuePropertyMappingStrategy().defaultValue() );
    }

    public NullValueMappingStrategyGem getNullValueMappingStrategy() {
        return NullValueMappingStrategyGem.valueOf( mapper.nullValueMappingStrategy().defaultValue() );
    }

    public SubclassExhaustiveStrategyGem getSubclassExhaustiveStrategy() {
        return SubclassExhaustiveStrategyGem.valueOf( mapper.subclassExhaustiveStrategy().defaultValue() );
    }

    public TypeDescriptor getSubclassExhaustiveException() {
        return mapper.subclassExhaustiveException().defaultValue();
    }

    public NullValueMappingStrategyGem getNullValueIterableMappingStrategy() {
        NullValueMappingStrategyGem nullValueIterableMappingStrategy = options.getNullValueIterableMappingStrategy();
        if ( nullValueIterableMappingStrategy != null ) {
            return nullValueIterableMappingStrategy;
        }
        return NullValueMappingStrategyGem.valueOf( mapper.nullValueIterableMappingStrategy().defaultValue() );
    }

    public NullValueMappingStrategyGem getNullValueMapMappingStrategy() {
        NullValueMappingStrategyGem nullValueMapMappingStrategy = options.getNullValueMapMappingStrategy();
        if ( nullValueMapMappingStrategy != null ) {
            return nullValueMapMappingStrategy;
        }
        return NullValueMappingStrategyGem.valueOf( mapper.nullValueMapMappingStrategy().defaultValue() );
    }

    public BuilderGem getBuilder() {
        // TODO: I realized this is not correct, however it needs to be null in order to keep downward compatibility
        // but assuming a default @Builder will make testcases fail. Not having a default means that you need to
        // specify this mandatory on @MapperConfig and @Mapper.
        return null;
    }

    @Override
    public MappingControl getMappingControl(TypeFactory typeFactory) {
        return MappingControl.fromTypeDescriptor(
            mapper.mappingControl().defaultValue(),
            typeFactory.langElements()
        );
    }

    @Override
    public TypeDescriptor getUnexpectedValueMappingException() {
        return null;
    }

    @Override
    public boolean hasAnnotation() {
        return false;
    }

}
