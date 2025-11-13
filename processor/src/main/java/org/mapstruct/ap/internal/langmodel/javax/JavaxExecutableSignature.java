/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.internal.langmodel.ExecutableSignature;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

final class JavaxExecutableSignature implements ExecutableSignature {

    private final List<TypeDescriptor> parameterTypes;
    private final TypeDescriptor returnType;
    private final List<TypeDescriptor> thrownTypes;

    private JavaxExecutableSignature(List<TypeDescriptor> parameterTypes,
                                     TypeDescriptor returnType,
                                     List<TypeDescriptor> thrownTypes) {
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.thrownTypes = thrownTypes;
    }

    static JavaxExecutableSignature fromExecutableType(JavaxDescriptorFactory factory, ExecutableType executableType) {
        if ( executableType == null ) {
            return null;
        }

        List<TypeDescriptor> parameterDescriptors;
        if ( executableType.getParameterTypes().isEmpty() ) {
            parameterDescriptors = Collections.emptyList();
        }
        else {
            parameterDescriptors = new ArrayList<>( executableType.getParameterTypes().size() );
            for ( TypeMirror parameter : executableType.getParameterTypes() ) {
                TypeDescriptor descriptor = factory.typeDescriptor( parameter );
                if ( descriptor != null ) {
                    parameterDescriptors.add( descriptor );
                }
            }
        }

        List<TypeDescriptor> thrownDescriptors;
        if ( executableType.getThrownTypes().isEmpty() ) {
            thrownDescriptors = Collections.emptyList();
        }
        else {
            thrownDescriptors = new ArrayList<>( executableType.getThrownTypes().size() );
            for ( TypeMirror thrownType : executableType.getThrownTypes() ) {
                TypeDescriptor descriptor = factory.typeDescriptor( thrownType );
                if ( descriptor != null ) {
                    thrownDescriptors.add( descriptor );
                }
            }
        }

        TypeDescriptor returnDescriptor = factory.typeDescriptor( executableType.getReturnType() );

        return new JavaxExecutableSignature(
            Collections.unmodifiableList( parameterDescriptors ),
            returnDescriptor,
            Collections.unmodifiableList( thrownDescriptors )
        );
    }

    @Override
    public List<TypeDescriptor> parameterTypes() {
        return parameterTypes;
    }

    @Override
    public TypeDescriptor returnType() {
        return returnType;
    }

    @Override
    public List<TypeDescriptor> thrownTypes() {
        return thrownTypes;
    }
}
