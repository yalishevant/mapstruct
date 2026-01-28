/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.processing.CodeGenerator;
import com.google.devtools.ksp.processing.KSPLogger;
import com.google.devtools.ksp.processing.Resolver;
import com.google.devtools.ksp.symbol.KSClassDeclaration;

import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapterFactory;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.LangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.MapperEntryPoint;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.version.VersionInformation;

import java.util.Collections;
import java.util.Map;

/**
 * {@link LangModelContextFactory} producing {@link KspLangModelContext} instances.
 */
public final class KspLangModelContextFactory implements LangModelContextFactory {

    private static final DescriptorUnwrapper DESCRIPTOR_UNWRAPPER = new KspDescriptorUnwrapper();
    private static final AccessorNamingAdapterFactory ACCESSOR_NAMING_ADAPTER_FACTORY =
        new KspAccessorNamingAdapterFactory();

    @Override
    public LangModelContext create(MapperEntryPoint entryPoint) {

        VersionInformation versionInformation = entryPoint.versionInformation();

        Resolver resolver = entryPoint.unwrap( Resolver.class )
            .orElseThrow( () -> new IllegalStateException(
                "Resolver handle is required for the KSP backend" ) );

        KSPLogger logger = entryPoint.unwrap( KSPLogger.class )
            .orElseThrow( () -> new IllegalStateException(
                "KSPLogger handle is required for the KSP backend" ) );

        CodeGenerator codeGenerator = entryPoint.unwrap( CodeGenerator.class )
            .orElseThrow( () -> new IllegalStateException(
                "CodeGenerator handle is required for the KSP backend" ) );

        KSClassDeclaration mapperElement = entryPoint.unwrap( KSClassDeclaration.class ).orElse( null );

        @SuppressWarnings( "unchecked" )
        Map<String, String> processorOptions = entryPoint.unwrap( Map.class )
            .map( m -> (Map<String, String>) m )
            .orElse( Collections.emptyMap() );

        return KspLangModelContext.create(
            resolver, logger, codeGenerator, versionInformation, mapperElement, processorOptions );
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
        return "ksp";
    }
}
