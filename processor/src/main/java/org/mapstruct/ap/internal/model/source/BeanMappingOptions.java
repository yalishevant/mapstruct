/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mapstruct.ap.internal.gem.BeanMappingGem;
import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.gem.NullValueCheckStrategyGem;
import org.mapstruct.ap.internal.gem.NullValueMappingStrategyGem;
import org.mapstruct.ap.internal.gem.NullValuePropertyMappingStrategyGem;
import org.mapstruct.ap.internal.gem.ReportingPolicyGem;
import org.mapstruct.ap.internal.gem.SubclassExhaustiveStrategyGem;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.tools.gem.GemValue;

/**
 * Represents an bean mapping as configured via {@code @BeanMapping}.
 *
 * @author Sjaak Derksen
 */
public class BeanMappingOptions extends DelegatingOptions {

    private final SelectionParameters selectionParameters;
    private final List<String> ignoreUnmappedSourceProperties;
    private final BeanMappingGem beanMapping;
    private final AnnotationDescriptor annotation;
    private final TypeDescriptor subclassExhaustiveException;

    /**
     * creates a mapping for inheritance. Will set
     *
     * @param beanMapping the bean mapping options that should be used
     * @param isInverse whether the inheritance is inverse
     *
     * @return new mapping
     */
    public static BeanMappingOptions forInheritance(BeanMappingOptions beanMapping, boolean isInverse) {
        BeanMappingOptions options =  new BeanMappingOptions(
            SelectionParameters.forInheritance( beanMapping.selectionParameters ),
            isInverse ? Collections.emptyList() : beanMapping.ignoreUnmappedSourceProperties,
            beanMapping.beanMapping,
            beanMapping.annotation,
            beanMapping.subclassExhaustiveException,
            beanMapping
        );
        return options;
    }

    public static BeanMappingOptions forForgedMethods(BeanMappingOptions beanMapping) {
        BeanMappingOptions options = new BeanMappingOptions(
            beanMapping.selectionParameters != null ?
                SelectionParameters.withoutResultType( beanMapping.selectionParameters ) : SelectionParameters.empty(),
            Collections.emptyList(),
            beanMapping.beanMapping,
            beanMapping.annotation,
            beanMapping.subclassExhaustiveException,
            beanMapping
        );
        return options;
    }

    public static BeanMappingOptions forSubclassForgedMethods(BeanMappingOptions beanMapping) {

        return new BeanMappingOptions(
            beanMapping.selectionParameters != null ?
                SelectionParameters.withoutResultType( beanMapping.selectionParameters ) : null,
            beanMapping.ignoreUnmappedSourceProperties,
            beanMapping.beanMapping,
            beanMapping.annotation,
            beanMapping.subclassExhaustiveException,
            beanMapping
        );
    }

    public static BeanMappingOptions empty(DelegatingOptions delegatingOptions) {
        return new BeanMappingOptions(
            SelectionParameters.empty(),
            Collections.emptyList(),
            null,
            null,
            null,
            delegatingOptions
        );
    }

    public static BeanMappingOptions getInstanceOn(BeanMappingGem beanMapping, MapperOptions mapperOptions,
                                                   ExecutableDescriptor method, FormattingMessager messager,
                                                   TypeFactory typeFactory
    ) {
        if ( beanMapping == null || !isConsistent( beanMapping, method, messager ) ) {
            return empty( mapperOptions );
        }

        Objects.requireNonNull( method );
        Objects.requireNonNull( messager );
        Objects.requireNonNull( method );
        Objects.requireNonNull( typeFactory );

        List<TypeDescriptor> qualifiers;
        if ( beanMapping.qualifiedBy().hasValue() ) {
            qualifiers = AnnotationValueUtils.asTypeList(
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( beanMapping.qualifiedBy().getAnnotationValue() ) );
        }
        else {
            qualifiers = Collections.<TypeDescriptor>emptyList();
        }
        TypeDescriptor resultType = beanMapping.resultType().hasValue()
            ? AnnotationValueUtils.asType(
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( beanMapping.resultType().getAnnotationValue() )
            )
            : null;

        SelectionParameters selectionParameters = new SelectionParameters(
            qualifiers,
            beanMapping.qualifiedByName().get(),
            resultType
        );

        //TODO Do we want to add the reporting policy to the BeanMapping as well? To give more granular support?
        AnnotationDescriptor annotationDescriptor =
            typeFactory.getDescriptorFactory().annotationDescriptor( beanMapping.mirror() );

        TypeDescriptor subclassException = beanMapping.subclassExhaustiveException().hasValue()
            ? typeFactory.getDescriptorFactory()
                .typeDescriptor( beanMapping.subclassExhaustiveException().getValue() )
            : null;

        BeanMappingOptions options = new BeanMappingOptions(
            selectionParameters,
            beanMapping.ignoreUnmappedSourceProperties().get(),
            beanMapping,
            annotationDescriptor,
            subclassException,
            mapperOptions
        );
        return options;
    }

    private static boolean isConsistent(BeanMappingGem gem, ExecutableDescriptor method,
                                        FormattingMessager messager) {
        if ( !gem.resultType().hasValue()
            && !gem.mappingControl().hasValue()
            && !gem.qualifiedBy().hasValue()
            && !gem.qualifiedByName().hasValue()
            && !gem.ignoreUnmappedSourceProperties().hasValue()
            && !gem.nullValueCheckStrategy().hasValue()
            && !gem.nullValuePropertyMappingStrategy().hasValue()
            && !gem.nullValueMappingStrategy().hasValue()
            && !gem.subclassExhaustiveStrategy().hasValue()
            && !gem.unmappedTargetPolicy().hasValue()
            && !gem.unmappedSourcePolicy().hasValue()
            && !gem.ignoreByDefault().hasValue()
            && !gem.builder().hasValue() ) {

            messager.printMessage( method, Message.BEANMAPPING_NO_ELEMENTS );
            return false;
        }
        return true;
    }

    private BeanMappingOptions(SelectionParameters selectionParameters,
                               List<String> ignoreUnmappedSourceProperties,
                               BeanMappingGem beanMapping,
                               AnnotationDescriptor annotation,
                               TypeDescriptor subclassExhaustiveException,
                               DelegatingOptions next) {
        super( next );
        this.selectionParameters = selectionParameters;
        this.ignoreUnmappedSourceProperties = ignoreUnmappedSourceProperties;
        this.beanMapping = beanMapping;
        this.annotation = annotation;
        this.subclassExhaustiveException = subclassExhaustiveException;
    }

    // @Mapping, @BeanMapping

    @Override
    public NullValueCheckStrategyGem getNullValueCheckStrategy() {
        return Optional.ofNullable( beanMapping ).map( BeanMappingGem::nullValueCheckStrategy )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( NullValueCheckStrategyGem::valueOf )
            .orElse( next().getNullValueCheckStrategy() );
    }

    @Override
    public NullValuePropertyMappingStrategyGem getNullValuePropertyMappingStrategy() {
        return Optional.ofNullable( beanMapping ).map( BeanMappingGem::nullValuePropertyMappingStrategy )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( NullValuePropertyMappingStrategyGem::valueOf )
            .orElse( next().getNullValuePropertyMappingStrategy() );
    }

    @Override
    public NullValueMappingStrategyGem getNullValueMappingStrategy() {
        return Optional.ofNullable( beanMapping ).map( BeanMappingGem::nullValueMappingStrategy )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( NullValueMappingStrategyGem::valueOf )
            .orElse( next().getNullValueMappingStrategy() );
    }

    @Override
    public SubclassExhaustiveStrategyGem getSubclassExhaustiveStrategy() {
        return Optional.ofNullable( beanMapping ).map( BeanMappingGem::subclassExhaustiveStrategy )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( SubclassExhaustiveStrategyGem::valueOf )
            .orElse( next().getSubclassExhaustiveStrategy() );
    }

    @Override
    public TypeDescriptor getSubclassExhaustiveException() {
        if ( subclassExhaustiveException != null ) {
            return subclassExhaustiveException;
        }
        return next().getSubclassExhaustiveException();
    }

    @Override
    public ReportingPolicyGem unmappedTargetPolicy() {
        return Optional.ofNullable( beanMapping ).map( BeanMappingGem::unmappedTargetPolicy )
                .filter( GemValue::hasValue )
                .map( GemValue::getValue )
                .map( ReportingPolicyGem::valueOf )
                .orElse( next().unmappedTargetPolicy() );
    }

    @Override
    public ReportingPolicyGem unmappedSourcePolicy() {
        return Optional.ofNullable( beanMapping ).map( BeanMappingGem::unmappedSourcePolicy )
                .filter( GemValue::hasValue )
                .map( GemValue::getValue )
                .map( ReportingPolicyGem::valueOf )
                .orElse( next().unmappedSourcePolicy() );
    }

    @Override
    public BuilderGem getBuilder() {
        return Optional.ofNullable( beanMapping ).map( BeanMappingGem::builder )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .orElse( next().getBuilder() );
    }

    @Override
    public MappingControl getMappingControl(TypeFactory typeFactory) {
        return Optional.ofNullable( beanMapping ).map( BeanMappingGem::mappingControl )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( mc -> MappingControl.fromTypeDescriptor(
                typeFactory.getDescriptorFactory().typeDescriptor( mc ),
                typeFactory.langElements() ) )
            .orElse( next().getMappingControl( typeFactory ) );
    }

    // @BeanMapping specific

    public SelectionParameters getSelectionParameters() {
        return selectionParameters;
    }

    public boolean isIgnoredByDefault() {
        return Optional.ofNullable( beanMapping ).map( BeanMappingGem::ignoreByDefault )
            .map( GemValue::get )
            .orElse( false );
    }

    public List<String> getIgnoreUnmappedSourceProperties() {
        return ignoreUnmappedSourceProperties;
    }

    public AnnotationDescriptor getAnnotation() {
        return annotation;
    }

    @Override
    public boolean hasAnnotation() {
        return beanMapping != null;
    }
}
