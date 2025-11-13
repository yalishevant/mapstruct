/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * Provides factory methods for converting backend-specific compiler handles into language-neutral descriptors.
 *
 * <p>The core pipeline always works with descriptor abstractions, therefore the concrete type of the supplied native
 * handles is only relevant to the backend implementation. The contract purposefully avoids generics to keep the API
 * small and MapStruct-oriented.</p>
 */
public interface LangDescriptorFactory {

    /**
     * Wrap the given backend type handle into a {@link TypeDescriptor}.
     *
     * @param nativeType the backend-specific type representation
     * @return descriptor abstraction
     */
    TypeDescriptor typeDescriptor(Object nativeType);

    /**
     * Wrap the given backend element handle into a {@link TypeElementDescriptor}.
     *
     * @param nativeTypeElement the backend-specific type element
     * @return descriptor abstraction
     */
    TypeElementDescriptor typeElementDescriptor(Object nativeTypeElement);

    /**
     * Wrap the given backend element handle into an {@link ElementDescriptor}.
     *
     * @param nativeElement the backend-specific element
     * @return descriptor abstraction
     */
    ElementDescriptor elementDescriptor(Object nativeElement);

    /**
     * Wrap the given backend annotation handle into an {@link AnnotationDescriptor}.
     *
     * @param nativeAnnotation the backend-specific annotation mirror/instance
     * @return descriptor abstraction
     */
    AnnotationDescriptor annotationDescriptor(Object nativeAnnotation);

    /**
     * Wrap the given backend annotation value handle into an {@link AnnotationValueDescriptor}.
     *
     * @param nativeAnnotationValue the backend-specific annotation value representation
     * @return descriptor abstraction
     */
    AnnotationValueDescriptor annotationValueDescriptor(Object nativeAnnotationValue);
}
