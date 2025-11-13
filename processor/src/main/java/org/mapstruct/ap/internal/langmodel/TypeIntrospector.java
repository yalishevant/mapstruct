/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * Provides backend-specific facilities for resolving executable signatures in the context of a containing type.
 * <p>
 * Compared to the early iterations of the thin LangModel layer this contract is intentionally narrow: the only
 * supported operation is computing the resolved signature of a method or constructor once type arguments have been
 * applied. All other member/metadata traversal is handled via
 * {@link org.mapstruct.ap.internal.langmodel.api.LangElements} and
 * {@link org.mapstruct.ap.internal.langmodel.api.LangTypes}.
 */
public interface TypeIntrospector {

    /**
     * Resolve an executable in the context of a containing type.
     *
     * @param containingType type that provides the resolution scope
     * @param executable     executable descriptor to resolve
     *
     * @return resolved signature or {@code null} if it cannot be resolved
     */
    ExecutableSignature resolveExecutable(TypeDescriptor containingType, ExecutableDescriptor executable);
}
