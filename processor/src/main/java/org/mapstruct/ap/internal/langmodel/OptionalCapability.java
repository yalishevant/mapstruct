/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Lightweight optional wrapper used for exposing backend capabilities that may be absent.
 *
 * @param <T> capability type
 */
public final class OptionalCapability<T> {

    private static final OptionalCapability<?> EMPTY = new OptionalCapability<>( null, false );

    private final T value;
    private final boolean present;

    private OptionalCapability(T value, boolean present) {
        this.value = value;
        this.present = present;
    }

    public static <T> OptionalCapability<T> of(T value) {
        return new OptionalCapability<>( Objects.requireNonNull( value, "value" ), true );
    }

    @SuppressWarnings("unchecked")
    public static <T> OptionalCapability<T> empty() {
        return (OptionalCapability<T>) EMPTY;
    }

    public void ifPresent(Consumer<? super T> action) {
        Objects.requireNonNull( action, "action" );
        if ( present ) {
            action.accept( value );
        }
    }

    public T orElse(T other) {
        return present ? value : other;
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        Objects.requireNonNull( exceptionSupplier, "exceptionSupplier" );
        if ( present ) {
            return value;
        }
        throw exceptionSupplier.get();
    }
}
