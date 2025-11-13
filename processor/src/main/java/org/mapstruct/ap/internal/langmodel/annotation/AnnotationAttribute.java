/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.annotation;

import java.util.Objects;
import java.util.Optional;

/**
 * Captures the value of an annotation attribute together with its explicitness and default.
 *
 * @param <T> attribute value type
 */
public final class AnnotationAttribute<T> {

    private final boolean hasValue;
    private final T value;
    private final T defaultValue;

    private AnnotationAttribute(boolean hasValue, T value, T defaultValue) {
        this.hasValue = hasValue;
        this.value = value;
        this.defaultValue = defaultValue;
    }

    public static <T> AnnotationAttribute<T> absent(T defaultValue) {
        return new AnnotationAttribute<>( false, null, defaultValue );
    }

    public static <T> AnnotationAttribute<T> present(T value, T defaultValue) {
        return new AnnotationAttribute<>( true, Objects.requireNonNull( value ), defaultValue );
    }

    public boolean hasValue() {
        return hasValue;
    }

    public Optional<T> value() {
        return Optional.ofNullable( value );
    }

    public T valueOrDefault() {
        return hasValue && value != null ? value : defaultValue;
    }

    public T defaultValue() {
        return defaultValue;
    }
}

