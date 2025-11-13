/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.lang.model.element.VariableElement;

import org.mapstruct.ap.internal.langmodel.descriptor.FieldDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

final class JavaxFieldElementDescriptor extends JavaxElementDescriptor implements FieldDescriptor {

    private final VariableElement element;

    JavaxFieldElementDescriptor(JavaxLangModelContext context,
                                JavaxDescriptorFactory factory,
                                VariableElement element) {
        super( context, factory, element );
        this.element = element;
    }

    @Override
    public TypeDescriptor fieldType() {
        return factory.typeDescriptor( element.asType() );
    }
}
