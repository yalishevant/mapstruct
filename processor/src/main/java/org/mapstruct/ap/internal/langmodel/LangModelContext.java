/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import java.util.Optional;

import org.mapstruct.ap.internal.langmodel.annotation.MapperAnnotationView;
import org.mapstruct.ap.internal.langmodel.annotation.MapperConfigAnnotationView;
import org.mapstruct.ap.internal.langmodel.codegen.GeneratedFileAccess;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.api.LangTypes;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.OptionalCapability;
import org.mapstruct.ap.internal.langmodel.spi.LangDiagnostics;

/**
 * Entry point for accessing language-neutral descriptors bound to a specific processing round.
 */
public interface LangModelContext extends AutoCloseable {

    /**
     * Provides access to the descriptor factory bound to this context.
     *
     * @return descriptor factory
     */
    LangDescriptorFactory descriptors();

    /**
     * @return language-neutral type utilities.
     */
    LangTypes types();

    /**
     * @return descriptor-first type introspector.
     */
    TypeIntrospector typeIntrospector();

    /**
     * @return language-neutral element utilities.
     */
    LangElements elements();

    /**
     * Resolves the mapper annotation descriptor attached to the supplied mapper descriptor.
     *
     * @param element mapper type descriptor
     *
     * @return mapper annotation view or {@code null} when unavailable
     */
    MapperAnnotationView mapperAnnotation(TypeElementDescriptor element);

    /**
     * Resolves the mapper configuration descriptor represented by the supplied type descriptor.
     *
     * @param configType configuration type descriptor
     *
     * @return mapper configuration annotation descriptor when available
     */
    Optional<MapperConfigAnnotationView> mapperConfig(TypeDescriptor configType);

    /**
     * @return mandatory diagnostics facade.
     */
    LangDiagnostics diagnostics();

    /**
     * @return mandatory generated file access facade.
     */
    GeneratedFileAccess generatedFiles();

    /**
     * Provides access to optional backend capabilities.
     *
     * @param capabilityType capability contract
     * @param <T> capability contract type
     *
     * @return optional capability wrapper
     */
    <T> OptionalCapability<T> optional(Class<T> capabilityType);

    @Override
    default void close() {
    }
}
