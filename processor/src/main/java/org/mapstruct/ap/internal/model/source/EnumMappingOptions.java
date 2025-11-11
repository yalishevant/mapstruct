/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Map;

import org.mapstruct.ap.internal.gem.EnumMappingGem;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Strings;
import org.mapstruct.ap.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.langmodel.AnnotationGemFactory;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.spi.EnumTransformationStrategy;

import static org.mapstruct.ap.internal.util.Message.ENUMMAPPING_INCORRECT_TRANSFORMATION_STRATEGY;
import static org.mapstruct.ap.internal.util.Message.ENUMMAPPING_MISSING_CONFIGURATION;
import static org.mapstruct.ap.internal.util.Message.ENUMMAPPING_NO_ELEMENTS;

/**
 * @author Filip Hrisafov
 */
public class EnumMappingOptions extends DelegatingOptions {

    private final EnumMappingGem enumMapping;
    private final boolean inverse;
    private final boolean valid;
    private final AnnotationDescriptor annotation;

    private EnumMappingOptions(EnumMappingGem enumMapping,
                               AnnotationDescriptor annotation,
                               boolean inverse,
                               boolean valid,
                               DelegatingOptions next) {
        super( next );
        this.enumMapping = enumMapping;
        this.inverse = inverse;
        this.valid = valid;
        this.annotation = annotation;
    }

    @Override
    public boolean hasAnnotation() {
        return annotation != null;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean hasNameTransformationStrategy() {
        return hasAnnotation() && Strings.isNotEmpty( getNameTransformationStrategy() );
    }

    public String getNameTransformationStrategy() {
        return enumMapping.nameTransformationStrategy().getValue();
    }

    public String getNameTransformationConfiguration() {
        return enumMapping.configuration().getValue();
    }

    @Override
    public TypeDescriptor getUnexpectedValueMappingException() {
        if ( enumMapping != null && enumMapping.unexpectedValueMappingException().hasValue() && annotation != null ) {
            AnnotationValueDescriptor value = AnnotationDescriptorUtils.getValue(
                annotation,
                "unexpectedValueMappingException"
            );
            return AnnotationValueUtils.asType( value );
        }

        return next().getUnexpectedValueMappingException();
    }

    public AnnotationDescriptor getAnnotation() {
        return annotation;
    }

    public boolean isInverse() {
        return inverse;
    }

    public EnumMappingOptions inverse() {
        return new EnumMappingOptions( enumMapping, annotation, true, valid, next() );
    }

    public static EnumMappingOptions getInstanceOn(AnnotationDescriptor annotation,
        MapperOptions mapperOptions,
        ExecutableDescriptor method,
        Map<String, EnumTransformationStrategy> enumTransformationStrategies,
        FormattingMessager messager,
        AnnotationGemFactory annotationGems) {

        EnumMappingGem enumMapping = annotationGems.enumMapping( annotation );
        if ( enumMapping == null ) {
            return new EnumMappingOptions( null, null, false, true, mapperOptions );
        }
        else if ( !isConsistent( enumMapping, annotation, method, enumTransformationStrategies, messager ) ) {
            return new EnumMappingOptions( null, null, false, false, mapperOptions );
        }

        return new EnumMappingOptions(
            enumMapping,
            annotation,
            false,
            true,
            mapperOptions
        );
    }

    private static boolean isConsistent(EnumMappingGem gem,
        AnnotationDescriptor annotation,
        ExecutableDescriptor method,
        Map<String, EnumTransformationStrategy> enumTransformationStrategies,
        FormattingMessager messager) {

        String strategy = gem.nameTransformationStrategy().getValue();
        String configuration = gem.configuration().getValue();

        boolean isConsistent = false;

        AnnotationValueDescriptor strategyValue = annotation != null
            ? AnnotationDescriptorUtils.getValue( annotation, "nameTransformationStrategy" )
            : null;
        AnnotationValueDescriptor configurationValue = annotation != null
            ? AnnotationDescriptorUtils.getValue( annotation, "configuration" )
            : null;

        if ( Strings.isNotEmpty( strategy ) || Strings.isNotEmpty( configuration ) ) {
            if ( !enumTransformationStrategies.containsKey( strategy ) ) {
                String registeredStrategies = Strings.join( enumTransformationStrategies.keySet(), ", " );
                messager.printMessage(
                    method,
                    annotation,
                    strategyValue,
                    ENUMMAPPING_INCORRECT_TRANSFORMATION_STRATEGY,
                    strategy,
                    registeredStrategies
                );

                return false;
            }
            else if ( Strings.isEmpty( configuration ) ) {
                messager.printMessage(
                    method,
                    annotation,
                    configurationValue,
                    ENUMMAPPING_MISSING_CONFIGURATION
                );
                return false;
            }

            isConsistent = true;
        }

        isConsistent = isConsistent || gem.unexpectedValueMappingException().hasValue();

        if ( !isConsistent ) {
            messager.printMessage(
                method,
                annotation,
                ENUMMAPPING_NO_ELEMENTS
            );
        }

        return isConsistent;
    }
}
