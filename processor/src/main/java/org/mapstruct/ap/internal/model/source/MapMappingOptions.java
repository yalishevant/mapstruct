/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.mapstruct.ap.internal.gem.MapMappingGem;
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
 * Represents a map mapping as configured via {@code @MapMapping}.
 *
 * @author Gunnar Morling
 */
public class MapMappingOptions extends DelegatingOptions {

    private final SelectionParameters keySelectionParameters;
    private final SelectionParameters valueSelectionParameters;
    private final FormattingParameters keyFormattingParameters;
    private final FormattingParameters valueFormattingParameters;
    private final MapMappingGem mapMapping;
    private final AnnotationDescriptor annotation;

    public static MapMappingOptions fromGem(MapMappingGem mapMapping, MapperOptions mapperOptions,
                                            ExecutableDescriptor method, FormattingMessager messager,
                                            TypeFactory typeFactory) {

        if ( mapMapping == null || !isConsistent( mapMapping, method, messager ) ) {
            MapMappingOptions options = new MapMappingOptions(
                null,
                SelectionParameters.empty(),
                null,
                SelectionParameters.empty(),
                null,
                null,
                mapperOptions
            );
            return options;
        }

        String locale = mapMapping.locale().getValue();

        List<TypeDescriptor> keyQualifiers;
        if ( mapMapping.keyQualifiedBy().hasValue() ) {
            keyQualifiers = AnnotationValueUtils.asTypeList(
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( mapMapping.keyQualifiedBy().getAnnotationValue() ) );
        }
        else {
            keyQualifiers = Collections.<TypeDescriptor>emptyList();
        }
        TypeDescriptor keyTarget = mapMapping.keyTargetType().hasValue()
            ? AnnotationValueUtils.asType(
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( mapMapping.keyTargetType().getAnnotationValue() ) )
            : null;

        SelectionParameters keySelection = new SelectionParameters(
            keyQualifiers,
            mapMapping.keyQualifiedByName().get(),
            keyTarget
        );

        List<TypeDescriptor> valueQualifiers;
        if ( mapMapping.valueQualifiedBy().hasValue() ) {
            valueQualifiers = AnnotationValueUtils.asTypeList(
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( mapMapping.valueQualifiedBy().getAnnotationValue() ) );
        }
        else {
            valueQualifiers = Collections.<TypeDescriptor>emptyList();
        }
        TypeDescriptor valueTarget = mapMapping.valueTargetType().hasValue()
            ? AnnotationValueUtils.asType(
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( mapMapping.valueTargetType().getAnnotationValue() ) )
            : null;

        SelectionParameters valueSelection = new SelectionParameters(
            valueQualifiers,
            mapMapping.valueQualifiedByName().get(),
            valueTarget
        );

        AnnotationDescriptor annotationDescriptor =
            typeFactory.getDescriptorFactory().annotationDescriptor( mapMapping.mirror() );

        FormattingParameters keyFormatting = new FormattingParameters(
            mapMapping.keyDateFormat().get(),
            mapMapping.keyNumberFormat().get(),
            annotationDescriptor,
            typeFactory.getDescriptorFactory()
                .annotationValueDescriptor( mapMapping.keyDateFormat().getAnnotationValue() ),
            method,
            locale
        );

        FormattingParameters valueFormatting = new FormattingParameters(
            mapMapping.valueDateFormat().get(),
            mapMapping.valueNumberFormat().get(),
            annotationDescriptor,
            typeFactory.getDescriptorFactory()
                .annotationValueDescriptor( mapMapping.valueDateFormat().getAnnotationValue() ),
            method,
            locale
        );

        MapMappingOptions options = new MapMappingOptions(
            keyFormatting,
            keySelection,
            valueFormatting,
            valueSelection,
            mapMapping,
            annotationDescriptor,
            mapperOptions
        );
        return options;
    }

    private static boolean isConsistent(MapMappingGem gem, ExecutableDescriptor method,
                                        FormattingMessager messager) {
        if ( !gem.keyDateFormat().hasValue()
            && !gem.keyNumberFormat().hasValue()
            && !gem.keyQualifiedBy().hasValue()
            && !gem.keyQualifiedByName().hasValue()
            && !gem.valueDateFormat().hasValue()
            && !gem.valueNumberFormat().hasValue()
            && !gem.valueQualifiedBy().hasValue()
            && !gem.valueQualifiedByName().hasValue()
            && !gem.keyTargetType().hasValue()
            && !gem.valueTargetType().hasValue()
            && !gem.nullValueMappingStrategy().hasValue() ) {
            messager.printMessage( method, Message.MAPMAPPING_NO_ELEMENTS );
            return false;
        }
        return true;
    }

    private MapMappingOptions(FormattingParameters keyFormatting, SelectionParameters keySelectionParameters,
                              FormattingParameters valueFormatting, SelectionParameters valueSelectionParameters,
                              MapMappingGem mapMapping, AnnotationDescriptor annotation, DelegatingOptions next ) {
        super( next );
        this.keyFormattingParameters = keyFormatting;
        this.keySelectionParameters = keySelectionParameters;
        this.valueFormattingParameters = valueFormatting;
        this.valueSelectionParameters = valueSelectionParameters;
        this.mapMapping = mapMapping;
        this.annotation = annotation;
    }

    public FormattingParameters getKeyFormattingParameters() {
        return keyFormattingParameters;
    }

    public SelectionParameters getKeySelectionParameters() {
        return keySelectionParameters;
    }

    public FormattingParameters getValueFormattingParameters() {
        return valueFormattingParameters;
    }

    public AnnotationDescriptor getAnnotation() {
        return annotation;
    }

    public SelectionParameters getValueSelectionParameters() {
        return valueSelectionParameters;
    }

    @Override
    public NullValueMappingStrategyGem getNullValueMappingStrategy() {
        return Optional.ofNullable( mapMapping ).map( MapMappingGem::nullValueMappingStrategy )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( NullValueMappingStrategyGem::valueOf )
            .orElse( next().getNullValueMapMappingStrategy() );
    }

    public MappingControl getKeyMappingControl(TypeFactory typeFactory) {
        return Optional.ofNullable( mapMapping ).map( MapMappingGem::keyMappingControl )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( mc -> MappingControl.fromTypeDescriptor(
                typeFactory.getDescriptorFactory().typeDescriptor( mc ),
                typeFactory.langElements() ) )
            .orElse( next().getMappingControl( typeFactory ) );
    }

    public MappingControl getValueMappingControl(TypeFactory typeFactory) {
        return Optional.ofNullable( mapMapping ).map( MapMappingGem::valueMappingControl )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( mc -> MappingControl.fromTypeDescriptor(
                typeFactory.getDescriptorFactory().typeDescriptor( mc ),
                typeFactory.langElements() ) )
            .orElse( next().getMappingControl( typeFactory ) );
    }

    @Override
    public boolean hasAnnotation() {
        return mapMapping != null;
    }

}
