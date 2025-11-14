/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen.template;

import java.util.HashMap;
import java.util.Map;

/**
 * Default map-backed implementation of {@link Writable.Context}.
 */
public final class DefaultTemplateRenderingContext implements Writable.Context {

    private final Map<Class<?>, Object> values;

    public DefaultTemplateRenderingContext(Map<Class<?>, Object> values) {
        this.values = new HashMap<>( values );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        return (T) values.get( type );
    }
}
