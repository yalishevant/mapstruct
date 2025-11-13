/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

import java.util.List;
import java.util.Optional;

/**
 * Language-neutral representation of a type used by MapStruct's processor core.
 */
public interface TypeDescriptor extends Comparable<TypeDescriptor> {

    /**
     * @return stable identifier for caching purposes.
     */
    String id();

    LangTypeKind kind();

    String displayName();

    Optional<String> qualifiedName();

    Optional<String> packageName();

    Optional<TypeElementDescriptor> typeElement();

    Optional<TypeDescriptor> componentType();

    List<TypeDescriptor> typeArguments();

    boolean isPrimitive();

    boolean isVoid();

    boolean isEnum();

    boolean isInterface();

    Optional<TypeDescriptor> wildcardExtendsBound();

    Optional<TypeDescriptor> wildcardSuperBound();

    Optional<String> typeVariableName();

    List<TypeDescriptor> typeVariableBounds();

    default Optional<TypeDescriptor> typeVariableUpperBound() {
        return Optional.empty();
    }

    default Optional<TypeDescriptor> typeVariableLowerBound() {
        return Optional.empty();
    }

    TypeDescriptor erasure();

    /**
     * @return source-specific handle, primarily for debugging.
     */
    Object unwrap();
}
