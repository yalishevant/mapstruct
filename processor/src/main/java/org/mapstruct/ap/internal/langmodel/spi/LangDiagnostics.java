/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.spi;

import java.util.List;

import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.spi.AstModifyingAnnotationProcessor;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;

/**
 * Diagnostic bridge exposed to SPI implementations for reporting warnings and errors using the active backend.
 */
public interface LangDiagnostics {

    void warning(String message);

    void error(String message);

    boolean isTypeComplete(TypeDescriptor descriptor, List<? extends AstModifyingAnnotationProcessor> processors);

    TypeHierarchyErroneousException typeHierarchyErroneousException(TypeDescriptor descriptor);
}
