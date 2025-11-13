/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.api;

import java.util.Optional;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * Backend-specific helper for converting language-neutral descriptors to compiler native handles.
 */
public interface DescriptorUnwrapper {

    /**
     * Converts the supplied {@link TypeDescriptor} into a backend-specific representation.
     *
     * @param descriptor language-neutral descriptor
     * @param nativeType requested native type
     * @param <NATIVE> native type parameter
     *
     * @return optional native handle
     */
    <NATIVE> Optional<NATIVE> type(TypeDescriptor descriptor, Class<NATIVE> nativeType);

    /**
     * Converts the supplied {@link TypeElementDescriptor} into a backend-specific representation.
     *
     * @param descriptor language-neutral descriptor
     * @param nativeType requested native type
     * @param <NATIVE> native type parameter
     *
     * @return optional native handle
     */
    <NATIVE> Optional<NATIVE> type(TypeElementDescriptor descriptor, Class<NATIVE> nativeType);

    /**
     * Converts the supplied {@link ElementDescriptor} into a backend-specific representation.
     *
     * @param descriptor language-neutral descriptor
     * @param nativeType requested native type
     * @param <NATIVE> native type parameter
     *
     * @return optional native handle
     */
    <NATIVE> Optional<NATIVE> element(ElementDescriptor descriptor, Class<NATIVE> nativeType);

    /**
     * Converts the supplied {@link AnnotationDescriptor} into a backend-specific representation.
     *
     * @param descriptor language-neutral descriptor
     * @param nativeType requested native type
     * @param <NATIVE> native type parameter
     *
     * @return optional native handle
     */
    <NATIVE> Optional<NATIVE> annotation(AnnotationDescriptor descriptor, Class<NATIVE> nativeType);

    /**
     * Converts the supplied {@link AnnotationValueDescriptor} into a backend-specific representation.
     *
     * @param descriptor language-neutral descriptor
     * @param nativeType requested native type
     * @param <NATIVE> native type parameter
     *
     * @return optional native handle
     */
    <NATIVE> Optional<NATIVE> annotationValue(AnnotationValueDescriptor descriptor, Class<NATIVE> nativeType);
}
