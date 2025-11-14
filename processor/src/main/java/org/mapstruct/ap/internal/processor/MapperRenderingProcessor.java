/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.processor;

import org.mapstruct.ap.internal.codegen.GeneratedFile;
import org.mapstruct.ap.internal.model.GeneratedType;
import org.mapstruct.ap.internal.model.Mapper;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * A {@link ModelElementProcessor} which creates a Java source file representing
 * the given {@link Mapper} object, unless the given mapper type is erroneous.
 *
 * @author Gunnar Morling
 */
public class MapperRenderingProcessor implements ModelElementProcessor<Mapper, Mapper> {

    @Override
    public Mapper process(ProcessorContext context, TypeElementDescriptor mapperDescriptor, Mapper mapper) {
        if ( mapper == null ) {
            return null;
        }
        if ( !context.isErroneous() ) {
            writeToSourceFile( context, mapper, mapperDescriptor );
            return mapper;
        }

        return null;
    }

    private void writeToSourceFile(ProcessorContext context,
                                   Mapper model,
                                   TypeElementDescriptor originatingDescriptor) {
        createSourceFile( context, model, originatingDescriptor );
        if ( model.getDecorator() != null ) {
            createSourceFile( context, model.getDecorator(), originatingDescriptor );
        }
    }

    private void createSourceFile(ProcessorContext context, GeneratedType model,
                                  TypeElementDescriptor originatingDescriptor) {
        String fileName = "";
        if ( model.hasPackageName() ) {
            fileName += model.getPackageName() + ".";
        }
        fileName += model.getName();

        GeneratedFile generatedFile = GeneratedFile.javaSource(
            model.getPackageName(),
            model.getName(),
            model
        )
            .addOriginatingElement( originatingDescriptor )
            .build();

        context.getCodeGenerator().generate( generatedFile, context.getCodeGenerationContext() );
    }

    @Override
    public int getPriority() {
        return 9999;
    }
}
