package org.mapstruct.ap.internal.langmodel;


import org.mapstruct.ap.internal.langmodel.api.LangTypes;

/**
 * Facade exposing descriptor factories and type utilities.
 */
public interface LangModelTypeSystem<T, E, A, V> {

    LangDescriptorFactory descriptors();

    LangTypes types();

    TypeIntrospector typeIntrospector();
}
