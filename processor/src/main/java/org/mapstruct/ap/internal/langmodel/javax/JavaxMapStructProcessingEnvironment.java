/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.mapstruct.ap.spi.MapStructProcessingEnvironment;

/**
 * {@link MapStructProcessingEnvironment} backed by descriptor abstractions provided by {@link JavaxLangModelContext}.
 */
final class JavaxMapStructProcessingEnvironment implements MapStructProcessingEnvironment {

    private final Elements elements;
    private final Types types;
    private final Map<String, String> options;

    JavaxMapStructProcessingEnvironment(Elements elements,
                                        Types types,
                                        Map<String, String> options) {
        this.elements = Objects.requireNonNull( elements, "elements" );
        this.types = Objects.requireNonNull( types, "types" );
        Map<String, String> resolved = new LinkedHashMap<>();
        if ( options != null ) {
            resolved.putAll( options );
        }
        this.options = Collections.unmodifiableMap( resolved );
    }

    @Override
    public Elements getElementUtils() {
        return elements;
    }

    @Override
    public Types getTypeUtils() {
        return types;
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }
}
