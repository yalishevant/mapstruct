/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;

import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.ExecutableSignature;
import org.mapstruct.ap.internal.langmodel.TypeIntrospector;

/**
 * {@link TypeIntrospector} backed by {@code javax.lang.model}.
 */
final class JavaxTypeIntrospector implements TypeIntrospector {

    private final JavaxLangModelContext context;

    JavaxTypeIntrospector(JavaxLangModelContext context) {
        this.context = context;
    }

    @Override
    public ExecutableSignature resolveExecutable(TypeDescriptor containingType, ExecutableDescriptor executable) {
        if ( containingType == null || executable == null ) {
            return null;
        }

        if ( !( containingType instanceof JavaxTypeDescriptor )
            || !( executable instanceof JavaxExecutableDescriptor ) ) {
            return null;
        }

        DeclaredType containingHandle = (DeclaredType) ( (JavaxTypeDescriptor) containingType ).mirror();
        ExecutableElement executableHandle = ( (JavaxExecutableDescriptor) executable ).element();

        ExecutableType resolved = (ExecutableType) context.delegateTypeUtils()
            .asMemberOf( containingHandle, executableHandle );
        return JavaxExecutableSignature.fromExecutableType( context.descriptorFactory(), resolved );
    }
}
