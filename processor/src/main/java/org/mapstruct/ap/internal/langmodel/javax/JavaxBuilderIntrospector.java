/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mapstruct.ap.internal.langmodel.descriptor.BuilderDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.util.AnnotationProcessorContext;
import org.mapstruct.ap.spi.BuilderInfo;
import org.mapstruct.ap.spi.MoreThanOneBuilderCreationMethodException;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;
import org.mapstruct.ap.internal.langmodel.spi.BuilderIntrospector;

final class JavaxBuilderIntrospector implements BuilderIntrospector {

    private final AnnotationProcessorContext annotationProcessorContext;

    JavaxBuilderIntrospector(AnnotationProcessorContext annotationProcessorContext) {
        this.annotationProcessorContext = annotationProcessorContext;
    }

    @Override
    public BuilderDescriptor findBuilder(TypeDescriptor type)
        throws TypeHierarchyErroneousException, MoreThanOneBuilderCreationMethodException {
        if ( type == null ) {
            return null;
        }

        try {
            BuilderInfo builderInfo = annotationProcessorContext.getBuilderProvider().findBuilderInfo( type );
            if ( builderInfo == null ) {
                return null;
            }

            return new BuilderDescriptor(
                builderInfo.getBuilderCreationMethod(),
                builderInfo.getBuildMethods()
            );
        }
        catch ( MoreThanOneBuilderCreationMethodException ex ) {
            Collection<ExecutableDescriptor> conflicting = extractCreationMethods( ex );
            throw new org.mapstruct.ap.internal.langmodel.BuilderIntrospectionException(
                ex.getType(),
                conflicting
            );
        }
    }

    private Collection<ExecutableDescriptor> extractCreationMethods(MoreThanOneBuilderCreationMethodException ex) {
        if ( ex.getBuilderInfo() == null ) {
            return java.util.Collections.emptyList();
        }
        List<ExecutableDescriptor> elements = new ArrayList<>( ex.getBuilderInfo().size() );
        for ( BuilderInfo info : ex.getBuilderInfo() ) {
            elements.add( info.getBuilderCreationMethod() );
        }
        return elements;
    }
}
