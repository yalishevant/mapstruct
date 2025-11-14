/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ParameterDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

final class JavaxExecutableDescriptor extends JavaxElementDescriptor implements ExecutableDescriptor {

    private final ExecutableElement executableElement;

    JavaxExecutableDescriptor(JavaxLangModelContext context,
                              JavaxDescriptorFactory factory,
                              ExecutableElement executableElement) {
        super( context, factory, executableElement );
        this.executableElement = executableElement;
    }

    ExecutableElement element() {
        return executableElement;
    }

    @Override
    public List<ParameterDescriptor> parameters() {
        if ( executableElement.getParameters().isEmpty() ) {
            return Collections.emptyList();
        }
        List<ParameterDescriptor> result = new ArrayList<>();
        List<? extends javax.lang.model.element.VariableElement> parameters = executableElement.getParameters();
        int lastIndex = parameters.size() - 1;
        for ( int i = 0; i < parameters.size(); i++ ) {
            javax.lang.model.element.VariableElement parameter = parameters.get( i );
            result.add( new JavaxParameterDescriptor( context, factory, parameter,
                executableElement.isVarArgs() && i == lastIndex ) );
        }
        return Collections.unmodifiableList( result );
    }

    @Override
    public TypeDescriptor returnType() {
        ExecutableType executableType = (ExecutableType) executableElement.asType();
        return factory.typeDescriptor( executableType.getReturnType() );
    }

    @Override
    public List<TypeDescriptor> thrownTypes() {
        if ( executableElement.getThrownTypes().isEmpty() ) {
            return Collections.emptyList();
        }
        List<TypeDescriptor> result = new ArrayList<>();
        for ( TypeMirror thrownType : executableElement.getThrownTypes() ) {
            result.add( factory.typeDescriptor( thrownType ) );
        }
        return Collections.unmodifiableList( result );
    }

    @Override
    public boolean isDefault() {
        return executableElement.isDefault();
    }

    @Override
    public boolean isVarArgs() {
        return executableElement.isVarArgs();
    }

    @Override
    public AnnotationValueDescriptor defaultValue() {
        return factory.annotationValueDescriptor( executableElement.getDefaultValue() );
    }

    @Override
    public List<TypeDescriptor> typeParameters() {
        if ( executableElement.getTypeParameters().isEmpty() ) {
            return Collections.emptyList();
        }
        List<TypeDescriptor> descriptors = new ArrayList<>( executableElement.getTypeParameters().size() );
        for ( TypeParameterElement typeParameter : executableElement.getTypeParameters() ) {
            descriptors.add( factory.typeDescriptor( typeParameter.asType() ) );
        }
        return Collections.unmodifiableList( descriptors );
    }

    List<AnnotationDescriptor> annotations() {
        return JavaxAnnotationFactory.annotations( context, factory, executableElement );
    }
}
