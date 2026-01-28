/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSValueArgument;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * KSP implementation of {@link AnnotationDescriptor}.
 */
final class KspAnnotationDescriptor implements AnnotationDescriptor {

    private final KspDescriptorFactory factory;
    private final KSAnnotation annotation;
    private Map<String, AnnotationValueDescriptor> elementValues;

    KspAnnotationDescriptor(KspDescriptorFactory factory, KSAnnotation annotation) {
        this.factory = Objects.requireNonNull( factory, "factory" );
        this.annotation = Objects.requireNonNull( annotation, "annotation" );
    }

    KSAnnotation annotation() {
        return annotation;
    }

    @Override
    public TypeElementDescriptor annotationType() {
        KSType type = annotation.getAnnotationType().resolve();
        KSDeclaration declaration = type.getDeclaration();
        if ( declaration instanceof KSClassDeclaration ) {
            return factory.typeElementDescriptor( declaration );
        }
        return null;
    }

    @Override
    public Map<String, AnnotationValueDescriptor> elementValues() {
        if ( elementValues == null ) {
            elementValues = computeElementValues();
        }
        return elementValues;
    }

    @Override
    public boolean hasValue(String elementName) {
        // Check if the argument was explicitly provided (not using default)
        for ( KSValueArgument arg : annotation.getArguments() ) {
            if ( arg.getName() != null && arg.getName().asString().equals( elementName ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object unwrap() {
        return annotation;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof KspAnnotationDescriptor ) ) {
            return false;
        }
        KspAnnotationDescriptor other = (KspAnnotationDescriptor) obj;
        return Objects.equals( annotation, other.annotation );
    }

    @Override
    public int hashCode() {
        return annotation.hashCode();
    }

    @Override
    public String toString() {
        TypeElementDescriptor type = annotationType();
        String typeName = type != null ? type.qualifiedName() : "unknown";
        return "KspAnnotationDescriptor[@" + typeName + "]";
    }

    private Map<String, AnnotationValueDescriptor> computeElementValues() {
        var arguments = annotation.getArguments();
        if ( arguments.isEmpty() ) {
            return Collections.emptyMap();
        }

        Map<String, AnnotationValueDescriptor> result = new LinkedHashMap<>();
        for ( KSValueArgument arg : arguments ) {
            String name = arg.getName() != null ? arg.getName().asString() : "";
            if ( !name.isEmpty() ) {
                result.put( name, new KspAnnotationValueDescriptor( factory, arg ) );
            }
        }
        return Collections.unmodifiableMap( result );
    }
}
