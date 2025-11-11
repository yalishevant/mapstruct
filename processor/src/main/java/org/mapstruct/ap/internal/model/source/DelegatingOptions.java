/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.LinkedHashSet;
import java.util.List;
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
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;
import org.mapstruct.ap.descriptor.TypeDescriptor;

/**
 * Chain Of Responsibility Pattern.
 */
public abstract class DelegatingOptions {

    private final DelegatingOptions next;

    public DelegatingOptions(DelegatingOptions next) {
        this.next = next;
    }

    // @Mapper and @MapperConfig

    public String implementationName() {
        return next.implementationName();
    }

    public String implementationPackage() {
        return next.implementationPackage();
    }

    public Set<TypeDescriptor> uses() {
        return next.uses();
    }

    public Set<TypeDescriptor> imports() {
        return next.imports();
    }

    public ReportingPolicyGem unmappedTargetPolicy() {
        return next.unmappedTargetPolicy();
    }

    public ReportingPolicyGem unmappedSourcePolicy() {
        return next.unmappedSourcePolicy();
    }

    public ReportingPolicyGem typeConversionPolicy() {
        return next.typeConversionPolicy();
    }

    public String componentModel() {
        return next.componentModel();
    }

    public boolean suppressTimestampInGenerated() {
        return next.suppressTimestampInGenerated();
    }

    public MappingInheritanceStrategyGem getMappingInheritanceStrategy() {
        return next.getMappingInheritanceStrategy();
    }

    public InjectionStrategyGem getInjectionStrategy() {
        return next.getInjectionStrategy();
    }

    public Boolean isDisableSubMappingMethodsGeneration() {
        return next.isDisableSubMappingMethodsGeneration();
    }

    // BeanMapping and Mapping

    public CollectionMappingStrategyGem getCollectionMappingStrategy() {
        return next.getCollectionMappingStrategy();
    }

    public NullValueCheckStrategyGem getNullValueCheckStrategy() {
        return next.getNullValueCheckStrategy();
    }

    public NullValuePropertyMappingStrategyGem getNullValuePropertyMappingStrategy() {
        return next.getNullValuePropertyMappingStrategy();
    }

    public NullValueMappingStrategyGem getNullValueMappingStrategy() {
        return next.getNullValueMappingStrategy();
    }

    public SubclassExhaustiveStrategyGem getSubclassExhaustiveStrategy() {
        return next.getSubclassExhaustiveStrategy();
    }

    public TypeDescriptor getSubclassExhaustiveException() {
        return next.getSubclassExhaustiveException();
    }

    public NullValueMappingStrategyGem getNullValueIterableMappingStrategy() {
        return next.getNullValueIterableMappingStrategy();
    }

    public NullValueMappingStrategyGem getNullValueMapMappingStrategy() {
        return next.getNullValueMapMappingStrategy();
    }

    public BuilderGem getBuilder() {
        return next.getBuilder();
    }

    public MappingControl getMappingControl(TypeFactory typeFactory) {
        return next.getMappingControl( typeFactory );
    }

    public TypeDescriptor getUnexpectedValueMappingException() {
        return next.getUnexpectedValueMappingException();
    }

    DelegatingOptions next() {
        return next;
    }

    protected Set<TypeDescriptor> mergeTypeDescriptors(List<TypeDescriptor> descriptors, Set<TypeDescriptor> next) {
        Set<TypeDescriptor> result = new LinkedHashSet<>();
        if ( descriptors != null ) {
            for ( TypeDescriptor descriptor : descriptors ) {
                if ( descriptor == null ) {
                    throw new TypeHierarchyErroneousException();
                }
                result.add( descriptor );
            }
        }
        result.addAll( next );
        return result;
    }

    public abstract boolean hasAnnotation();

}
