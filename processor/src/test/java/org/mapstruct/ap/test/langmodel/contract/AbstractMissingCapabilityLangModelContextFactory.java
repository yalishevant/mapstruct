/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import java.util.Objects;
import java.util.Set;

import org.mapstruct.ap.langmodel.AccessorNamingAdapterFactory;
import org.mapstruct.ap.langmodel.GeneratedFileAccess;
import org.mapstruct.ap.langmodel.LangModelContext;
import org.mapstruct.ap.langmodel.LangModelContextFactory;
import org.mapstruct.ap.langmodel.LangModelElementQuery;
import org.mapstruct.ap.langmodel.LangModelTypeSystem;
import org.mapstruct.ap.langmodel.MapperEntryPoint;
import org.mapstruct.ap.langmodel.OptionalCapability;
import org.mapstruct.ap.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.langmodel.codegen.GeneratedFileSink;
import org.mapstruct.ap.langmodel.javax.JavaxLangModelContextFactory;
import org.mapstruct.ap.spi.lang.LangDiagnostics;

/**
 * Test utility for stripping specific capabilities from the javax backend to validate fail-fast behaviour.
 */
abstract class AbstractMissingCapabilityLangModelContextFactory implements LangModelContextFactory {

    private final JavaxLangModelContextFactory delegate = new JavaxLangModelContextFactory();

    @Override
    public LangModelContext<?, ?, ?, ?> create(MapperEntryPoint entryPoint) {
        LangModelContext<?, ?, ?, ?> context = delegate.create( entryPoint );
        return new MissingCapabilityContext<>( context, missingCapabilities() );
    }

    @Override
    public DescriptorUnwrapper descriptorUnwrapper() {
        return delegate.descriptorUnwrapper();
    }

    @Override
    public AccessorNamingAdapterFactory accessorNamingAdapterFactory() {
        return delegate.accessorNamingAdapterFactory();
    }

    @Override
    public GeneratedFileSink generatedFileSink(MapperEntryPoint entryPoint) {
        return delegate.generatedFileSink( entryPoint );
    }

    protected abstract Set<Class<?>> missingCapabilities();

    private static final class MissingCapabilityContext<T, E, A, V> implements LangModelContext<T, E, A, V> {

        private final LangModelContext<T, E, A, V> delegate;
        private final Set<Class<?>> missingCapabilities;

        private MissingCapabilityContext(LangModelContext<T, E, A, V> delegate,
                                         Set<Class<?>> missingCapabilities) {
            this.delegate = Objects.requireNonNull( delegate, "delegate" );
            this.missingCapabilities = Objects.requireNonNull( missingCapabilities, "missingCapabilities" );
        }

        @Override
        public LangModelTypeSystem<T, E, A, V> typeSystem() {
            return delegate.typeSystem();
        }

        @Override
        public LangModelElementQuery elementQuery() {
            return delegate.elementQuery();
        }

        @Override
        public LangDiagnostics diagnostics() {
            return delegate.diagnostics();
        }

        @Override
        public GeneratedFileAccess generatedFiles() {
            return delegate.generatedFiles();
        }

        @Override
        public <C> OptionalCapability<C> optional(Class<C> capabilityType) {
            if ( missingCapabilities.contains( capabilityType ) ) {
                return OptionalCapability.empty();
            }
            return delegate.optional( capabilityType );
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
