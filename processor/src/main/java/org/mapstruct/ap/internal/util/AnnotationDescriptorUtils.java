/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * Utility methods for working with {@link AnnotationDescriptor} instances.
 */
public final class AnnotationDescriptorUtils {

    private AnnotationDescriptorUtils() {
    }

    public static Optional<AnnotationDescriptor> findAnnotation(LangElements elements,
                                                                ElementDescriptor element,
                                                                String annotationFqn) {
        Objects.requireNonNull( elements, "elements" );
        Objects.requireNonNull( element, "element" );
        Objects.requireNonNull( annotationFqn, "annotationFqn" );

        for ( AnnotationDescriptor annotation : elements.annotationMirrors( element ) ) {
            if ( hasQualifiedName( annotation, annotationFqn ) ) {
                return Optional.of( annotation );
            }
        }
        return Optional.empty();
    }

    public static Optional<AnnotationDescriptor> findAnnotationBySimpleName(LangElements elements,
                                                                            ElementDescriptor element,
                                                                            String annotationSimpleName) {
        Objects.requireNonNull( elements, "elements" );
        Objects.requireNonNull( element, "element" );
        Objects.requireNonNull( annotationSimpleName, "annotationSimpleName" );

        for ( AnnotationDescriptor annotation : elements.annotationMirrors( element ) ) {
            if ( hasSimpleName( annotation, annotationSimpleName ) ) {
                return Optional.of( annotation );
            }
        }
        return Optional.empty();
    }

    public static List<AnnotationDescriptor> findRepeatableAnnotations(LangElements elements,
                                                                       ElementDescriptor element,
                                                                       String singularFqn,
                                                                       String containerFqn) {
        Objects.requireNonNull( elements, "elements" );
        Objects.requireNonNull( element, "element" );
        Objects.requireNonNull( singularFqn, "singularFqn" );
        Objects.requireNonNull( containerFqn, "containerFqn" );

        List<AnnotationDescriptor> matches = new ArrayList<>();
        for ( AnnotationDescriptor annotation : elements.annotationMirrors( element ) ) {
            if ( hasQualifiedName( annotation, singularFqn ) ) {
                matches.add( annotation );
            }
            else if ( hasQualifiedName( annotation, containerFqn ) ) {
                AnnotationValueDescriptor valueDescriptor = annotation.elementValues().get( "value" );
                if ( valueDescriptor == null ) {
                    continue;
                }
                for ( AnnotationValueDescriptor nested : valueDescriptor.asList() ) {
                    AnnotationDescriptor nestedAnnotation = nested.asAnnotation();
                    if ( nestedAnnotation != null && hasQualifiedName( nestedAnnotation, singularFqn ) ) {
                        matches.add( nestedAnnotation );
                    }
                }
            }
        }
        return Collections.unmodifiableList( matches );
    }

    public static AnnotationValueDescriptor getValue(AnnotationDescriptor annotation, String elementName) {
        Objects.requireNonNull( annotation, "annotation" );
        Objects.requireNonNull( elementName, "elementName" );
        return annotation.elementValues().get( elementName );
    }

    public static List<AnnotationValueDescriptor> getValueList(AnnotationDescriptor annotation, String elementName) {
        AnnotationValueDescriptor descriptor = getValue( annotation, elementName );
        if ( descriptor == null ) {
            return Collections.emptyList();
        }
        return descriptor.asList();
    }

    public static List<AnnotationDescriptor> getAnnotationList(AnnotationDescriptor annotation, String elementName) {
        AnnotationValueDescriptor descriptor = getValue( annotation, elementName );
        if ( descriptor == null ) {
            return Collections.emptyList();
        }
        List<AnnotationValueDescriptor> nestedValues = descriptor.asList();
        if ( nestedValues.isEmpty() ) {
            AnnotationDescriptor nested = descriptor.asAnnotation();
            if ( nested == null ) {
                return Collections.emptyList();
            }
            return Collections.singletonList( nested );
        }

        List<AnnotationDescriptor> nestedAnnotations = new ArrayList<>( nestedValues.size() );
        for ( AnnotationValueDescriptor value : nestedValues ) {
            AnnotationDescriptor nested = value.asAnnotation();
            if ( nested != null ) {
                nestedAnnotations.add( nested );
            }
        }
        return Collections.unmodifiableList( nestedAnnotations );
    }

    public static boolean hasQualifiedName(AnnotationDescriptor annotation, String annotationFqn) {
        Objects.requireNonNull( annotation, "annotation" );
        Objects.requireNonNull( annotationFqn, "annotationFqn" );
        TypeElementDescriptor type = annotation.annotationType();
        return type != null && annotationFqn.equals( type.qualifiedName() );
    }

    public static boolean hasSimpleName(AnnotationDescriptor annotation, String annotationSimpleName) {
        Objects.requireNonNull( annotation, "annotation" );
        Objects.requireNonNull( annotationSimpleName, "annotationSimpleName" );
        TypeElementDescriptor type = annotation.annotationType();
        return type != null
            && type.simpleName() != null
            && annotationSimpleName.equals( type.simpleName().content() );
    }
}
