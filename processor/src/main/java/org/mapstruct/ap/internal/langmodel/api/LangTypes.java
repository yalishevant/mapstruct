/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.api;

import java.util.List;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * Abstraction over compiler-specific type utilities.
 */
public interface LangTypes {

    TypeDescriptor declaredType(TypeElementDescriptor element, List<TypeDescriptor> arguments);

    TypeDescriptor erasure(TypeDescriptor type);

    TypeDescriptor boxed(TypeDescriptor type);

    TypeDescriptor unboxed(TypeDescriptor type);

    boolean isAssignable(TypeDescriptor source, TypeDescriptor target);

    boolean isSameType(TypeDescriptor a, TypeDescriptor b);

    boolean isSubtype(TypeDescriptor sub, TypeDescriptor sup);

    boolean isSubtypeErased(TypeDescriptor sub, TypeDescriptor rawSuper);

    boolean contains(TypeDescriptor container, TypeDescriptor contained);

    List<TypeDescriptor> directSupertypes(TypeDescriptor type);

    TypeDescriptor asMemberOf(TypeDescriptor containingType, ElementDescriptor member);

    TypeElementDescriptor asElement(TypeDescriptor type);

    TypeDescriptor primitive(String primitiveName);

    TypeDescriptor voidType();
}
