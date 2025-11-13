/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.api;

import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * Abstraction over backend-specific operations required for enum mapping strategies.
 */
public interface EnumMappingSupport {

    String defaultNullEnumConstant(TypeDescriptor enumType);

    String enumConstant(TypeDescriptor enumType, String enumConstant);

    TypeDescriptor unexpectedValueMappingExceptionType();
}
