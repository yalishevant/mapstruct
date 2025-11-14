/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.mapstruct.ap.internal.gem.IterableMappingGem;
import org.mapstruct.ap.internal.gem.NullValueMappingStrategyGem;
import org.mapstruct.ap.internal.model.common.FormattingParameters;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.tools.gem.GemValue;

/**
 * Represents an iterable mapping as configured via {@code @IterableMapping}.
 *
 * @author Gunnar Morling
 */
public class IterableMappingOptions extends DelegatingOptions {

    private final SelectionParameters selectionParameters;
    private final FormattingParameters formattingParameters;
    private final IterableMappingGem iterableMapping;
    private final AnnotationDescriptor annotation;

    public static IterableMappingOptions fromGem(IterableMappingGem iterableMapping,
                                                 MapperOptions mapperOptions, ExecutableDescriptor method,
                                                 FormattingMessager messager, TypeFactory typeFactory) {

        if ( iterableMapping == null || !isConsistent( iterableMapping, method, messager ) ) {
            IterableMappingOptions options = new IterableMappingOptions(
                null,
                SelectionParameters.empty(),
                null,
                null,
                mapperOptions
            );
            return options;
        }

        List<TypeDescriptor> qualifiers;
        if ( iterableMapping.qualifiedBy().hasValue() ) {
            qualifiers = AnnotationValueUtils.asTypeList(
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( iterableMapping.qualifiedBy().getAnnotationValue() ) );
        }
        else {
            qualifiers = Collections.<TypeDescriptor>emptyList();
        }
        TypeDescriptor elementTarget = iterableMapping.elementTargetType().hasValue()
            ? AnnotationValueUtils.asType(
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( iterableMapping.elementTargetType().getAnnotationValue() )
            )
            : null;

        SelectionParameters selection = new SelectionParameters(
            qualifiers,
            iterableMapping.qualifiedByName().get(),
            elementTarget
        );

        AnnotationDescriptor annotationDescriptor =
            typeFactory.getDescriptorFactory().annotationDescriptor( iterableMapping.mirror() );

        FormattingParameters formatting = new FormattingParameters(
            iterableMapping.dateFormat().get(),
            iterableMapping.numberFormat().get(),
            annotationDescriptor,
            typeFactory.getDescriptorFactory()
                .annotationValueDescriptor( iterableMapping.dateFormat().getAnnotationValue() ),
            method,
            iterableMapping.locale().getValue()
        );

        IterableMappingOptions options =
            new IterableMappingOptions( formatting, selection, iterableMapping, annotationDescriptor, mapperOptions );
        return options;
    }

    private static boolean isConsistent(IterableMappingGem gem, ExecutableDescriptor method,
                                        FormattingMessager messager) {
        if ( !gem.dateFormat().hasValue()
            && !gem.numberFormat().hasValue()
            && !gem.qualifiedBy().hasValue()
            && !gem.qualifiedByName().hasValue()
            && !gem.elementTargetType().hasValue()
            && !gem.nullValueMappingStrategy().hasValue() ) {
            messager.printMessage( method, Message.ITERABLEMAPPING_NO_ELEMENTS );
            return false;
        }
        return true;
    }

    private IterableMappingOptions(FormattingParameters formattingParameters, SelectionParameters selectionParameters,
                                   IterableMappingGem iterableMapping,
                                   AnnotationDescriptor annotation,
                                   DelegatingOptions next) {
        super( next );
        this.formattingParameters = formattingParameters;
        this.selectionParameters = selectionParameters;
        this.iterableMapping = iterableMapping;
        this.annotation = annotation;
    }

    public SelectionParameters getSelectionParameters() {
        return selectionParameters;
    }

    public FormattingParameters getFormattingParameters() {
        return formattingParameters;
    }

    public AnnotationDescriptor getAnnotation() {
        return annotation;
    }

    @Override
    public NullValueMappingStrategyGem getNullValueMappingStrategy() {
        return Optional.ofNullable( iterableMapping ).map( IterableMappingGem::nullValueMappingStrategy )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( NullValueMappingStrategyGem::valueOf )
            .orElse( next().getNullValueIterableMappingStrategy() );
    }

    public MappingControl getElementMappingControl(TypeFactory typeFactory) {
        return Optional.ofNullable( iterableMapping ).map( IterableMappingGem::elementMappingControl )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( mc -> MappingControl.fromTypeDescriptor(
                typeFactory.getDescriptorFactory().typeDescriptor( mc ),
                typeFactory.langElements() ) )
            .orElse( next().getMappingControl( typeFactory ) );
    }

    @Override
    public boolean hasAnnotation() {
        return iterableMapping != null;
    }

}
