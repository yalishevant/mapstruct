/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.util.accessor.Accessor;

/**
 * Provides descriptor-based utilities around {@link ExecutableDescriptor}s.
 *
 * @author Gunnar Morling
 */
public final class Executables {

    private Executables() {
    }

    static boolean isPublicNotStatic(ExecutableDescriptor method) {
        return isPublic( method ) && method != null && !method.modifiers().contains( LangModifier.STATIC );
    }

    static boolean isPublic(ExecutableDescriptor method) {
        return method != null && method.modifiers().contains( LangModifier.PUBLIC );
    }

    public static boolean isFinal(Accessor accessor) {
        return accessor != null && accessor.getModifiers().contains( LangModifier.FINAL );
    }

    public static boolean isDefaultMethod(ExecutableDescriptor method) {
        return method != null && method.isDefault();
    }

    /**
     * @param executable the element to check
     * @param elements descriptor view for annotation lookup
     * @return {@code true} if the executable is annotated with {@code @BeforeMapping} or {@code @AfterMapping}
     */
    public static boolean isLifecycleCallbackMethod(ExecutableDescriptor executable, LangElements elements) {
        return isBeforeMappingMethod( executable, elements ) || isAfterMappingMethod( executable, elements );
    }

    /**
     * @param executable the element to check
     * @param elements descriptor view for annotation lookup
     * @return {@code true} if the executable is annotated with {@code @AfterMapping}
     */
    public static boolean isAfterMappingMethod(ExecutableDescriptor executable, LangElements elements) {
        return hasAnnotation( executable, elements, "org.mapstruct.AfterMapping" );
    }

    /**
     * @param executable the element to check
     * @param elements descriptor view for annotation lookup
     * @return {@code true} if the executable is annotated with {@code @BeforeMapping}
     */
    public static boolean isBeforeMappingMethod(ExecutableDescriptor executable, LangElements elements) {
        return hasAnnotation( executable, elements, "org.mapstruct.BeforeMapping" );
    }

    private static boolean hasAnnotation(ExecutableDescriptor executable,
                                         LangElements elements,
                                         String annotationFqn) {
        if ( executable == null || elements == null ) {
            return false;
        }
        return AnnotationDescriptorUtils.findAnnotation( elements, executable, annotationFqn ).isPresent();
    }
}
