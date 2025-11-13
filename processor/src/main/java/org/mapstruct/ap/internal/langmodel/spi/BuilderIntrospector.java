/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.spi;

import org.mapstruct.ap.internal.langmodel.descriptor.BuilderDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.spi.MoreThanOneBuilderCreationMethodException;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;

/**
 * Abstraction for retrieving builder metadata in a language-neutral manner.
 */
public interface BuilderIntrospector {

    BuilderDescriptor findBuilder(TypeDescriptor type)
        throws TypeHierarchyErroneousException, MoreThanOneBuilderCreationMethodException;
}
