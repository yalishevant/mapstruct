/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSValueArgument;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * KSP implementation of {@link AnnotationValueDescriptor}.
 */
final class KspAnnotationValueDescriptor implements AnnotationValueDescriptor {

    private final KspDescriptorFactory factory;
    private final KSValueArgument argument;

    KspAnnotationValueDescriptor(KspDescriptorFactory factory, KSValueArgument argument) {
        this.factory = Objects.requireNonNull( factory, "factory" );
        this.argument = Objects.requireNonNull( argument, "argument" );
    }

    KSValueArgument argument() {
        return argument;
    }

    @Override
    public Object value() {
        Object rawValue = argument.getValue();
        return unwrapValue( rawValue );
    }

    @Override
    public TypeDescriptor asType() {
        Object rawValue = argument.getValue();
        if ( rawValue instanceof KSType ) {
            return factory.typeDescriptor( (KSType) rawValue );
        }
        return null;
    }

    @Override
    public List<AnnotationValueDescriptor> asList() {
        Object rawValue = argument.getValue();
        if ( rawValue instanceof List ) {
            List<?> list = (List<?>) rawValue;
            if ( list.isEmpty() ) {
                return Collections.emptyList();
            }

            List<AnnotationValueDescriptor> result = new ArrayList<>( list.size() );
            for ( Object item : list ) {
                result.add( new KspListItemAnnotationValueDescriptor( factory, item ) );
            }
            return Collections.unmodifiableList( result );
        }
        return Collections.emptyList();
    }

    @Override
    public AnnotationDescriptor asAnnotation() {
        Object rawValue = argument.getValue();
        if ( rawValue instanceof KSAnnotation ) {
            return factory.annotationDescriptor( rawValue );
        }
        return null;
    }

    @Override
    public Object unwrap() {
        return argument;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof KspAnnotationValueDescriptor ) ) {
            return false;
        }
        KspAnnotationValueDescriptor other = (KspAnnotationValueDescriptor) obj;
        return Objects.equals( argument, other.argument );
    }

    @Override
    public int hashCode() {
        return argument.hashCode();
    }

    @Override
    public String toString() {
        return "KspAnnotationValueDescriptor[" + value() + "]";
    }

    private Object unwrapValue(Object value) {
        if ( value instanceof KSType ) {
            // Return the qualified name for class literals
            KSType type = (KSType) value;
            if ( type.getDeclaration() != null && type.getDeclaration().getQualifiedName() != null ) {
                return type.getDeclaration().getQualifiedName().asString();
            }
            return type.toString();
        }
        if ( value instanceof KSClassDeclaration ) {
            KSClassDeclaration decl = (KSClassDeclaration) value;
            if ( decl.getQualifiedName() != null ) {
                return decl.getQualifiedName().asString();
            }
            return decl.getSimpleName().asString();
        }
        if ( value instanceof KSAnnotation ) {
            return value; // Keep as annotation for further processing
        }
        if ( value instanceof List ) {
            List<?> list = (List<?>) value;
            List<Object> result = new ArrayList<>( list.size() );
            for ( Object item : list ) {
                result.add( unwrapValue( item ) );
            }
            return result;
        }
        // Primitive values, strings, enums - return as-is
        return value;
    }

    /**
     * Helper descriptor for items within an annotation array value.
     */
    private static final class KspListItemAnnotationValueDescriptor implements AnnotationValueDescriptor {

        private final KspDescriptorFactory factory;
        private final Object item;

        KspListItemAnnotationValueDescriptor(KspDescriptorFactory factory, Object item) {
            this.factory = factory;
            this.item = item;
        }

        @Override
        public Object value() {
            if ( item instanceof KSType ) {
                KSType type = (KSType) item;
                if ( type.getDeclaration() != null && type.getDeclaration().getQualifiedName() != null ) {
                    return type.getDeclaration().getQualifiedName().asString();
                }
                return type.toString();
            }
            return item;
        }

        @Override
        public TypeDescriptor asType() {
            if ( item instanceof KSType ) {
                return factory.typeDescriptor( (KSType) item );
            }
            return null;
        }

        @Override
        public List<AnnotationValueDescriptor> asList() {
            return Collections.emptyList();
        }

        @Override
        public AnnotationDescriptor asAnnotation() {
            if ( item instanceof KSAnnotation ) {
                return factory.annotationDescriptor( item );
            }
            return null;
        }

        @Override
        public Object unwrap() {
            return item;
        }
    }
}
