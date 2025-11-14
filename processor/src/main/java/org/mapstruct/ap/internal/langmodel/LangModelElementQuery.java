package org.mapstruct.ap.internal.langmodel;

import java.util.Optional;

import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * Facade exposing descriptor-backed element queries and mapper metadata.
 */
public interface LangModelElementQuery {

    /**
     * @return language-neutral element utilities.
     */
    LangElements elements();

    /**
     * Resolves the {@link MapperAnnotation} attached to the supplied mapper descriptor.
     *
     * @param element mapper type descriptor
     *
     * @return mapper annotation view or {@code null} when unavailable
     */
    MapperAnnotation mapperAnnotation(TypeElementDescriptor element);

    /**
     * Resolves the mapper configuration descriptor represented by the supplied type descriptor.
     *
     * @param configType configuration type descriptor
     *
     * @return mapper configuration annotation descriptor when available
     */
    Optional<MapperConfigAnnotation> mapperConfig(TypeDescriptor configType);
}
