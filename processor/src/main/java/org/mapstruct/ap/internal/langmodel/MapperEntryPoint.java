/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mapstruct.ap.internal.version.VersionInformation;

/**
 * Neutral entry point describing the mapper currently being processed.
 *
 * <p>
 * Back-end implementations obtain their native compiler handles (e.g. the annotation processing environment or mapper
 * elements) by requesting them via {@link #unwrap(Class)}. The core pipeline only observes the neutral descriptor-first
 * API.
 * </p>
 */
public interface MapperEntryPoint {

    /**
     * Compiler/runtime metadata resolved for the current processing round.
     *
     * @return version information
     */
    VersionInformation versionInformation();

    /**
     * Provides a backend-specific handle associated with this mapper entry point.
     *
     * @param nativeType requested native type
     * @param <T> type of the requested handle
     *
     * @return optional containing the handle when available
     */
    <T> Optional<T> unwrap(Class<T> nativeType);

    /**
     * Creates a mapper entry point backed by the supplied native handles.
     *
     * @param versionInformation version metadata for the current round
     * @param nativeHandles backend-specific handles to expose through {@link #unwrap(Class)}
     *
     * @return mapper entry point
     */
    static MapperEntryPoint of(VersionInformation versionInformation, Object... nativeHandles) {
        Objects.requireNonNull( versionInformation, "versionInformation" );
        List<Object> handles = new ArrayList<>();
        if ( nativeHandles != null ) {
            for ( Object nativeHandle : nativeHandles ) {
                if ( nativeHandle != null ) {
                    handles.add( nativeHandle );
                }
            }
        }
        List<Object> immutableHandles = Collections.unmodifiableList( new ArrayList<>( handles ) );
        return new MapperEntryPoint() {
            @Override
            public VersionInformation versionInformation() {
                return versionInformation;
            }

            @Override
            public <T> Optional<T> unwrap(Class<T> nativeType) {
                Objects.requireNonNull( nativeType, "nativeType" );
                for ( Object candidate : immutableHandles ) {
                    if ( nativeType.isInstance( candidate ) ) {
                        return Optional.of( nativeType.cast( candidate ) );
                    }
                }
                return Optional.empty();
            }
        };
    }
}
