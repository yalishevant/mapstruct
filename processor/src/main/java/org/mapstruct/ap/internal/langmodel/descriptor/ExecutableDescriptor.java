/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

import java.util.List;

/**
 * Descriptor representing methods and constructors.
 */
public interface ExecutableDescriptor extends ElementDescriptor {

    List<ParameterDescriptor> parameters();

    TypeDescriptor returnType();

    List<TypeDescriptor> thrownTypes();

    boolean isDefault();

    boolean isVarArgs();

    AnnotationValueDescriptor defaultValue();

    List<TypeDescriptor> typeParameters();
}
