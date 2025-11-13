/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.langmodel.annotation.AnnotationAttribute;
import org.mapstruct.ap.internal.langmodel.annotation.MapperAnnotationView;
import org.mapstruct.ap.internal.langmodel.annotation.MapperConfigAnnotationView;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

final class JavaxMapperAnnotation {

    private static final String MAPPER_ANNOTATION_FQN = "org.mapstruct.Mapper";

    private static final MapperDefaults DEFAULTS = MapperDefaults.create();

    private final JavaxLangModelContext context;
    private final AnnotationDescriptor descriptor;
    private final AnnotationAttribute<String> implementationName;
    private final AnnotationAttribute<String> implementationPackage;
    private final AnnotationAttribute<java.util.List<TypeDescriptor>> uses;
    private final AnnotationAttribute<java.util.List<TypeDescriptor>> imports;
    private final AnnotationAttribute<String> unmappedTargetPolicy;
    private final AnnotationAttribute<String> unmappedSourcePolicy;
    private final AnnotationAttribute<String> typeConversionPolicy;
    private final AnnotationAttribute<String> componentModel;
    private final AnnotationAttribute<Boolean> suppressTimestampInGenerated;
    private final AnnotationAttribute<String> mappingInheritanceStrategy;
    private final AnnotationAttribute<String> injectionStrategy;
    private final AnnotationAttribute<Boolean> disableSubMappingMethodsGeneration;
    private final AnnotationAttribute<String> collectionMappingStrategy;
    private final AnnotationAttribute<String> nullValueCheckStrategy;
    private final AnnotationAttribute<String> nullValuePropertyMappingStrategy;
    private final AnnotationAttribute<String> nullValueMappingStrategy;
    private final AnnotationAttribute<String> subclassExhaustiveStrategy;
    private final AnnotationAttribute<TypeDescriptor> subclassExhaustiveException;
    private final AnnotationAttribute<String> nullValueIterableMappingStrategy;
    private final AnnotationAttribute<String> nullValueMapMappingStrategy;
    private final AnnotationAttribute<BuilderGem> builder;
    private final AnnotationAttribute<TypeDescriptor> mappingControl;
    private final AnnotationAttribute<TypeDescriptor> unexpectedValueMappingException;
    private final AnnotationAttribute<TypeDescriptor> config;
    private Optional<MapperConfigAnnotationView> mapperConfig;
    private final boolean valid;

    private JavaxMapperAnnotation(JavaxLangModelContext context,
                                  AnnotationDescriptor descriptor,
                                  TypeElement mapperElement) {
        this.context = context;
        this.descriptor = descriptor;
        boolean[] error = new boolean[1];

        this.implementationName = safeAttribute( "implementationName",
            descriptor,
            () -> JavaxAnnotationAttributeFactory.stringAttribute(
                value( descriptor, "implementationName" ),
                DEFAULTS.implementationName
            ),
            AnnotationAttribute.absent( DEFAULTS.implementationName ),
            error
        );
        this.implementationPackage = safeAttribute( "implementationPackage",
            descriptor,
            () -> JavaxAnnotationAttributeFactory.stringAttribute(
                value( descriptor, "implementationPackage" ),
                DEFAULTS.implementationPackage
            ),
            AnnotationAttribute.absent( DEFAULTS.implementationPackage ),
            error
        );
        this.uses = safeAttribute( "uses",
            descriptor,
            () -> JavaxAnnotationAttributeFactory.typeListAttribute(
                context,
                mapperElement,
                descriptor,
                value( descriptor, "uses" ),
                descriptor.hasValue( "uses" )
            ),
            AnnotationAttribute.absent( java.util.Collections.emptyList() ),
            error
        );
        this.imports = safeAttribute( "imports",
            descriptor,
            () -> JavaxAnnotationAttributeFactory.typeListAttribute(
                context,
                mapperElement,
                descriptor,
                value( descriptor, "imports" ),
                descriptor.hasValue( "imports" )
            ),
            AnnotationAttribute.absent( java.util.Collections.emptyList() ),
            error
        );
        this.unmappedTargetPolicy = safeAttribute( "unmappedTargetPolicy",
            descriptor,
            () -> enumAttribute( value( descriptor, "unmappedTargetPolicy" ), DEFAULTS.unmappedTargetPolicy ),
            AnnotationAttribute.absent( DEFAULTS.unmappedTargetPolicy ),
            error
        );
        this.unmappedSourcePolicy = safeAttribute( "unmappedSourcePolicy",
            descriptor,
            () -> enumAttribute( value( descriptor, "unmappedSourcePolicy" ), DEFAULTS.unmappedSourcePolicy ),
            AnnotationAttribute.absent( DEFAULTS.unmappedSourcePolicy ),
            error
        );
        this.typeConversionPolicy = safeAttribute( "typeConversionPolicy",
            descriptor,
            () -> enumAttribute( value( descriptor, "typeConversionPolicy" ), DEFAULTS.typeConversionPolicy ),
            AnnotationAttribute.absent( DEFAULTS.typeConversionPolicy ),
            error
        );
        this.componentModel = safeAttribute(
            "componentModel",
            descriptor,
            () -> JavaxAnnotationAttributeFactory.stringAttribute(
                value( descriptor, "componentModel" ),
                DEFAULTS.componentModel
            ),
            AnnotationAttribute.absent( DEFAULTS.componentModel ),
            error
        );
        this.suppressTimestampInGenerated = safeAttribute(
            "suppressTimestampInGenerated",
            descriptor,
            () -> JavaxAnnotationAttributeFactory.booleanAttribute(
                value( descriptor, "suppressTimestampInGenerated" ),
                DEFAULTS.suppressTimestampInGenerated
            ),
            AnnotationAttribute.absent( DEFAULTS.suppressTimestampInGenerated ),
            error
        );
        this.mappingInheritanceStrategy = safeAttribute(
            "mappingInheritanceStrategy",
            descriptor,
            () -> enumAttribute(
                value( descriptor, "mappingInheritanceStrategy" ),
                DEFAULTS.mappingInheritanceStrategy
            ),
            AnnotationAttribute.absent( DEFAULTS.mappingInheritanceStrategy ),
            error
        );
        this.injectionStrategy = safeAttribute(
            "injectionStrategy",
            descriptor,
            () -> enumAttribute(
                value( descriptor, "injectionStrategy" ),
                DEFAULTS.injectionStrategy
            ),
            AnnotationAttribute.absent( DEFAULTS.injectionStrategy ),
            error
        );
        this.disableSubMappingMethodsGeneration = safeAttribute(
            "disableSubMappingMethodsGeneration",
            descriptor,
            () -> JavaxAnnotationAttributeFactory.booleanAttribute(
                value( descriptor, "disableSubMappingMethodsGeneration" ),
                DEFAULTS.disableSubMappingMethodsGeneration
            ),
            AnnotationAttribute.absent( DEFAULTS.disableSubMappingMethodsGeneration ),
            error
        );
        this.collectionMappingStrategy = safeAttribute(
            "collectionMappingStrategy",
            descriptor,
            () -> enumAttribute(
                value( descriptor, "collectionMappingStrategy" ),
                DEFAULTS.collectionMappingStrategy
            ),
            AnnotationAttribute.absent( DEFAULTS.collectionMappingStrategy ),
            error
        );
        this.nullValueCheckStrategy = safeAttribute(
            "nullValueCheckStrategy",
            descriptor,
            () -> enumAttribute(
                value( descriptor, "nullValueCheckStrategy" ),
                DEFAULTS.nullValueCheckStrategy
            ),
            AnnotationAttribute.absent( DEFAULTS.nullValueCheckStrategy ),
            error
        );
        this.nullValuePropertyMappingStrategy = safeAttribute(
            "nullValuePropertyMappingStrategy",
            descriptor,
            () -> enumAttribute(
                value( descriptor, "nullValuePropertyMappingStrategy" ),
                DEFAULTS.nullValuePropertyMappingStrategy
            ),
            AnnotationAttribute.absent( DEFAULTS.nullValuePropertyMappingStrategy ),
            error
        );
        this.nullValueMappingStrategy = safeAttribute(
            "nullValueMappingStrategy",
            descriptor,
            () -> enumAttribute(
                value( descriptor, "nullValueMappingStrategy" ),
                DEFAULTS.nullValueMappingStrategy
            ),
            AnnotationAttribute.absent( DEFAULTS.nullValueMappingStrategy ),
            error
        );
        this.subclassExhaustiveStrategy = safeAttribute(
            "subclassExhaustiveStrategy",
            descriptor,
            () -> enumAttribute(
                value( descriptor, "subclassExhaustiveStrategy" ),
                DEFAULTS.subclassExhaustiveStrategy
            ),
            AnnotationAttribute.absent( DEFAULTS.subclassExhaustiveStrategy ),
            error
        );
        this.subclassExhaustiveException = safeAttribute(
            "subclassExhaustiveException",
            descriptor,
            () -> JavaxAnnotationAttributeFactory.typeAttribute(
                value( descriptor, "subclassExhaustiveException" ),
                toTypeDescriptor( DEFAULTS.subclassExhaustiveException, context )
            ),
            AnnotationAttribute.absent( toTypeDescriptor( DEFAULTS.subclassExhaustiveException, context ) ),
            error
        );
        this.nullValueIterableMappingStrategy = safeAttribute(
            "nullValueIterableMappingStrategy",
            descriptor,
            () -> enumAttribute(
                value( descriptor, "nullValueIterableMappingStrategy" ),
                DEFAULTS.nullValueIterableMappingStrategy
            ),
            AnnotationAttribute.absent( DEFAULTS.nullValueIterableMappingStrategy ),
            error
        );
        this.nullValueMapMappingStrategy = safeAttribute(
            "nullValueMapMappingStrategy",
            descriptor,
            () -> enumAttribute(
                value( descriptor, "nullValueMapMappingStrategy" ),
                DEFAULTS.nullValueMapMappingStrategy
            ),
            AnnotationAttribute.absent( DEFAULTS.nullValueMapMappingStrategy ),
            error
        );
        this.builder = safeAttribute(
            "builder",
            descriptor,
            () -> JavaxAnnotationAttributeFactory.builderAttribute(
                context,
                value( descriptor, "builder" )
            ),
            AnnotationAttribute.absent( null ),
            error
        );
        this.mappingControl = safeAttribute(
            "mappingControl",
            descriptor,
            () -> JavaxAnnotationAttributeFactory.typeAttribute(
                value( descriptor, "mappingControl" ),
                toTypeDescriptor( DEFAULTS.mappingControl, context )
            ),
            AnnotationAttribute.absent( toTypeDescriptor( DEFAULTS.mappingControl, context ) ),
            error
        );
        this.unexpectedValueMappingException = safeAttribute(
            "unexpectedValueMappingException",
            descriptor,
            () -> JavaxAnnotationAttributeFactory.typeAttribute(
                value( descriptor, "unexpectedValueMappingException" ),
                toTypeDescriptor( DEFAULTS.unexpectedValueMappingException, context )
            ),
            AnnotationAttribute.absent( toTypeDescriptor( DEFAULTS.unexpectedValueMappingException, context ) ),
            error
        );
        this.config = safeAttribute( "config",
            descriptor,
            () -> {
                try {
                    return JavaxAnnotationAttributeFactory.typeAttribute(
                        value( descriptor, "config" ),
                        toTypeDescriptor( DEFAULTS.config, context )
                    );
                }
                catch ( org.mapstruct.ap.spi.TypeHierarchyErroneousException ex ) {
                    throw ex;
                }
            },
            AnnotationAttribute.absent( toTypeDescriptor( DEFAULTS.config, context ) ),
            error
        );

        if ( descriptor == null || error[0] ) {
            this.mapperConfig = Optional.empty();
        }
        else {
            TypeDescriptor configValue = this.config.value().orElse( null );
            if ( this.config.hasValue() && configValue != null && configValue.kind() != LangTypeKind.VOID ) {
                this.mapperConfig = JavaxMapperConfigAnnotation.from( context, configValue );
            }
            else {
                this.mapperConfig = Optional.empty();
            }
        }

        this.valid = descriptor != null && !error[0];
    }

    private MapperAnnotationView toView() {
        return MapperAnnotationView.create(
            descriptor,
            valid,
            implementationName,
            implementationPackage,
            uses,
            imports,
            unmappedTargetPolicy,
            unmappedSourcePolicy,
            typeConversionPolicy,
            componentModel,
            suppressTimestampInGenerated,
            mappingInheritanceStrategy,
            injectionStrategy,
            disableSubMappingMethodsGeneration,
            collectionMappingStrategy,
            nullValueCheckStrategy,
            nullValuePropertyMappingStrategy,
            nullValueMappingStrategy,
            subclassExhaustiveStrategy,
            subclassExhaustiveException,
            nullValueIterableMappingStrategy,
            nullValueMapMappingStrategy,
            builder,
            mappingControl,
            unexpectedValueMappingException,
            config,
            mapperConfig
        );
    }

    static MapperAnnotationView from(JavaxLangModelContext context, TypeElement element) {
        try {
            if ( element == null ) {
                return MapperAnnotationView.absent();
            }
            java.util.List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
            AnnotationMirror mapperMirror = null;
            for ( AnnotationMirror mirror : annotationMirrors ) {
                try {
                    Element annotationElement = mirror.getAnnotationType().asElement();
                    if ( annotationElement instanceof TypeElement ) {
                        Name name = ( (TypeElement) annotationElement ).getQualifiedName();
                        if ( name != null && name.contentEquals( MAPPER_ANNOTATION_FQN ) ) {
                            mapperMirror = mirror;
                            break;
                        }
                    }
                }
                catch ( org.mapstruct.ap.spi.TypeHierarchyErroneousException ex ) {
                    throw ex;
                }
                catch ( Throwable ex ) {
                    // skip erroneous annotations
                }
            }
            if ( mapperMirror == null ) {
                return MapperAnnotationView.absent();
            }
            AnnotationDescriptor descriptor = context.descriptorFactory().annotationDescriptor( mapperMirror );
            return new JavaxMapperAnnotation( context, descriptor, element ).toView();
        }
        catch ( org.mapstruct.ap.spi.TypeHierarchyErroneousException ex ) {
            throw ex;
        }
        catch ( Throwable ex ) {
            return MapperAnnotationView.absent();
        }
    }

    @Override
    private static AnnotationAttribute<String> enumAttribute(AnnotationValueDescriptor descriptor,
                                                             String defaultValue) {
        if ( descriptor == null ) {
            return AnnotationAttribute.absent( defaultValue );
        }
        String representation = AnnotationValueUtils.asString( descriptor );
        if ( representation == null ) {
            return AnnotationAttribute.absent( defaultValue );
        }
        int lastDot = representation.lastIndexOf( '.' );
        String enumName = lastDot >= 0 && lastDot + 1 < representation.length()
            ? representation.substring( lastDot + 1 )
            : representation;
        return AnnotationAttribute.present( enumName, defaultValue );
    }

    private static <T> AnnotationAttribute<T> safeAttribute(String attributeName,
                                                            AnnotationDescriptor descriptor,
                                                            Supplier<AnnotationAttribute<T>> supplier,
                                                            AnnotationAttribute<T> defaultAttribute,
                                                            boolean[] errorFlag) {
        if ( descriptor == null ) {
            return defaultAttribute;
        }
        try {
            return supplier.get();
        }
        catch ( Throwable ex ) {
            if ( ex instanceof org.mapstruct.ap.spi.TypeHierarchyErroneousException ) {
                throw (org.mapstruct.ap.spi.TypeHierarchyErroneousException) ex;
            }
            errorFlag[0] = true;
            return defaultAttribute;
        }
    }

    private static AnnotationValueDescriptor value(AnnotationDescriptor descriptor, String elementName) {
        if ( descriptor == null || !descriptor.hasValue( elementName ) ) {
            return null;
        }
        return AnnotationDescriptorUtils.getValue( descriptor, elementName );
    }

    private static TypeDescriptor toTypeDescriptor(Class<?> type, JavaxLangModelContext context) {
        if ( type == null || type == void.class || type == Void.class ) {
            return null;
        }
        TypeElement element = context.delegateElementUtils().getTypeElement( type.getCanonicalName() );
        if ( element == null ) {
            return null;
        }
        return context.descriptorFactory().typeDescriptor( element.asType() );
    }

    private static final class MapperDefaults {
        final String implementationName = "<CLASS_NAME>Impl";
        final String implementationPackage = "<PACKAGE_NAME>";
        final String unmappedTargetPolicy = "WARN";
        final String unmappedSourcePolicy = "IGNORE";
        final String typeConversionPolicy = "IGNORE";
        final String componentModel = "default";
        final boolean suppressTimestampInGenerated = false;
        final String mappingInheritanceStrategy = "EXPLICIT";
        final String injectionStrategy = "FIELD";
        final boolean disableSubMappingMethodsGeneration = false;
        final String collectionMappingStrategy = "ACCESSOR_ONLY";
        final String nullValueCheckStrategy = "ON_IMPLICIT_CONVERSION";
        final String nullValuePropertyMappingStrategy = "SET_TO_NULL";
        final String nullValueMappingStrategy = "RETURN_NULL";
        final String subclassExhaustiveStrategy = "COMPILE_ERROR";
        final Class<?> subclassExhaustiveException = IllegalArgumentException.class;
        final String nullValueIterableMappingStrategy = "RETURN_NULL";
        final String nullValueMapMappingStrategy = "RETURN_NULL";
        final Class<?> mappingControl = null;
        final Class<?> unexpectedValueMappingException = IllegalArgumentException.class;
        final Class<?> config = void.class;

        static MapperDefaults create() {
            return new MapperDefaults();
        }
    }

}
