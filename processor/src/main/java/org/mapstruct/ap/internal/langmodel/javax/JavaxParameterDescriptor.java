/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.List;

import javax.lang.model.element.VariableElement;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ParameterDescriptor;

final class JavaxParameterDescriptor extends JavaxElementDescriptor implements ParameterDescriptor {

    private final boolean varArgs;

    JavaxParameterDescriptor(JavaxLangModelContext context,
                             JavaxDescriptorFactory factory,
                             VariableElement element,
                             boolean varArgs) {
        super( context, factory, element );
        this.varArgs = varArgs;
    }

    @Override
    public List<AnnotationDescriptor> annotations() {
        return JavaxAnnotationFactory.annotations(
            context,
            factory,
            (VariableElement) element
        );
    }

    @Override
    public boolean isVarArgs() {
        return varArgs;
    }
}
