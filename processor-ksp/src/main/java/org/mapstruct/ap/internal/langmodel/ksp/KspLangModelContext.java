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
import com.google.devtools.ksp.symbol.KSType;

import org.mapstruct.ap.internal.langmodel.GeneratedFileAccess;
import org.mapstruct.ap.internal.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.MapperAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperConfigAnnotation;
import org.mapstruct.ap.internal.langmodel.OptionalCapability;
import org.mapstruct.ap.internal.langmodel.TypeIntrospector;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.api.LangTypes;
import org.mapstruct.ap.internal.langmodel.codegen.GeneratedFileSink;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.spi.LangDiagnostics;
import org.mapstruct.ap.internal.langmodel.spi.SpiBridgeCapability;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.spi.AstModifyingAnnotationProcessor;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * KSP implementation of {@link LangModelContext}.
 * Provides access to language-neutral descriptors bound to a specific KSP processing round.
 */
public final class KspLangModelContext implements LangModelContext {

    private final Resolver resolver;
    private final KSPLogger logger;
    private final CodeGenerator codeGenerator;
    private final VersionInformation versionInformation;
    private final KSClassDeclaration mapperElement;

    private final KspDescriptorFactory descriptorFactory;
    private final KspLangTypes types;
    private final KspLangElements elements;
    private final KspTypeIntrospector typeIntrospector;
    private final DescriptorUnwrapper descriptorUnwrapper;

    private final LangDiagnostics diagnostics;
    private final GeneratedFileAccess generatedFileAccess;
    private final SpiBridgeCapability spiBridgeCapability;

    private KspLangModelContext(Resolver resolver,
                                KSPLogger logger,
                                CodeGenerator codeGenerator,
                                VersionInformation versionInformation,
                                KSClassDeclaration mapperElement) {
        this.resolver = Objects.requireNonNull( resolver, "resolver" );
        this.logger = Objects.requireNonNull( logger, "logger" );
        this.codeGenerator = Objects.requireNonNull( codeGenerator, "codeGenerator" );
        this.versionInformation = Objects.requireNonNull( versionInformation, "versionInformation" );
        this.mapperElement = mapperElement;

        this.descriptorFactory = new KspDescriptorFactory( this, resolver );
        this.types = new KspLangTypes( this, descriptorFactory, resolver );
        this.elements = new KspLangElements( this, descriptorFactory, resolver );
        this.typeIntrospector = new KspTypeIntrospector( this, types, elements );
        this.descriptorUnwrapper = new KspDescriptorUnwrapper();

        this.diagnostics = new LangDiagnostics() {
            @Override
            public void warning(String message) {
                logger.warn( message, null );
            }

            @Override
            public void error(String message) {
                logger.error( message, null );
            }

            @Override
            public boolean isTypeComplete(TypeDescriptor descriptor,
                                          List<? extends AstModifyingAnnotationProcessor> processors) {
                // In KSP, types are always complete within a processing round
                // KSP doesn't have the same incremental compilation model as javac
                return true;
            }

            @Override
            public TypeHierarchyErroneousException typeHierarchyErroneousException(TypeDescriptor descriptor) {
                return KspLangModelContext.this.typeHierarchyErroneousException( descriptor );
            }
        };

        this.generatedFileAccess = new GeneratedFileAccess() {
            @Override
            public GeneratedFileSink generatedFileSink() {
                return new KspGeneratedFileSink( codeGenerator, new KspDescriptorUnwrapper() );
            }
        };

        this.spiBridgeCapability = processorOptions -> new KspMapStructProcessingEnvironment( processorOptions );
    }

    /**
     * Creates a new KSP language model context.
     *
     * @param resolver           KSP resolver
     * @param logger             KSP logger
     * @param codeGenerator      KSP code generator
     * @param versionInformation version metadata
     * @param mapperElement      mapper class declaration (may be null)
     *
     * @return new context
     */
    public static KspLangModelContext create(Resolver resolver,
                                              KSPLogger logger,
                                              CodeGenerator codeGenerator,
                                              VersionInformation versionInformation,
                                              KSClassDeclaration mapperElement) {
        return new KspLangModelContext( resolver, logger, codeGenerator, versionInformation, mapperElement );
    }

    Resolver resolver() {
        return resolver;
    }

    KSPLogger logger() {
        return logger;
    }

    CodeGenerator codeGenerator() {
        return codeGenerator;
    }

    VersionInformation versionInformation() {
        return versionInformation;
    }

    KSClassDeclaration mapperElement() {
        return mapperElement;
    }

    KspDescriptorFactory descriptorFactory() {
        return descriptorFactory;
    }

    KspLangTypes langTypes() {
        return types;
    }

    KspLangElements langElements() {
        return elements;
    }

    @Override
    public LangDescriptorFactory descriptors() {
        return descriptorFactory;
    }

    @Override
    public LangTypes types() {
        return types;
    }

    @Override
    public TypeIntrospector typeIntrospector() {
        return typeIntrospector;
    }

    @Override
    public LangElements elements() {
        return elements;
    }

    @Override
    public MapperAnnotation mapperAnnotation(TypeElementDescriptor element) {
        KSClassDeclaration classDecl = element != null
            ? descriptorUnwrapper.type( element, KSClassDeclaration.class ).orElse( null )
            : null;
        return KspMapperAnnotations.mapper( this, classDecl );
    }

    @Override
    public Optional<MapperConfigAnnotation> mapperConfig(TypeDescriptor configType) {
        return KspMapperAnnotations.mapperConfig( this, configType );
    }

    @Override
    public LangDiagnostics diagnostics() {
        return diagnostics;
    }

    @Override
    public GeneratedFileAccess generatedFiles() {
        return generatedFileAccess;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T> OptionalCapability<T> optional(Class<T> capabilityType) {
        Objects.requireNonNull( capabilityType, "capabilityType" );

        if ( capabilityType == SpiBridgeCapability.class ) {
            return OptionalCapability.of( (T) spiBridgeCapability );
        }

        // Other optional capabilities can be added here as needed
        return OptionalCapability.empty();
    }

    private TypeHierarchyErroneousException typeHierarchyErroneousException(TypeDescriptor descriptor) {
        if ( descriptor == null ) {
            return new TypeHierarchyErroneousException();
        }

        Object unwrapped = descriptor.unwrap();
        if ( unwrapped instanceof KSType ) {
            // KSP doesn't have a direct equivalent to TypeMirror for TypeHierarchyErroneousException
            // Return a generic exception
            return new TypeHierarchyErroneousException();
        }

        TypeElementDescriptor elementDescriptor = descriptor.typeElement().orElse( null );
        if ( elementDescriptor != null ) {
            Object elementHandle = elementDescriptor.unwrap();
            if ( elementHandle instanceof KSClassDeclaration ) {
                // Similarly, return a generic exception
                return new TypeHierarchyErroneousException();
            }
        }

        return new TypeHierarchyErroneousException();
    }
}
