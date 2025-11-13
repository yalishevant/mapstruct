/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

import java.util.List;

/**
 * Descriptor for method / constructor parameters.
 */
public interface ParameterDescriptor extends ElementDescriptor {

    default String name() {
        return simpleName().content();
    }

    default TypeDescriptor type() {
        return asType();
    }

    List<AnnotationDescriptor> annotations();

    boolean isVarArgs();
}
