/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

final class JavaxTypeElementDescriptor extends JavaxElementDescriptor implements TypeElementDescriptor {

    private static final Method SEALED_PERMITTED_SUBCLASSES_METHOD;

    static {
        Method permittedSubclassesMethod;
        try {
            permittedSubclassesMethod = TypeElement.class.getMethod( "getPermittedSubclasses" );
        }
        catch ( NoSuchMethodException e ) {
            permittedSubclassesMethod = null;
        }
        SEALED_PERMITTED_SUBCLASSES_METHOD = permittedSubclassesMethod;
    }

    private final TypeElement element;

    JavaxTypeElementDescriptor(JavaxLangModelContext context,
                               JavaxDescriptorFactory factory,
                               TypeElement element) {
        super( context, factory, element );
        this.element = element;
    }

    TypeElement element() {
        return element;
    }

    @Override
    public String qualifiedName() {
        return element.getQualifiedName().toString();
    }

    @Override
    public TypeDescriptor asType() {
        return factory.typeDescriptor( element.asType() );
    }

    @Override
    public boolean isRecord() {
        return "RECORD".equals( element.getKind().name() );
    }

    @Override
    public boolean isSealed() {
        return element.getModifiers().stream().anyMatch( modifier -> "SEALED".equals( modifier.name() ) );
    }

    @Override
    public List<TypeDescriptor> permittedSubclasses() {
        if ( SEALED_PERMITTED_SUBCLASSES_METHOD == null ) {
            return Collections.emptyList();
        }

        try {
            @SuppressWarnings("unchecked")
            List<? extends TypeMirror> mirrors =
                (List<? extends TypeMirror>) SEALED_PERMITTED_SUBCLASSES_METHOD.invoke( element );

            if ( mirrors == null || mirrors.isEmpty() ) {
                return Collections.emptyList();
            }

            List<TypeDescriptor> descriptors = new ArrayList<>( mirrors.size() );
            for ( TypeMirror mirror : mirrors ) {
                TypeDescriptor descriptor = factory.typeDescriptor( mirror );
                if ( descriptor != null ) {
                    descriptors.add( descriptor );
                }
            }
            return Collections.unmodifiableList( descriptors );
        }
        catch ( IllegalAccessException | InvocationTargetException ex ) {
            return Collections.emptyList();
        }
    }
}
