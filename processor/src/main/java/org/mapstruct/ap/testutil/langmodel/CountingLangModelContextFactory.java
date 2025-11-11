/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.testutil.langmodel;

import org.mapstruct.ap.langmodel.AccessorNamingAdapterFactory;
import org.mapstruct.ap.langmodel.LangModelContext;
import org.mapstruct.ap.langmodel.LangModelContextFactory;
import org.mapstruct.ap.langmodel.MapperEntryPoint;
import org.mapstruct.ap.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.langmodel.codegen.GeneratedFileSink;
import org.mapstruct.ap.langmodel.javax.JavaxLangModelContextFactory;

/**
 * Test-only {@link LangModelContextFactory} that counts the number of contexts created while delegating to the
 * standard javax implementation.
 */
public final class CountingLangModelContextFactory implements LangModelContextFactory {

    private static final String COUNTER_PROPERTY = "mapstruct.test.langmodel.counting.contexts";

    private final JavaxLangModelContextFactory delegate = new JavaxLangModelContextFactory();

    public static void reset() {
        synchronized ( System.class ) {
            System.clearProperty( COUNTER_PROPERTY );
        }
    }

    public static int createdContexts() {
        synchronized ( System.class ) {
            String value = System.getProperty( COUNTER_PROPERTY );
            return value == null ? 0 : Integer.parseInt( value );
        }
    }

    @Override
    public LangModelContext<?, ?, ?, ?> create(MapperEntryPoint entryPoint) {
        incrementCounter();
        return delegate.create( entryPoint );
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

    @Override
    public String backendId() {
        return "counting";
    }

    private static void incrementCounter() {
        synchronized ( System.class ) {
            int current = createdContexts();
            System.setProperty( COUNTER_PROPERTY, Integer.toString( current + 1 ) );
        }
    }
}
