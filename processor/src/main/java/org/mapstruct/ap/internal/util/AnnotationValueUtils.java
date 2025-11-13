/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * Helper for interpreting {@link AnnotationValueDescriptor} instances.
 */
public final class AnnotationValueUtils {

    private AnnotationValueUtils() {
    }

    public static String asString(AnnotationValueDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        Object value = descriptor.value();
        return value == null ? null : value.toString();
    }

    public static boolean asBoolean(AnnotationValueDescriptor descriptor, boolean defaultValue) {
        if ( descriptor == null ) {
            return defaultValue;
        }
        Object value = descriptor.value();
        if ( value instanceof Boolean ) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    public static TypeDescriptor asType(AnnotationValueDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        TypeDescriptor type = descriptor.asType();
        if ( type != null ) {
            return type;
        }
        Object value = descriptor.value();
        if ( value instanceof TypeDescriptor ) {
            return (TypeDescriptor) value;
        }
        return null;
    }

    public static List<TypeDescriptor> asTypeList(AnnotationValueDescriptor descriptor) {
        if ( descriptor == null ) {
            return Collections.emptyList();
        }
        List<AnnotationValueDescriptor> descriptors = descriptor.asList();
        if ( descriptors.isEmpty() ) {
            TypeDescriptor single = descriptor.asType();
            if ( single != null ) {
                return Collections.singletonList( single );
            }
            return Collections.emptyList();
        }
        return descriptors.stream()
            .map( AnnotationValueUtils::asType )
            .filter( Objects::nonNull )
            .collect( Collectors.collectingAndThen( Collectors.toList(), Collections::unmodifiableList ) );
    }

    public static List<String> asStringList(AnnotationValueDescriptor descriptor) {
        if ( descriptor == null ) {
            return Collections.emptyList();
        }
        List<AnnotationValueDescriptor> descriptors = descriptor.asList();
        if ( descriptors.isEmpty() ) {
            String value = asString( descriptor );
            if ( value == null ) {
                return Collections.emptyList();
            }
            return Collections.singletonList( value );
        }
        return descriptors.stream()
            .map( AnnotationValueUtils::asString )
            .filter( Objects::nonNull )
            .collect( Collectors.collectingAndThen( Collectors.toList(), Collections::unmodifiableList ) );
    }

    public static <E extends Enum<E>> E asEnum(AnnotationValueDescriptor descriptor, Class<E> enumType) {
        String name = asString( descriptor );
        if ( name == null ) {
            return null;
        }
        try {
            return Enum.valueOf( enumType, name );
        }
        catch ( IllegalArgumentException ex ) {
            throw new IllegalStateException(
                "Unexpected enum constant '" + name + "' for " + enumType.getName(),
                ex
            );
        }
    }

    public static AnnotationDescriptor asAnnotation(AnnotationValueDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        return descriptor.asAnnotation();
    }
}
