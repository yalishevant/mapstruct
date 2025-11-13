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

import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.api.LangTypes;
import org.mapstruct.ap.spi.MapStructProcessingEnvironment;

/**
 * {@link MapStructProcessingEnvironment} backed by descriptor abstractions provided by {@link JavaxLangModelContext}.
 */
final class JavaxMapStructProcessingEnvironment implements MapStructProcessingEnvironment {

    private final LangElements elements;
    private final LangTypes types;
    private final DescriptorUnwrapper descriptorUnwrapper;
    private final Map<String, String> options;

    JavaxMapStructProcessingEnvironment(LangElements elements,
                                        LangTypes types,
                                        DescriptorUnwrapper descriptorUnwrapper,
                                        Map<String, String> options) {
        this.elements = Objects.requireNonNull( elements, "elements" );
        this.types = Objects.requireNonNull( types, "types" );
        this.descriptorUnwrapper = Objects.requireNonNull( descriptorUnwrapper, "descriptorUnwrapper" );
        Map<String, String> resolved = new LinkedHashMap<>();
        if ( options != null ) {
            resolved.putAll( options );
        }
        this.options = Collections.unmodifiableMap( resolved );
    }

    @Override
    public LangElements elements() {
        return elements;
    }

    @Override
    public LangTypes types() {
        return types;
    }

    @Override
    public DescriptorUnwrapper descriptorUnwrapper() {
        return descriptorUnwrapper;
    }

    @Override
    public Map<String, String> options() {
        return options;
    }
}
