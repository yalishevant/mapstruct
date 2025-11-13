/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import java.util.List;

import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * Language-neutral view over an executable's resolved signature (parameters, return type, thrown types).
 * <p>
 * Implementations are provided by compiler-specific back ends (e.g. JSR 269, KSP) and guarantee that any
 * generic type variables are resolved in the context of the supplied containing type.
 */
public interface ExecutableSignature {

    /**
     * @return ordered parameter type descriptors
     */
    List<TypeDescriptor> parameterTypes();

    /**
     * @return the resolved return type descriptor
     */
    TypeDescriptor returnType();

    /**
     * @return descriptors of declared thrown types
     */
    List<TypeDescriptor> thrownTypes();
}
