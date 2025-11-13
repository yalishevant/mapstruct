/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax.codegen;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.mapstruct.ap.internal.langmodel.codegen.GeneratedFileSink;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;

/**
 * {@link GeneratedFileSink} backed by the JSR 269 {@link Filer}.
 */
public final class JavaxGeneratedFileSink implements GeneratedFileSink {

    private final Filer filer;
    private final DescriptorUnwrapper descriptorUnwrapper;

    public JavaxGeneratedFileSink(Filer filer, DescriptorUnwrapper descriptorUnwrapper) {
        this.filer = Objects.requireNonNull( filer, "filer" );
        this.descriptorUnwrapper = Objects.requireNonNull( descriptorUnwrapper, "descriptorUnwrapper" );
    }

    @Override
    public Writer createJavaSourceWriter(String packageName, String simpleName, List<Object> originatingElements)
        throws IOException {

        String qualifiedName = ( packageName == null || packageName.isEmpty() )
            ? simpleName
            : packageName + "." + simpleName;

        JavaFileObject javaFile = filer.createSourceFile(
            qualifiedName,
            unwrapOriginatingElements( originatingElements )
        );

        return new BufferedWriter( javaFile.openWriter() );
    }

    @Override
    public Writer createResourceWriter(String resourceName, List<Object> originatingElements) throws IOException {
        FileObject resource = filer.createResource(
            StandardLocation.CLASS_OUTPUT,
            "",
            resourceName,
            unwrapOriginatingElements( originatingElements )
        );
        return new BufferedWriter( resource.openWriter() );
    }

    private Element[] unwrapOriginatingElements(List<Object> originatingElements) {
        if ( originatingElements == null || originatingElements.isEmpty() ) {
            return new Element[0];
        }

        List<Element> resolved = new ArrayList<>( originatingElements.size() );
        for ( Object origin : originatingElements ) {
            if ( origin instanceof Element ) {
                resolved.add( (Element) origin );
            }
            else if ( origin instanceof ElementDescriptor ) {
                descriptorUnwrapper.element( (ElementDescriptor) origin, Element.class )
                    .ifPresent( resolved::add );
            }
        }
        return resolved.toArray( new Element[0] );
    }
}
