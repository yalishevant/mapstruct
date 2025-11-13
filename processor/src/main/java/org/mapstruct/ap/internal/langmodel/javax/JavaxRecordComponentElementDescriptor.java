/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.lang.model.element.Element;

import org.mapstruct.ap.internal.langmodel.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

final class JavaxRecordComponentElementDescriptor extends JavaxElementDescriptor implements RecordComponentDescriptor {

    private final Element recordComponent;

    static JavaxRecordComponentElementDescriptor create(JavaxLangModelContext context,
                                                        JavaxDescriptorFactory factory,
                                                        Element element) {
        return new JavaxRecordComponentElementDescriptor(
            context,
            factory,
            element
        );
    }

    JavaxRecordComponentElementDescriptor(JavaxLangModelContext context,
                                          JavaxDescriptorFactory factory,
                                          Element recordComponent) {
        super( context, factory, recordComponent );
        this.recordComponent = recordComponent;
    }

    @Override
    public TypeDescriptor componentType() {
        return factory.typeDescriptor( recordComponent.asType() );
    }
}
