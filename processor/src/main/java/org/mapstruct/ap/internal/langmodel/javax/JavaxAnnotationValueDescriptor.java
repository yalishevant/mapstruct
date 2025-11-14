/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

final class JavaxAnnotationValueDescriptor implements AnnotationValueDescriptor {

    private final JavaxDescriptorFactory factory;
    private final AnnotationValue value;

    JavaxAnnotationValueDescriptor(JavaxDescriptorFactory factory,
                                   AnnotationValue value) {
        this.factory = factory;
        this.value = value;
    }

    @Override
    public Object value() {
        return value.getValue();
    }

    @Override
    public TypeDescriptor asType() {
        TypeMirror mirror = resolveTypeMirror();
        if ( mirror != null ) {
            return factory.typeDescriptor( mirror );
        }
        return null;
    }

    @Override
    public List<AnnotationValueDescriptor> asList() {
        Object raw = value.getValue();
        if ( raw instanceof List ) {
            List<?> list = (List<?>) raw;
            return list.stream()
                .filter( AnnotationValue.class::isInstance )
                .map( AnnotationValue.class::cast )
                .map( factory::annotationValueDescriptor )
                .collect( Collectors.collectingAndThen( Collectors.toList(), Collections::unmodifiableList ) );
        }
        return Collections.emptyList();
    }

    @Override
    public org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor asAnnotation() {
        Object raw = value.getValue();
        if ( raw instanceof javax.lang.model.element.AnnotationMirror ) {
            return factory.annotationDescriptor( (javax.lang.model.element.AnnotationMirror) raw );
        }
        return AnnotationValueDescriptor.super.asAnnotation();
    }

    AnnotationValue annotationValue() {
        return value;
    }

    @Override
    public Object unwrap() {
        return value;
    }

    private TypeMirror resolveTypeMirror() {
        Object raw = value.getValue();
        if ( raw instanceof TypeMirror ) {
            return (TypeMirror) raw;
        }

        TypeMirror viaVisitor = value.accept( new SimpleAnnotationValueVisitor8<TypeMirror, Void>() {
            @Override
            public TypeMirror visitType(TypeMirror t, Void p) {
                return t;
            }
        }, null );

        if ( viaVisitor != null ) {
            return viaVisitor;
        }

        return extractTypeMirror( value );
    }

    private TypeMirror extractTypeMirror(Object attribute) {
        if ( attribute == null ) {
            return null;
        }

        if ( attribute instanceof TypeMirror ) {
            return (TypeMirror) attribute;
        }

        TypeMirror fromClassType = extractField( attribute, "classType" );
        if ( fromClassType != null ) {
            return fromClassType;
        }

        return extractField( attribute, "type" );
    }

    private TypeMirror extractField(Object attribute, String fieldName) {
        Class<?> current = attribute.getClass();
        while ( current != null ) {
            try {
                Field field = current.getField( fieldName );
                Object candidate = field.get( attribute );
                if ( candidate instanceof TypeMirror ) {
                    return (TypeMirror) candidate;
                }
                return null;
            }
            catch ( NoSuchFieldException ex ) {
                current = current.getSuperclass();
            }
            catch ( IllegalAccessException ex ) {
                return null;
            }
        }
        return null;
    }
}
