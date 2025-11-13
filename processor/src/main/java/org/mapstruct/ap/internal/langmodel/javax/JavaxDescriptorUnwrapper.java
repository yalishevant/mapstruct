/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.Optional;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * {@link DescriptorUnwrapper} backed by {@code javax.lang.model}.
 */
public final class JavaxDescriptorUnwrapper implements DescriptorUnwrapper {

    @Override
    public <NATIVE> Optional<NATIVE> type(TypeDescriptor descriptor, Class<NATIVE> nativeType) {
        if ( descriptor == null ) {
            return Optional.empty();
        }
        if ( descriptor instanceof JavaxTypeDescriptor ) {
            return unwrapHandle( ( (JavaxTypeDescriptor) descriptor ).mirror(), nativeType );
        }
        return Optional.empty();
    }

    @Override
    public <NATIVE> Optional<NATIVE> type(TypeElementDescriptor descriptor, Class<NATIVE> nativeType) {
        if ( descriptor == null ) {
            return Optional.empty();
        }

        if ( descriptor instanceof JavaxTypeElementDescriptor ) {
            TypeElement element = ( (JavaxTypeElementDescriptor) descriptor ).element();
            Optional<NATIVE> direct = unwrapHandle( element, nativeType );
            if ( direct.isPresent() ) {
                return direct;
            }

            if ( TypeMirror.class == nativeType ) {
                TypeMirror mirror = element.asType();
                return Optional.of( nativeType.cast( mirror ) );
            }

            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public <NATIVE> Optional<NATIVE> element(ElementDescriptor descriptor, Class<NATIVE> nativeType) {
        if ( descriptor == null ) {
            return Optional.empty();
        }
        if ( descriptor instanceof JavaxElementDescriptor ) {
            return unwrapHandle( ( (JavaxElementDescriptor) descriptor ).element(), nativeType );
        }
        return Optional.empty();
    }

    @Override
    public <NATIVE> Optional<NATIVE> annotation(AnnotationDescriptor descriptor, Class<NATIVE> nativeType) {
        if ( descriptor == null ) {
            return Optional.empty();
        }
        if ( descriptor instanceof JavaxAnnotationDescriptor ) {
            return unwrapHandle( ( (JavaxAnnotationDescriptor) descriptor ).mirror(), nativeType );
        }
        return Optional.empty();
    }

    @Override
    public <NATIVE> Optional<NATIVE> annotationValue(AnnotationValueDescriptor descriptor, Class<NATIVE> nativeType) {
        if ( descriptor == null ) {
            return Optional.empty();
        }
        if ( descriptor instanceof JavaxAnnotationValueDescriptor ) {
            return unwrapHandle( ( (JavaxAnnotationValueDescriptor) descriptor ).annotationValue(), nativeType );
        }
        return Optional.empty();
    }

    private <NATIVE> Optional<NATIVE> unwrapHandle(Object nativeHandle, Class<NATIVE> nativeType) {
        if ( nativeHandle == null ) {
            return Optional.empty();
        }
        if ( nativeType.isInstance( nativeHandle ) ) {
            return Optional.of( nativeType.cast( nativeHandle ) );
        }
        return Optional.empty();
    }
}
