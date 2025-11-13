/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.Optional;

import javax.lang.model.element.TypeElement;

import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.langmodel.annotation.AnnotationAttribute;
import org.mapstruct.ap.internal.langmodel.annotation.MapperConfigAnnotationView;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

final class JavaxMapperConfigAnnotation {

    private static final String MAPPER_CONFIG_FQN = "org.mapstruct.MapperConfig";
    private static final MapperConfigDefaults DEFAULTS = new MapperConfigDefaults();

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
    private final AnnotationAttribute<String> nullValueMappingStrategy;
    private final AnnotationAttribute<String> nullValueIterableMappingStrategy;
    private final AnnotationAttribute<String> nullValueMapMappingStrategy;
    private final AnnotationAttribute<String> nullValuePropertyMappingStrategy;
    private final AnnotationAttribute<String> nullValueCheckStrategy;
    private final AnnotationAttribute<String> subclassExhaustiveStrategy;
    private final AnnotationAttribute<TypeDescriptor> subclassExhaustiveException;
    private final AnnotationAttribute<BuilderGem> builder;
    private final AnnotationAttribute<TypeDescriptor> mappingControl;
    private final AnnotationAttribute<TypeDescriptor> unexpectedValueMappingException;

    private JavaxMapperConfigAnnotation(JavaxLangModelContext context,
                                        AnnotationDescriptor descriptor,
                                        TypeElement configElement) {
        this.descriptor = descriptor;

        this.implementationName = JavaxAnnotationAttributeFactory.stringAttribute(
            value( descriptor, "implementationName" ),
            DEFAULTS.implementationName
        );
        this.implementationPackage = JavaxAnnotationAttributeFactory.stringAttribute(
            value( descriptor, "implementationPackage" ),
            DEFAULTS.implementationPackage
        );
        this.uses = JavaxAnnotationAttributeFactory.typeListAttribute(
            context,
            configElement,
            descriptor,
            value( descriptor, "uses" ),
            descriptor.hasValue( "uses" )
        );
        this.imports = JavaxAnnotationAttributeFactory.typeListAttribute(
            context,
            configElement,
            descriptor,
            value( descriptor, "imports" ),
            descriptor.hasValue( "imports" )
        );
        this.unmappedTargetPolicy = enumAttribute(
            value( descriptor, "unmappedTargetPolicy" ),
            DEFAULTS.unmappedTargetPolicy
        );
        this.unmappedSourcePolicy = enumAttribute(
            value( descriptor, "unmappedSourcePolicy" ),
            DEFAULTS.unmappedSourcePolicy
        );
        this.typeConversionPolicy = enumAttribute(
            value( descriptor, "typeConversionPolicy" ),
            DEFAULTS.typeConversionPolicy
        );
        this.componentModel = JavaxAnnotationAttributeFactory.stringAttribute(
            value( descriptor, "componentModel" ),
            DEFAULTS.componentModel
        );
        this.suppressTimestampInGenerated = JavaxAnnotationAttributeFactory.booleanAttribute(
            value( descriptor, "suppressTimestampInGenerated" ),
            DEFAULTS.suppressTimestampInGenerated
        );
        this.mappingInheritanceStrategy = enumAttribute(
            value( descriptor, "mappingInheritanceStrategy" ),
            DEFAULTS.mappingInheritanceStrategy
        );
        this.injectionStrategy = enumAttribute(
            value( descriptor, "injectionStrategy" ),
            DEFAULTS.injectionStrategy
        );
        this.disableSubMappingMethodsGeneration = JavaxAnnotationAttributeFactory.booleanAttribute(
            value( descriptor, "disableSubMappingMethodsGeneration" ),
            DEFAULTS.disableSubMappingMethodsGeneration
        );
        this.collectionMappingStrategy = enumAttribute(
            value( descriptor, "collectionMappingStrategy" ),
            DEFAULTS.collectionMappingStrategy
        );
        this.nullValueMappingStrategy = enumAttribute(
            value( descriptor, "nullValueMappingStrategy" ),
            DEFAULTS.nullValueMappingStrategy
        );
        this.nullValueIterableMappingStrategy = enumAttribute(
            value( descriptor, "nullValueIterableMappingStrategy" ),
            DEFAULTS.nullValueIterableMappingStrategy
        );
        this.nullValueMapMappingStrategy = enumAttribute(
            value( descriptor, "nullValueMapMappingStrategy" ),
            DEFAULTS.nullValueMapMappingStrategy
        );
        this.nullValuePropertyMappingStrategy = enumAttribute(
            value( descriptor, "nullValuePropertyMappingStrategy" ),
            DEFAULTS.nullValuePropertyMappingStrategy
        );
        this.nullValueCheckStrategy = enumAttribute(
            value( descriptor, "nullValueCheckStrategy" ),
            DEFAULTS.nullValueCheckStrategy
        );
        this.subclassExhaustiveStrategy = enumAttribute(
            value( descriptor, "subclassExhaustiveStrategy" ),
            DEFAULTS.subclassExhaustiveStrategy
        );
        this.subclassExhaustiveException = JavaxAnnotationAttributeFactory.typeAttribute(
            value( descriptor, "subclassExhaustiveException" ),
            toTypeDescriptor( DEFAULTS.subclassExhaustiveException, context )
        );
        this.builder = JavaxAnnotationAttributeFactory.builderAttribute(
            context,
            value( descriptor, "builder" )
        );
        this.mappingControl = JavaxAnnotationAttributeFactory.typeAttribute(
            value( descriptor, "mappingControl" ),
            toTypeDescriptor( DEFAULTS.mappingControl, context )
        );
        this.unexpectedValueMappingException = JavaxAnnotationAttributeFactory.typeAttribute(
            value( descriptor, "unexpectedValueMappingException" ),
            toTypeDescriptor( DEFAULTS.unexpectedValueMappingException, context )
        );
    }

    private MapperConfigAnnotationView toView() {
        return MapperConfigAnnotationView.create(
            descriptor,
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
            unexpectedValueMappingException
        );
    }

    static Optional<MapperConfigAnnotationView> from(JavaxLangModelContext context, TypeDescriptor configType) {
        if ( configType == null ) {
            return Optional.empty();
        }
        var configElementDescriptor = configType.typeElement().orElse( null );
        if ( configElementDescriptor == null ) {
            return Optional.empty();
        }
        AnnotationDescriptor descriptor = AnnotationDescriptorUtils.findAnnotation(
            context.langElements(),
            configElementDescriptor,
            MAPPER_CONFIG_FQN
        ).orElse( null );
        if ( descriptor == null ) {
            return Optional.empty();
        }
        TypeElement nativeElement = configElementDescriptor instanceof JavaxTypeElementDescriptor
            ? ( (JavaxTypeElementDescriptor) configElementDescriptor ).element()
            : null;
        if ( nativeElement == null ) {
            return Optional.empty();
        }
        JavaxMapperConfigAnnotation annotation =
            new JavaxMapperConfigAnnotation( context, descriptor, nativeElement );
        return Optional.of( annotation.toView() );
    }

    @Override
    public AnnotationAttribute<String> implementationName() {
        return implementationName;
    }

    @Override
    public AnnotationAttribute<String> implementationPackage() {
        return implementationPackage;
    }

    @Override
    public AnnotationAttribute<java.util.List<TypeDescriptor>> uses() {
        return uses;
    }

    @Override
    public AnnotationAttribute<java.util.List<TypeDescriptor>> imports() {
        return imports;
    }

    @Override
    public AnnotationAttribute<String> unmappedTargetPolicy() {
        return unmappedTargetPolicy;
    }

    @Override
    public AnnotationAttribute<String> unmappedSourcePolicy() {
        return unmappedSourcePolicy;
    }

    @Override
    public AnnotationAttribute<String> typeConversionPolicy() {
        return typeConversionPolicy;
    }

    @Override
    public AnnotationAttribute<String> componentModel() {
        return componentModel;
    }

    @Override
    public AnnotationAttribute<Boolean> suppressTimestampInGenerated() {
        return suppressTimestampInGenerated;
    }

    @Override
    public AnnotationAttribute<String> mappingInheritanceStrategy() {
        return mappingInheritanceStrategy;
    }

    @Override
    public AnnotationAttribute<String> injectionStrategy() {
        return injectionStrategy;
    }

    @Override
    public AnnotationAttribute<Boolean> disableSubMappingMethodsGeneration() {
        return disableSubMappingMethodsGeneration;
    }

    @Override
    public AnnotationAttribute<String> collectionMappingStrategy() {
        return collectionMappingStrategy;
    }

    private static AnnotationAttribute<String> enumAttribute(AnnotationValueDescriptor descriptor,
                                                             String defaultValue) {
        if ( descriptor == null ) {
            return AnnotationAttribute.absent( defaultValue );
        }
        String value = enumConstantName( descriptor );
        if ( value == null ) {
            return AnnotationAttribute.absent( defaultValue );
        }
        return AnnotationAttribute.present( value, defaultValue );
    }

    private static AnnotationValueDescriptor value(AnnotationDescriptor descriptor, String elementName) {
        if ( descriptor == null || !descriptor.hasValue( elementName ) ) {
            return null;
        }
        return AnnotationDescriptorUtils.getValue( descriptor, elementName );
    }

    private static String enumConstantName(AnnotationValueDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        String representation = AnnotationValueUtils.asString( descriptor );
        if ( representation == null ) {
            return null;
        }
        int lastDot = representation.lastIndexOf( '.' );
        if ( lastDot >= 0 && lastDot + 1 < representation.length() ) {
            return representation.substring( lastDot + 1 );
        }
        return representation;
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

    private static final class MapperConfigDefaults {
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
        final String nullValueMappingStrategy = "RETURN_NULL";
        final String nullValueIterableMappingStrategy = "RETURN_NULL";
        final String nullValueMapMappingStrategy = "RETURN_NULL";
        final String nullValuePropertyMappingStrategy = "SET_TO_NULL";
        final String nullValueCheckStrategy = "ON_IMPLICIT_CONVERSION";
        final String subclassExhaustiveStrategy = "COMPILE_ERROR";
        final Class<?> subclassExhaustiveException = IllegalArgumentException.class;
        final Class<?> mappingControl = null;
        final Class<?> unexpectedValueMappingException = IllegalArgumentException.class;
    }
}
