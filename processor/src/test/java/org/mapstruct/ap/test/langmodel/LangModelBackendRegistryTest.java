/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mapstruct.ap.MappingProcessor;
import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapter;
import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapterFactory;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.LangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.MapperEntryPoint;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.spi.AccessorNamingStrategy;
import org.mapstruct.ap.spi.MethodType;

import static org.assertj.core.api.Assertions.assertThat;

class LangModelBackendRegistryTest {

    @Test
    void selectDefaultFactoryPrefersJavaxBackendWhenAvailable() throws Exception {
        MappingProcessor processor = new MappingProcessor();
        Method selectDefault = MappingProcessor.class.getDeclaredMethod(
            "selectDefaultFactory",
            List.class
        );
        selectDefault.setAccessible( true );

        SampleFactory custom = new SampleFactory( "custom" );
        SampleFactory javax = new SampleFactory( "javax" );

        @SuppressWarnings("unchecked")
        LangModelContextFactory selected = (LangModelContextFactory) selectDefault.invoke(
            processor,
            new ArrayList<>( List.of( custom, javax ) )
        );

        assertThat( selected ).isSameAs( javax );
    }

    @Test
    void matchesBackendRecognizesBackendIdAndClassName() throws Exception {
        MappingProcessor processor = new MappingProcessor();
        Method matchesBackend = MappingProcessor.class.getDeclaredMethod(
            "matchesBackend",
            LangModelContextFactory.class,
            String.class
        );
        matchesBackend.setAccessible( true );

        SampleFactory factory = new SampleFactory( "custom-backend" );

        boolean matchesId = (Boolean) matchesBackend.invoke( null, factory, "custom-backend" );
        boolean matchesCanonical = (Boolean) matchesBackend.invoke(
            null,
            factory,
            factory.getClass().getName().toLowerCase( java.util.Locale.ROOT )
        );
        boolean mismatch = (Boolean) matchesBackend.invoke( null, factory, "missing" );

        assertThat( matchesId ).isTrue();
        assertThat( matchesCanonical ).isTrue();
        assertThat( mismatch ).isFalse();
    }

    private static final class SampleFactory implements LangModelContextFactory {

        private static final DescriptorUnwrapper NOOP_UNWRAPPER = new NoopDescriptorUnwrapper();
        private static final AccessorNamingAdapterFactory NOOP_ACCESSOR_FACTORY =
            new NoopAccessorNamingAdapterFactory();

        private final String backendId;

        private SampleFactory(String backendId) {
            this.backendId = backendId;
        }

        @Override
        public LangModelContext create(MapperEntryPoint entryPoint) {
            throw new UnsupportedOperationException( "Not required for registry test" );
        }

        @Override
        public DescriptorUnwrapper descriptorUnwrapper() {
            return NOOP_UNWRAPPER;
        }

        @Override
        public AccessorNamingAdapterFactory accessorNamingAdapterFactory() {
            return NOOP_ACCESSOR_FACTORY;
        }

        @Override
        public String backendId() {
            return backendId;
        }
    }

    private static final class NoopDescriptorUnwrapper implements DescriptorUnwrapper {

        @Override
        public <NATIVE> Optional<NATIVE> type(TypeDescriptor descriptor, Class<NATIVE> nativeType) {
            return Optional.empty();
        }

        @Override
        public <NATIVE> Optional<NATIVE> type(TypeElementDescriptor descriptor, Class<NATIVE> nativeType) {
            return Optional.empty();
        }

        @Override
        public <NATIVE> Optional<NATIVE> element(ElementDescriptor descriptor, Class<NATIVE> nativeType) {
            return Optional.empty();
        }

        @Override
        public <NATIVE> Optional<NATIVE> annotation(AnnotationDescriptor descriptor, Class<NATIVE> nativeType) {
            return Optional.empty();
        }

        @Override
        public <NATIVE> Optional<NATIVE> annotationValue(AnnotationValueDescriptor descriptor,
                                                         Class<NATIVE> nativeType) {
            return Optional.empty();
        }
    }

    private static final class NoopAccessorNamingAdapterFactory implements AccessorNamingAdapterFactory {

        @Override
        public AccessorNamingAdapter create(AccessorNamingStrategy accessorNamingStrategy,
                                            DescriptorUnwrapper descriptorUnwrapper,
                                            LangModelContext langModelContext) {
            return new AccessorNamingAdapter() {
                @Override
                public MethodType methodType(ExecutableDescriptor executable) {
                    return MethodType.OTHER;
                }

                @Override
                public String propertyName(ExecutableDescriptor executable) {
                    return "";
                }

                @Override
                public String elementName(ElementDescriptor element) {
                    return "";
                }
            };
        }
    }
}
