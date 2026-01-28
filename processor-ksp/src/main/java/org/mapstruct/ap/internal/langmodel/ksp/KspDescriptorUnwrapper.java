/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import java.util.Optional;

import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * KSP implementation of {@link DescriptorUnwrapper}.
 * Converts language-neutral descriptors to KSP native handles.
 */
final class KspDescriptorUnwrapper implements DescriptorUnwrapper {

    KspDescriptorUnwrapper() {
    }

    @Override
    public <NATIVE> Optional<NATIVE> type(TypeDescriptor descriptor, Class<NATIVE> nativeType) {
        if ( descriptor == null || nativeType == null ) {
            return Optional.empty();
        }
        Object unwrapped = descriptor.unwrap();
        if ( nativeType.isInstance( unwrapped ) ) {
            return Optional.of( nativeType.cast( unwrapped ) );
        }
        return Optional.empty();
    }

    @Override
    public <NATIVE> Optional<NATIVE> type(TypeElementDescriptor descriptor, Class<NATIVE> nativeType) {
        if ( descriptor == null || nativeType == null ) {
            return Optional.empty();
        }
        Object unwrapped = descriptor.unwrap();
        if ( nativeType.isInstance( unwrapped ) ) {
            return Optional.of( nativeType.cast( unwrapped ) );
        }
        return Optional.empty();
    }

    @Override
    public <NATIVE> Optional<NATIVE> element(ElementDescriptor descriptor, Class<NATIVE> nativeType) {
        if ( descriptor == null || nativeType == null ) {
            return Optional.empty();
        }
        Object unwrapped = descriptor.unwrap();
        if ( nativeType.isInstance( unwrapped ) ) {
            return Optional.of( nativeType.cast( unwrapped ) );
        }
        return Optional.empty();
    }

    @Override
    public <NATIVE> Optional<NATIVE> annotation(AnnotationDescriptor descriptor, Class<NATIVE> nativeType) {
        if ( descriptor == null || nativeType == null ) {
            return Optional.empty();
        }
        Object unwrapped = descriptor.unwrap();
        if ( nativeType.isInstance( unwrapped ) ) {
            return Optional.of( nativeType.cast( unwrapped ) );
        }
        return Optional.empty();
    }

    @Override
    public <NATIVE> Optional<NATIVE> annotationValue(AnnotationValueDescriptor descriptor, Class<NATIVE> nativeType) {
        if ( descriptor == null || nativeType == null ) {
            return Optional.empty();
        }
        Object unwrapped = descriptor.unwrap();
        if ( nativeType.isInstance( unwrapped ) ) {
            return Optional.of( nativeType.cast( unwrapped ) );
        }
        return Optional.empty();
    }
}
