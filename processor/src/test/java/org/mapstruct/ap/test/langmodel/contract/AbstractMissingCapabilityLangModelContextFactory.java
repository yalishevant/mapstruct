/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import java.util.Objects;
import java.util.Set;

import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapterFactory;
import org.mapstruct.ap.internal.langmodel.GeneratedFileAccess;
import org.mapstruct.ap.internal.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.LangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.LangModelElementQuery;
import org.mapstruct.ap.internal.langmodel.LangModelTypeSystem;
import org.mapstruct.ap.internal.langmodel.api.LangTypes;
import org.mapstruct.ap.internal.langmodel.MapperAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperConfigAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperEntryPoint;
import org.mapstruct.ap.internal.langmodel.OptionalCapability;
import org.mapstruct.ap.internal.langmodel.TypeIntrospector;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.javax.JavaxLangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.spi.LangDiagnostics;

/**
 * Test utility for stripping specific capabilities from the javax backend to validate fail-fast behaviour.
 */
abstract class AbstractMissingCapabilityLangModelContextFactory implements LangModelContextFactory {

    private final JavaxLangModelContextFactory delegate = new JavaxLangModelContextFactory();

    @Override
    public LangModelContext create(MapperEntryPoint entryPoint) {
        LangModelContext context = delegate.create( entryPoint );
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

    protected abstract Set<Class<?>> missingCapabilities();

    private static final class MissingCapabilityContext<T, E, A, V> implements LangModelContext {

        private final LangModelContext delegate;
        private final Set<Class<?>> missingCapabilities;

        private MissingCapabilityContext(LangModelContext delegate,
                                         Set<Class<?>> missingCapabilities) {
            this.delegate = Objects.requireNonNull( delegate, "delegate" );
            this.missingCapabilities = Objects.requireNonNull( missingCapabilities, "missingCapabilities" );
        }

        @Override
        public LangDescriptorFactory descriptors() {
            return delegate.descriptors();
        }

        @Override
        public LangTypes types() {
            return delegate.types();
        }

        @Override
        public TypeIntrospector typeIntrospector() {
            return delegate.typeIntrospector();
        }

        @Override
        public LangElements elements() {
            return delegate.elements();
        }

        @Override
        public MapperAnnotation mapperAnnotation(TypeElementDescriptor element) {
            return delegate.mapperAnnotation( element );
        }

        @Override
        public java.util.Optional<MapperConfigAnnotation> mapperConfig(TypeDescriptor configType) {
            return delegate.mapperConfig( configType );
        }

        @Override
        @SuppressWarnings("unchecked")
        public LangModelTypeSystem<T, E, A, V> typeSystem() {
            return (LangModelTypeSystem<T, E, A, V>) delegate.typeSystem();
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
