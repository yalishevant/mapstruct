/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.mapstruct.ap.internal.langmodel.AnnotationGemFactory;
import org.mapstruct.ap.internal.langmodel.AnnotationGemsCapability;
import org.mapstruct.ap.internal.langmodel.BuilderIntrospectorCapability;
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
import org.mapstruct.ap.internal.langmodel.javax.codegen.JavaxGeneratedFileSink;
import org.mapstruct.ap.internal.langmodel.spi.EnumMappingCapability;
import org.mapstruct.ap.internal.langmodel.spi.LangDiagnostics;
import org.mapstruct.ap.internal.langmodel.spi.MappingExclusionCapability;
import org.mapstruct.ap.internal.langmodel.spi.SpiBridgeCapability;
import org.mapstruct.ap.internal.processor.AnnotationProcessorContext;
import org.mapstruct.ap.internal.util.IgnoreJRERequirement;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.spi.AstModifyingAnnotationProcessor;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;

import com.sun.source.util.Trees;

/**
 * {@link LangModelContext} backed by {@code javax.lang.model}.
 */
@IgnoreJRERequirement
public final class JavaxLangModelContext implements LangModelContext {

    private final ProcessingEnvironment processingEnvironment;
    private final TypeUtils typeUtils;
    private final ElementUtils elementUtils;
    private final JavaxDescriptorFactory descriptorFactory;
    private final JavaxLangTypes types;
    private final JavaxLangElements elements;
    private final JavaxTypeIntrospector typeIntrospector;
    private final AnnotationGemFactory annotationGemFactory;
    private final Trees trees;
    private final DescriptorUnwrapper descriptorUnwrapper;

    private final LangDiagnostics diagnostics;
    private final GeneratedFileAccess generatedFileAccess;
    private final AnnotationGemsCapability annotationGemsCapability;
    private final BuilderIntrospectorCapability builderIntrospectorCapability;
    private final EnumMappingCapability enumMappingCapability;
    private final MappingExclusionCapability mappingExclusionCapability;
    private final SpiBridgeCapability spiBridgeCapability;

    @IgnoreJRERequirement
    private JavaxLangModelContext(ProcessingEnvironment processingEnvironment,
                                  TypeUtils typeUtils,
                                  ElementUtils elementUtils) {
        this.processingEnvironment = processingEnvironment;
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;
        this.descriptorFactory = new JavaxDescriptorFactory( this );
        this.types = new JavaxLangTypes( this, descriptorFactory );
        this.elements = new JavaxLangElements( this, descriptorFactory );
        this.typeIntrospector = new JavaxTypeIntrospector( this );
        this.annotationGemFactory = JavaxAnnotationGemFactory.instance();
        Trees treesInstance;
        try {
            treesInstance = Trees.instance( processingEnvironment );
        }
        catch ( IllegalArgumentException ex ) {
            treesInstance = null;
        }
        this.trees = treesInstance;
        this.descriptorUnwrapper = new JavaxDescriptorUnwrapper();

        this.diagnostics = new LangDiagnostics() {
            @Override
            public void warning(String message) {
                processingEnvironment.getMessager().printMessage( Diagnostic.Kind.WARNING, message );
            }

            @Override
            public void error(String message) {
                processingEnvironment.getMessager().printMessage( Diagnostic.Kind.ERROR, message );
            }

            @Override
            public boolean isTypeComplete(TypeDescriptor descriptor,
                                          java.util.List<? extends org.mapstruct.ap.spi.AstModifyingAnnotationProcessor>
                                              processors) {
                return JavaxLangModelContext.this.isTypeComplete( descriptor, processors );
            }

            @Override
            public TypeHierarchyErroneousException typeHierarchyErroneousException(TypeDescriptor descriptor) {
                return JavaxLangModelContext.this.typeHierarchyErroneousException( descriptor );
            }
        };

        this.generatedFileAccess = new GeneratedFileAccess() {
            @Override
            public GeneratedFileSink generatedFileSink() {
                return new JavaxGeneratedFileSink(
                    JavaxLangModelContext.this.processingEnvironment.getFiler(),
                    new JavaxDescriptorUnwrapper()
                );
            }
        };

        this.annotationGemsCapability = () -> JavaxLangModelContext.this.annotationGemFactory;
        this.builderIntrospectorCapability = context -> {
            if ( !( context instanceof AnnotationProcessorContext ) ) {
                throw new IllegalArgumentException( "Expected AnnotationProcessorContext" );
            }
            return new JavaxBuilderIntrospector( JavaxLangModelContext.this, (AnnotationProcessorContext) context );
        };
        this.enumMappingCapability = strategy -> new JavaxEnumMappingSupport( JavaxLangModelContext.this, strategy );
        this.mappingExclusionCapability = provider -> new JavaxMappingExclusionSupport( provider );
        this.spiBridgeCapability = processorOptions ->
            new JavaxMapStructProcessingEnvironment(
                processingEnvironment.getElementUtils(),
                processingEnvironment.getTypeUtils(),
                processorOptions
            );
    }

    public static JavaxLangModelContext create(ProcessingEnvironment processingEnvironment,
                                               VersionInformation versionInformation,
                                               TypeElement mapperElement) {
        Objects.requireNonNull( processingEnvironment, "processingEnvironment" );
        Objects.requireNonNull( versionInformation, "versionInformation" );
        Objects.requireNonNull( mapperElement, "mapperElement" );

        TypeUtils typeUtils = TypeUtils.create( processingEnvironment, versionInformation );
        ElementUtils elementUtils = ElementUtils.create( processingEnvironment, versionInformation, mapperElement );
        return new JavaxLangModelContext( processingEnvironment, typeUtils, elementUtils );
    }

    ProcessingEnvironment processingEnvironment() {
        return processingEnvironment;
    }

    TypeUtils delegateTypeUtils() {
        return typeUtils;
    }

    ElementUtils delegateElementUtils() {
        return elementUtils;
    }

    JavaxDescriptorFactory descriptorFactory() {
        return descriptorFactory;
    }

    AnnotationGemFactory annotationGemFactory() {
        return annotationGemFactory;
    }

    JavaxLangTypes langTypes() {
        return types;
    }

    JavaxLangElements langElements() {
        return elements;
    }

    JavaxTypeIntrospector typeIntrospectorDelegate() {
        return typeIntrospector;
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
        TypeElement typeElement = element != null
            ? descriptorUnwrapper.type( element, TypeElement.class ).orElse( null )
            : null;
        return JavaxMapperAnnotations.mapper( this, typeElement );
    }

    @Override
    public Optional<MapperConfigAnnotation> mapperConfig(TypeDescriptor configType) {
        return JavaxMapperAnnotations.mapperConfig( this, configType );
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
    public <T> OptionalCapability<T> optional(Class<T> capabilityType) {
        Objects.requireNonNull( capabilityType, "capabilityType" );

        if ( capabilityType == AnnotationGemsCapability.class ) {
            return OptionalCapability.of( capabilityType.cast( annotationGemsCapability ) );
        }
        if ( capabilityType == BuilderIntrospectorCapability.class ) {
            return OptionalCapability.of( capabilityType.cast( builderIntrospectorCapability ) );
        }
        if ( capabilityType == EnumMappingCapability.class ) {
            return OptionalCapability.of( capabilityType.cast( enumMappingCapability ) );
        }
        if ( capabilityType == MappingExclusionCapability.class ) {
            return OptionalCapability.of( capabilityType.cast( mappingExclusionCapability ) );
        }
        if ( capabilityType == SpiBridgeCapability.class ) {
            return OptionalCapability.of( capabilityType.cast( spiBridgeCapability ) );
        }

        return OptionalCapability.empty();
    }

    boolean isTypeComplete(TypeDescriptor descriptor,
                           java.util.List<? extends org.mapstruct.ap.spi.AstModifyingAnnotationProcessor> processors) {
        if ( processors == null || processors.isEmpty() ) {
            return true;
        }
        TypeMirror type = descriptorUnwrapper.type( descriptor, TypeMirror.class ).orElse( null );
        if ( type == null ) {
            return true;
        }
        for ( AstModifyingAnnotationProcessor processor : processors ) {
            if ( processor != null && !processor.isTypeComplete( type ) ) {
                return false;
            }
        }
        return true;
    }

    TypeHierarchyErroneousException typeHierarchyErroneousException(TypeDescriptor descriptor) {
        if ( descriptor == null ) {
            return new TypeHierarchyErroneousException();
        }

        Object unwrapped = descriptor.unwrap();
        if ( unwrapped instanceof TypeMirror ) {
            return new TypeHierarchyErroneousException( (TypeMirror) unwrapped );
        }

        TypeElementDescriptor elementDescriptor = descriptor.typeElement().orElse( null );
        if ( elementDescriptor != null ) {
            Object elementHandle = elementDescriptor.unwrap();
            if ( elementHandle instanceof TypeElement ) {
                return new TypeHierarchyErroneousException( (TypeElement) elementHandle );
            }
        }

        return new TypeHierarchyErroneousException();
    }

    @IgnoreJRERequirement
    Trees trees() {
        return trees;
    }
}
