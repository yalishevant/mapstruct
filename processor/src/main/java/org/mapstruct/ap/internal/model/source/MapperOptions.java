/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Objects;
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
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.LangModelElementQuery;
import org.mapstruct.ap.internal.langmodel.MapperAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperConfigAnnotation;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

public class MapperOptions extends DelegatingOptions {

    private final MapperAnnotation mapper;
    private final TypeDescriptor mapperConfigType;
    private final AnnotationDescriptor annotation;

    public static MapperOptions fromAnnotation(MapperAnnotation mapper,
                                               TypeElementDescriptor mapperElement,
                                               Options options,
                                               LangModelContext langModelContext) {
        DelegatingOptions defaults = new DefaultOptions( mapper, options );
        TypeDescriptor mapperConfigType = null;
        DelegatingOptions next = defaults;
        boolean mapperAnnotationValid = mapper != null && mapper.isValid();
        MapperConfigAnnotation mapperConfig = mapperAnnotationValid ? mapper.mapperConfig().orElse( null ) : null;
        LangModelElementQuery elementQuery = langModelContext.elementQuery();
        if ( mapperAnnotationValid && mapperConfig == null ) {
            TypeDescriptor configType = mapper.config().valueOrDefault();
            TypeDescriptor defaultConfigType = mapper.config().defaultValue();
            boolean configDefined = configType != null && configType.typeElement().isPresent()
                && ( defaultConfigType == null
                    || !Objects.equals( configType.id(), defaultConfigType.id() ) );
            if ( configDefined ) {
                mapperConfig = elementQuery.mapperConfig( configType ).orElse( null );
            }
        }
        if ( mapperConfig == null ) {
            TypeDescriptor configType = resolveConfigTypeFromElement( mapperElement, elementQuery );
            if ( configType != null ) {
                mapperConfig = elementQuery.mapperConfig( configType ).orElse( null );
                if ( mapperConfig != null ) {
                    mapperConfigType = configType;
                }
            }
        }
        if ( mapperConfig != null ) {
            if ( mapperConfigType == null ) {
                mapperConfigType = mapperAnnotationValid ? mapper.config().value().orElse( null ) : null;
            }
            next = new MapperConfigOptions( mapperConfig, defaults );
        }
        return new MapperOptions( mapper, mapperConfigType, next, mapper.descriptor() );
    }

    private static TypeDescriptor resolveConfigTypeFromElement(TypeElementDescriptor mapperElement,
                                                               LangModelElementQuery elementQuery) {
        if ( mapperElement == null ) {
            return null;
        }
        MapperAnnotation mapper = elementQuery.mapperAnnotation( mapperElement );
        if ( mapper == null || !mapper.isValid() ) {
            return null;
        }
        AnnotationAttribute<TypeDescriptor> configAttribute = mapper.config();
        if ( configAttribute.hasValue() ) {
            return configAttribute.value().orElse( null );
        }
        return null;
    }

    private MapperOptions(MapperAnnotation mapper,
                          TypeDescriptor mapperConfigType,
                          DelegatingOptions next,
                          AnnotationDescriptor annotation) {
        super( next );
        this.mapper = mapper;
        this.mapperConfigType = mapperConfigType;
        this.annotation = annotation;
    }

    @Override
    public String implementationName() {
        return mapper.implementationName().hasValue() ?
            mapper.implementationName().value().orElse( null ) :
            next().implementationName();
    }

    @Override
    public String implementationPackage() {
        return mapper.implementationPackage().hasValue() ?
            mapper.implementationPackage().value().orElse( null ) :
            next().implementationPackage();
    }

    @Override
    public Set<TypeDescriptor> uses() {
        if ( mapper.uses().hasValue() ) {
            return mergeTypeDescriptors( mapper.uses().value().orElse( null ), next().uses() );
        }
        return next().uses();
    }

    @Override
    public Set<TypeDescriptor> imports() {
        if ( mapper.imports().hasValue() ) {
            return mergeTypeDescriptors( mapper.imports().value().orElse( null ), next().imports() );
        }
        return next().imports();
    }

    @Override
    public ReportingPolicyGem unmappedTargetPolicy() {
        return mapper.unmappedTargetPolicy().hasValue() ?
            ReportingPolicyGem.valueOf( mapper.unmappedTargetPolicy().value().orElse( null ) ) :
            next().unmappedTargetPolicy();
    }

    @Override
    public ReportingPolicyGem unmappedSourcePolicy() {
        return mapper.unmappedSourcePolicy().hasValue() ?
            ReportingPolicyGem.valueOf( mapper.unmappedSourcePolicy().value().orElse( null ) ) :
            next().unmappedSourcePolicy();
    }

    @Override
    public ReportingPolicyGem typeConversionPolicy() {
        return mapper.typeConversionPolicy().hasValue() ?
            ReportingPolicyGem.valueOf( mapper.typeConversionPolicy().value().orElse( null ) ) :
            next().typeConversionPolicy();
    }

    @Override
    public String componentModel() {
        return mapper.componentModel().hasValue() ?
            mapper.componentModel().value().orElse( null ) :
            next().componentModel();
    }

    @Override
    public boolean suppressTimestampInGenerated() {
        AnnotationAttribute<Boolean> attribute = mapper.suppressTimestampInGenerated();
        if ( attribute.hasValue() ) {
            return Boolean.TRUE.equals( attribute.value().orElse( null ) );
        }
        return next().suppressTimestampInGenerated();
    }

    @Override
    public MappingInheritanceStrategyGem getMappingInheritanceStrategy() {
        return mapper.mappingInheritanceStrategy().hasValue() ?
            MappingInheritanceStrategyGem.valueOf( mapper.mappingInheritanceStrategy().value().orElse( null ) ) :
            next().getMappingInheritanceStrategy();
    }

    @Override
    public InjectionStrategyGem getInjectionStrategy() {
        return mapper.injectionStrategy().hasValue() ?
            InjectionStrategyGem.valueOf( mapper.injectionStrategy().value().orElse( null ) ) :
            next().getInjectionStrategy();
    }

    @Override
    public Boolean isDisableSubMappingMethodsGeneration() {
        return mapper.disableSubMappingMethodsGeneration().hasValue() ?
            mapper.disableSubMappingMethodsGeneration().value().orElse( null ) :
            next().isDisableSubMappingMethodsGeneration();
    }

    // @Mapping, @BeanMapping

    @Override
    public CollectionMappingStrategyGem getCollectionMappingStrategy() {
        return mapper.collectionMappingStrategy().hasValue() ?
            CollectionMappingStrategyGem.valueOf(
                mapper.collectionMappingStrategy().value().orElse( null ) ) :
            next().getCollectionMappingStrategy();
    }

    @Override
    public NullValueCheckStrategyGem getNullValueCheckStrategy() {
        return mapper.nullValueCheckStrategy().hasValue() ?
            NullValueCheckStrategyGem.valueOf( mapper.nullValueCheckStrategy().value().orElse( null ) ) :
            next().getNullValueCheckStrategy();
    }

    @Override
    public NullValuePropertyMappingStrategyGem getNullValuePropertyMappingStrategy() {
        if ( mapper.nullValuePropertyMappingStrategy().hasValue() ) {
            return NullValuePropertyMappingStrategyGem.valueOf(
                mapper.nullValuePropertyMappingStrategy().value().orElse( null ) );
        }
        return next().getNullValuePropertyMappingStrategy();
    }

    @Override
    public NullValueMappingStrategyGem getNullValueMappingStrategy() {
        return mapper.nullValueMappingStrategy().hasValue() ?
            NullValueMappingStrategyGem.valueOf( mapper.nullValueMappingStrategy().value().orElse( null ) ) :
            next().getNullValueMappingStrategy();
    }

    @Override
    public SubclassExhaustiveStrategyGem getSubclassExhaustiveStrategy() {
        return mapper.subclassExhaustiveStrategy().hasValue() ?
            SubclassExhaustiveStrategyGem.valueOf(
                mapper.subclassExhaustiveStrategy().value().orElse( null ) ) :
            next().getSubclassExhaustiveStrategy();
    }

    @Override
    public TypeDescriptor getSubclassExhaustiveException() {
        return mapper.subclassExhaustiveException().hasValue() ?
            mapper.subclassExhaustiveException().value().orElse( null ) :
            next().getSubclassExhaustiveException();
    }

    @Override
    public NullValueMappingStrategyGem getNullValueIterableMappingStrategy() {
        if ( mapper.nullValueIterableMappingStrategy().hasValue() ) {
            return NullValueMappingStrategyGem.valueOf(
                mapper.nullValueIterableMappingStrategy().value().orElse( null ) );
        }
        if ( mapper.nullValueMappingStrategy().hasValue() ) {
            return NullValueMappingStrategyGem.valueOf(
                mapper.nullValueMappingStrategy().value().orElse( null ) );
        }
        return next().getNullValueIterableMappingStrategy();
    }

    @Override
    public NullValueMappingStrategyGem getNullValueMapMappingStrategy() {
        if ( mapper.nullValueMapMappingStrategy().hasValue() ) {
            return NullValueMappingStrategyGem.valueOf(
                mapper.nullValueMapMappingStrategy().value().orElse( null ) );
        }
        if ( mapper.nullValueMappingStrategy().hasValue() ) {
            return NullValueMappingStrategyGem.valueOf(
                mapper.nullValueMappingStrategy().value().orElse( null ) );
        }
        return next().getNullValueMapMappingStrategy();
    }

    @Override
    public BuilderGem getBuilder() {
        return mapper.builder().hasValue() ?
            mapper.builder().value().orElse( null ) :
            next().getBuilder();
    }

    @Override
    public MappingControl getMappingControl(TypeFactory typeFactory) {
        if ( mapper.mappingControl().hasValue() ) {
            return MappingControl.fromTypeDescriptor(
                mapper.mappingControl().value().orElse( null ),
                typeFactory.langElements()
            );
        }
        return next().getMappingControl( typeFactory );
    }

    @Override
    public TypeDescriptor getUnexpectedValueMappingException() {
        return mapper.unexpectedValueMappingException().hasValue() ?
            mapper.unexpectedValueMappingException().value().orElse( null ) :
            next().getUnexpectedValueMappingException();
    }

    // @Mapper specific

    public TypeDescriptor mapperConfigType() {
        return mapperConfigType;
    }

    public boolean hasMapperConfig() {
        return mapperConfigType != null;
    }

    public boolean isValid() {
        return mapper != null && mapper.isValid();
    }

    public AnnotationDescriptor annotation() {
        return annotation;
    }

    @Override
    public boolean hasAnnotation() {
        return mapper != null;
    }

}
