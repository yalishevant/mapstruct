/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapterFactory;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.LangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.MapperEntryPoint;

/**
 * {@link LangModelContextFactory} producing {@link JavaxLangModelContext} instances.
 */
public final class JavaxLangModelContextFactory implements LangModelContextFactory {

    private static final DescriptorUnwrapper DESCRIPTOR_UNWRAPPER = new JavaxDescriptorUnwrapper();
    private static final AccessorNamingAdapterFactory ACCESSOR_NAMING_ADAPTER_FACTORY =
        new JavaxAccessorNamingAdapterFactory();

    @Override
    public LangModelContext create(MapperEntryPoint entryPoint) {

        VersionInformation versionInformation = entryPoint.versionInformation();
        javax.annotation.processing.ProcessingEnvironment processingEnvironment = entryPoint.unwrap(
            javax.annotation.processing.ProcessingEnvironment.class
        ).orElseThrow( () -> new IllegalStateException(
            "ProcessingEnvironment handle is required for the javax backend" ) );
        TypeElement mapperElement = entryPoint.unwrap( TypeElement.class ).orElse( null );

        return JavaxLangModelContext.create( processingEnvironment, versionInformation, mapperElement );
    }

    @Override
    public DescriptorUnwrapper descriptorUnwrapper() {
        return DESCRIPTOR_UNWRAPPER;
    }

    @Override
    public AccessorNamingAdapterFactory accessorNamingAdapterFactory() {
        return ACCESSOR_NAMING_ADAPTER_FACTORY;
    }

    @Override
    public String backendId() {
        return "javax";
    }
}
