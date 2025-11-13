/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

final class JavaxAnnotationFactory {

    private JavaxAnnotationFactory() {
    }

    static List<AnnotationDescriptor> annotations(JavaxLangModelContext context,
                                                  JavaxDescriptorFactory factory,
                                                  Element element) {
        List<? extends AnnotationMirror> mirrors = context.delegateElementUtils().getAllAnnotationMirrors( element );
        if ( mirrors.isEmpty() ) {
            return Collections.emptyList();
        }
        List<AnnotationDescriptor> descriptors = new ArrayList<>( mirrors.size() );
        for ( AnnotationMirror mirror : mirrors ) {
            AnnotationDescriptor descriptor = factory.annotationDescriptor( mirror );
            if ( descriptor != null ) {
                descriptors.add( descriptor );
            }
        }
        return Collections.unmodifiableList( descriptors );
    }

    static JavaxAnnotationDescriptor annotationDescriptor(JavaxLangModelContext context,
                                                          JavaxDescriptorFactory factory,
                                                          AnnotationMirror mirror) {
        TypeDescriptor annotationType = factory.typeDescriptor( mirror.getAnnotationType() );
        Map<String, AnnotationValueDescriptor> values = new LinkedHashMap<>();
        for ( Map.Entry<? extends javax.lang.model.element.ExecutableElement, ? extends AnnotationValue> entry :
            context.delegateElementUtils().getElementValuesWithDefaults( mirror ).entrySet() ) {
            values.put( entry.getKey().getSimpleName().toString(),
                factory.annotationValueDescriptor( entry.getValue() ) );
        }
        Set<String> declaredElements = mirror.getElementValues().keySet()
            .stream()
            .map( executable -> executable.getSimpleName().toString() )
            .collect( Collectors.collectingAndThen( Collectors.toSet(), Collections::unmodifiableSet ) );
        String annotationName = annotationType != null && annotationType.typeElement().isPresent()
            ? annotationType.typeElement().get().qualifiedName()
            : String.valueOf( mirror.getAnnotationType() );
        TypeElementDescriptor typeElementDescriptor = annotationType.typeElement().orElse( null );
        return new JavaxAnnotationDescriptor(
            mirror,
            typeElementDescriptor,
            Collections.unmodifiableMap( values ),
            declaredElements
        );
    }
}
