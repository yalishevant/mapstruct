/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.processor;

import org.mapstruct.ap.internal.codegen.GeneratedFile;
import org.mapstruct.ap.internal.gem.MappingConstantsGem;
import org.mapstruct.ap.internal.model.Decorator;
import org.mapstruct.ap.internal.model.GeneratedType;
import org.mapstruct.ap.internal.model.Mapper;
import org.mapstruct.ap.internal.model.ServicesEntry;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.source.MapperOptions;
import org.mapstruct.ap.langmodel.LangModelContext;
import org.mapstruct.ap.langmodel.MapperAnnotation;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;

/**
 * A {@link ModelElementProcessor} which creates files in the {@code META-INF/services}
 * hierarchy for classes with custom implementation class or package name.
 *
 * Service files will only be generated for mappers with the default component model
 * unless force using the {@code mapstruct.alwaysGenerateServicesFile} option.
 *
 * @author Christophe Labouisse on 12/07/2015.
 */
public class MapperServiceProcessor  implements ModelElementProcessor<Mapper, Void> {
    @Override
    public Void process(ProcessorContext context, TypeElementDescriptor mapperDescriptor, Mapper mapper) {
        if ( mapper == null ) {
            return null;
        }
        boolean spiGenerationNeeded;

        if ( context.getOptions().isAlwaysGenerateSpi() ) {
            spiGenerationNeeded = true;
        }
        else {
            LangModelContext<?, ?, ?, ?> langModelContext = context.getLangModelContext();
            MapperAnnotation mapperAnnotation = langModelContext.elementQuery().mapperAnnotation( mapperDescriptor );
            MapperOptions mapperOptions = MapperOptions.fromAnnotation(
                mapperAnnotation,
                mapperDescriptor,
                context.getOptions(),
                langModelContext
            );
            String componentModel = mapperOptions.componentModel();

            spiGenerationNeeded = MappingConstantsGem.ComponentModelGem.DEFAULT.equals( componentModel );
        }

        if ( !context.isErroneous() && spiGenerationNeeded && mapper.hasCustomImplementation() ) {
            writeToSourceFile( context, mapper );
        }
        return null;
    }

    @Override
    public int getPriority() {
        return 10000;
    }

    private void writeToSourceFile(ProcessorContext context, Mapper model) {
        ServicesEntry servicesEntry = getServicesEntry( model );
        createSourceFile( context, servicesEntry );
    }

    private ServicesEntry getServicesEntry(Mapper mapper) {
        if ( mapper.getDecorator() != null ) {
            return getServicesEntry( mapper.getDecorator() );
        }

        return getServicesEntry( mapper.getMapperDefinitionType(), mapper );
    }

    private ServicesEntry getServicesEntry(Decorator decorator) {
        return getServicesEntry( decorator.getMapperType(), decorator );
    }

    private ServicesEntry getServicesEntry(Type mapperType, GeneratedType model) {
        String mapperName = mapperType.getName();
        String mapperPackageName = mapperType.getPackageName();

        return new ServicesEntry(mapperPackageName, mapperName,
                                 model.getPackageName(), model.getName());
    }

    private void createSourceFile(ProcessorContext context, ServicesEntry model) {
        String fileName = model.getPackageName() + "." + model.getName();

        GeneratedFile generatedFile = GeneratedFile.resource(
            "META-INF/services/" + fileName,
            model
        ).build();

        context.getCodeGenerator().generate( generatedFile, context.getCodeGenerationContext() );
    }
}
