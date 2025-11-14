/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.BuilderDescriptor;
import org.mapstruct.ap.internal.processor.AnnotationProcessorContext;
import org.mapstruct.ap.spi.BuilderInfo;
import org.mapstruct.ap.spi.MoreThanOneBuilderCreationMethodException;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;
import org.mapstruct.ap.internal.langmodel.spi.BuilderIntrospector;

final class JavaxBuilderIntrospector implements BuilderIntrospector {

    private final JavaxLangModelContext langModelContext;
    private final AnnotationProcessorContext annotationProcessorContext;

    JavaxBuilderIntrospector(JavaxLangModelContext langModelContext,
                             AnnotationProcessorContext annotationProcessorContext) {
        this.langModelContext = langModelContext;
        this.annotationProcessorContext = annotationProcessorContext;
    }

    @Override
    public BuilderDescriptor findBuilder(TypeDescriptor type)
        throws TypeHierarchyErroneousException, MoreThanOneBuilderCreationMethodException {
        if ( type == null ) {
            return null;
        }

        TypeMirror nativeType = unwrapTypeMirror( type );
        if ( nativeType == null ) {
            return null;
        }

        try {
            BuilderInfo builderInfo = annotationProcessorContext.getBuilderProvider().findBuilderInfo( nativeType );
            if ( builderInfo == null ) {
                return null;
            }

            ExecutableDescriptor creationMethod = toExecutableDescriptor( builderInfo.getBuilderCreationMethod() );
            Collection<ExecutableDescriptor> buildMethods = toExecutableDescriptors( builderInfo.getBuildMethods() );
            if ( creationMethod == null || buildMethods == null ) {
                return null;
            }

            return new BuilderDescriptor( creationMethod, buildMethods );
        }
        catch ( MoreThanOneBuilderCreationMethodException ex ) {
            Collection<ExecutableDescriptor> conflicting = extractCreationMethods( ex );
            throw new org.mapstruct.ap.internal.langmodel.BuilderIntrospectionException( type, conflicting );
        }
    }

    private Collection<ExecutableDescriptor> extractCreationMethods(MoreThanOneBuilderCreationMethodException ex) {
        if ( ex.getBuilderInfo() == null ) {
            return java.util.Collections.emptyList();
        }
        List<ExecutableDescriptor> elements = new ArrayList<>( ex.getBuilderInfo().size() );
        for ( BuilderInfo info : ex.getBuilderInfo() ) {
            ExecutableDescriptor descriptor = toExecutableDescriptor( info.getBuilderCreationMethod() );
            if ( descriptor != null ) {
                elements.add( descriptor );
            }
        }
        return elements;
    }

    private TypeMirror unwrapTypeMirror(TypeDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        Object unwrapped = descriptor.unwrap();
        return unwrapped instanceof TypeMirror ? (TypeMirror) unwrapped : null;
    }

    private ExecutableDescriptor toExecutableDescriptor(ExecutableElement element) {
        if ( element == null ) {
            return null;
        }
        org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor descriptor =
            langModelContext.descriptorFactory().elementDescriptor( element );
        if ( descriptor instanceof ExecutableDescriptor ) {
            return (ExecutableDescriptor) descriptor;
        }
        return null;
    }

    private Collection<ExecutableDescriptor> toExecutableDescriptors(Collection<ExecutableElement> elements) {
        if ( elements == null ) {
            return null;
        }
        List<ExecutableDescriptor> descriptors = new ArrayList<>( elements.size() );
        for ( ExecutableElement element : elements ) {
            ExecutableDescriptor descriptor = toExecutableDescriptor( element );
            if ( descriptor != null ) {
                descriptors.add( descriptor );
            }
        }
        return descriptors;
    }
}
