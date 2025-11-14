/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Objects;
import java.util.Set;

import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;

import static org.mapstruct.ap.internal.gem.MappingConstantsGem.ANY_REMAINING;
import static org.mapstruct.ap.internal.gem.MappingConstantsGem.ANY_UNMAPPED;
import static org.mapstruct.ap.internal.gem.MappingConstantsGem.THROW_EXCEPTION;

/**
 * Represents the mapping between one value constant and another.
 *
 * @author Sjaak Derksen
 */
public class ValueMappingOptions {

    private final String source;
    private final String target;
    private final AnnotationDescriptor mirror;
    private final AnnotationValueDescriptor sourceAnnotationValue;
    private final AnnotationValueDescriptor targetAnnotationValue;

    public static void collect(Iterable<AnnotationDescriptor> annotations,
                               ExecutableDescriptor method,
                               FormattingMessager messager,
                               Set<ValueMappingOptions> mappings) {
        boolean anyFound = false;
        for ( AnnotationDescriptor annotation : annotations ) {
            ValueMappingOptions mapping = fromAnnotation( annotation );
            if ( mapping != null ) {

                if ( !mappings.contains( mapping ) ) {
                    mappings.add( mapping );
                }
                else {
                    messager.printMessage(
                        method,
                        mapping.mirror,
                        mapping.targetAnnotationValue,
                        Message.VALUEMAPPING_DUPLICATE_SOURCE,
                        mapping.source
                    );
                }
                if ( ANY_REMAINING.equals( mapping.source )
                    || ANY_UNMAPPED.equals( mapping.source ) ) {
                    if ( anyFound ) {
                        messager.printMessage(
                            method,
                            mapping.mirror,
                            mapping.targetAnnotationValue,
                            Message.VALUEMAPPING_ANY_AREADY_DEFINED,
                            mapping.source
                        );
                    }
                    anyFound = true;
                }
            }
        }
    }

    public static ValueMappingOptions fromAnnotation(AnnotationDescriptor annotation) {
        AnnotationValueDescriptor sourceValue = AnnotationDescriptorUtils.getValue( annotation, "source" );
        AnnotationValueDescriptor targetValue = AnnotationDescriptorUtils.getValue( annotation, "target" );
        String source = AnnotationValueUtils.asString( sourceValue );
        String target = AnnotationValueUtils.asString( targetValue );
        if ( source == null || target == null ) {
            return null;
        }
        return new ValueMappingOptions(
            source,
            target,
            annotation,
            sourceValue,
            targetValue
        );
    }

    private ValueMappingOptions(String source,
                                String target,
                                AnnotationDescriptor mirror,
                                AnnotationValueDescriptor sourceAnnotationValue,
                                AnnotationValueDescriptor targetAnnotationValue ) {
        this.source = source;
        this.target = target;
        this.mirror = mirror;
        this.sourceAnnotationValue = sourceAnnotationValue;
        this.targetAnnotationValue = targetAnnotationValue;
    }

    /**
     * @return the name of the constant in the source.
     */
    public String getSource() {
        return source;
    }

    /**
     * @return the name of the constant in the target.
     */
    public String getTarget() {
        return target;
    }

    public AnnotationDescriptor getAnnotation() {
        return mirror;
    }

    public AnnotationValueDescriptor getSourceAnnotationValue() {
        return sourceAnnotationValue;
    }

    public AnnotationValueDescriptor getTargetAnnotationValue() {
        return targetAnnotationValue;
    }

    public ValueMappingOptions inverse() {
        ValueMappingOptions result;
        if ( !(ANY_REMAINING.equals( source ) || ANY_UNMAPPED.equals( source ) || THROW_EXCEPTION.equals( target ) ) ) {
            result = new ValueMappingOptions(
                target,
                source,
                mirror,
                targetAnnotationValue,
                sourceAnnotationValue
            );
        }
        else {
            result = null;
        }
        return result;
    }

    public boolean isAnyMapping() {
        return ANY_REMAINING.equals( source ) || ANY_UNMAPPED.equals( source );
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.source != null ? this.source.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        final ValueMappingOptions other = (ValueMappingOptions) obj;
        return Objects.equals( this.source, other.source );
    }
}
