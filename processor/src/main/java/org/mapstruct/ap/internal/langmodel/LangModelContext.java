/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import java.util.Optional;
import org.mapstruct.ap.internal.langmodel.GeneratedFileAccess;
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
    MapperAnnotation mapperAnnotation(TypeElementDescriptor element);

    /**
     * Resolves the mapper configuration descriptor represented by the supplied type descriptor.
     *
     * @param configType configuration type descriptor
     *
     * @return mapper configuration annotation descriptor when available
     */
    Optional<MapperConfigAnnotation> mapperConfig(TypeDescriptor configType);

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

    /**
     * Legacy helper providing access to descriptor facilities grouped under a single facade.
     *
     * @return descriptor-oriented type system view
     */
    default LangModelTypeSystem<?, ?, ?, ?> typeSystem() {
        return new LangModelTypeSystem<Object, Object, Object, Object>() {
            @Override
            public LangDescriptorFactory descriptors() {
                return LangModelContext.this.descriptors();
            }

            @Override
            public LangTypes types() {
                return LangModelContext.this.types();
            }

            @Override
            public TypeIntrospector typeIntrospector() {
                return LangModelContext.this.typeIntrospector();
            }
        };
    }

    /**
     * Legacy helper providing element queries through a dedicated facade.
     *
     * @return element query view backed by {@link #elements()}, {@link #mapperAnnotation(TypeElementDescriptor)} and
     * {@link #mapperConfig(TypeDescriptor)}
     */
    default LangModelElementQuery elementQuery() {
        return new LangModelElementQuery() {
            @Override
            public LangElements elements() {
                return LangModelContext.this.elements();
            }

            @Override
            public MapperAnnotation mapperAnnotation(TypeElementDescriptor element) {
                return LangModelContext.this.mapperAnnotation( element );
            }

            @Override
            public Optional<MapperConfigAnnotation> mapperConfig(TypeDescriptor configType) {
                return LangModelContext.this.mapperConfig( configType );
            }
        };
    }

    @Override
    default void close() {
    }
}
