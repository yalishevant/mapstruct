/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSValueArgument;

import org.mapstruct.ap.internal.langmodel.AnnotationAttribute;
import org.mapstruct.ap.internal.langmodel.MapperAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperConfigAnnotation;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.gem.BuilderGem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for creating mapper annotation views from KSP annotations.
 */
final class KspMapperAnnotations {

    private static final String MAPPER_FQN = "org.mapstruct.Mapper";
    private static final String MAPPER_CONFIG_FQN = "org.mapstruct.MapperConfig";

    private KspMapperAnnotations() {
    }

    /**
     * Creates a mapper annotation view for the specified class declaration.
     *
     * @param context   KSP language model context
     * @param classDecl class declaration to inspect
     *
     * @return mapper annotation view or null if not a mapper
     */
    static MapperAnnotation mapper(KspLangModelContext context, KSClassDeclaration classDecl) {
        if ( classDecl == null ) {
            return null;
        }

        KSAnnotation mapperAnnotation = findAnnotation( classDecl, MAPPER_FQN );
        if ( mapperAnnotation == null ) {
            return null;
        }

        return new KspMapperAnnotation( context, mapperAnnotation );
    }

    /**
     * Creates a mapper config annotation view for the specified type descriptor.
     *
     * @param context    KSP language model context
     * @param configType type descriptor representing the config class
     *
     * @return optional mapper config annotation view
     */
    static Optional<MapperConfigAnnotation> mapperConfig(KspLangModelContext context, TypeDescriptor configType) {
        if ( configType == null ) {
            return Optional.empty();
        }

        Object unwrapped = configType.unwrap();
        KSClassDeclaration classDecl = null;

        if ( unwrapped instanceof KSType ) {
            KSDeclaration decl = ( (KSType) unwrapped ).getDeclaration();
            if ( decl instanceof KSClassDeclaration ) {
                classDecl = (KSClassDeclaration) decl;
            }
        }
        else if ( unwrapped instanceof KSClassDeclaration ) {
            classDecl = (KSClassDeclaration) unwrapped;
        }

        if ( classDecl == null ) {
            return Optional.empty();
        }

        KSAnnotation configAnnotation = findAnnotation( classDecl, MAPPER_CONFIG_FQN );
        if ( configAnnotation == null ) {
            return Optional.empty();
        }

        return Optional.of( new KspMapperConfigAnnotation( context, configAnnotation ) );
    }

    private static KSAnnotation findAnnotation(KSClassDeclaration classDecl, String annotationFqn) {
        for ( KSAnnotation annotation : KspSequenceUtils.toIterable( classDecl.getAnnotations() ) ) {
            KSType annotationType = annotation.getAnnotationType().resolve();
            KSDeclaration declaration = annotationType.getDeclaration();
            if ( declaration != null && declaration.getQualifiedName() != null ) {
                if ( annotationFqn.equals( declaration.getQualifiedName().asString() ) ) {
                    return annotation;
                }
            }
        }
        return null;
    }

    /**
     * KSP implementation of {@link MapperAnnotation}.
     */
    private static final class KspMapperAnnotation implements MapperAnnotation {

        private final KspLangModelContext context;
        private final KSAnnotation annotation;
        private final AnnotationDescriptor descriptor;

        KspMapperAnnotation(KspLangModelContext context, KSAnnotation annotation) {
            this.context = context;
            this.annotation = annotation;
            this.descriptor = context.descriptorFactory().annotationDescriptor( annotation );
        }

        @Override
        public AnnotationDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public boolean isValid() {
            return annotation != null;
        }

        @Override
        public AnnotationAttribute<String> implementationName() {
            return stringAttribute( "implementationName" );
        }

        @Override
        public AnnotationAttribute<String> implementationPackage() {
            return stringAttribute( "implementationPackage" );
        }

        @Override
        public AnnotationAttribute<List<TypeDescriptor>> uses() {
            return typeListAttribute( "uses" );
        }

        @Override
        public AnnotationAttribute<List<TypeDescriptor>> imports() {
            return typeListAttribute( "imports" );
        }

        @Override
        public AnnotationAttribute<String> unmappedTargetPolicy() {
            return enumAttribute( "unmappedTargetPolicy" );
        }

        @Override
        public AnnotationAttribute<String> unmappedSourcePolicy() {
            return enumAttribute( "unmappedSourcePolicy" );
        }

        @Override
        public AnnotationAttribute<String> typeConversionPolicy() {
            return enumAttribute( "typeConversionPolicy" );
        }

        @Override
        public AnnotationAttribute<String> componentModel() {
            return stringAttribute( "componentModel" );
        }

        @Override
        public AnnotationAttribute<Boolean> suppressTimestampInGenerated() {
            return booleanAttribute( "suppressTimestampInGenerated" );
        }

        @Override
        public AnnotationAttribute<String> mappingInheritanceStrategy() {
            return enumAttribute( "mappingInheritanceStrategy" );
        }

        @Override
        public AnnotationAttribute<String> injectionStrategy() {
            return enumAttribute( "injectionStrategy" );
        }

        @Override
        public AnnotationAttribute<Boolean> disableSubMappingMethodsGeneration() {
            return booleanAttribute( "disableSubMappingMethodsGeneration" );
        }

        @Override
        public AnnotationAttribute<String> collectionMappingStrategy() {
            return enumAttribute( "collectionMappingStrategy" );
        }

        @Override
        public AnnotationAttribute<String> nullValueCheckStrategy() {
            return enumAttribute( "nullValueCheckStrategy" );
        }

        @Override
        public AnnotationAttribute<String> nullValuePropertyMappingStrategy() {
            return enumAttribute( "nullValuePropertyMappingStrategy" );
        }

        @Override
        public AnnotationAttribute<String> nullValueMappingStrategy() {
            return enumAttribute( "nullValueMappingStrategy" );
        }

        @Override
        public AnnotationAttribute<String> subclassExhaustiveStrategy() {
            return enumAttribute( "subclassExhaustiveStrategy" );
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> subclassExhaustiveException() {
            return typeAttribute( "subclassExhaustiveException" );
        }

        @Override
        public AnnotationAttribute<String> nullValueIterableMappingStrategy() {
            return enumAttribute( "nullValueIterableMappingStrategy" );
        }

        @Override
        public AnnotationAttribute<String> nullValueMapMappingStrategy() {
            return enumAttribute( "nullValueMapMappingStrategy" );
        }

        @Override
        public AnnotationAttribute<BuilderGem> builder() {
            // Builder is a nested annotation - requires special handling
            return AnnotationAttribute.absent( null );
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> mappingControl() {
            return typeAttribute( "mappingControl" );
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> unexpectedValueMappingException() {
            return typeAttribute( "unexpectedValueMappingException" );
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> config() {
            return typeAttribute( "config" );
        }

        @Override
        public Optional<MapperConfigAnnotation> mapperConfig() {
            AnnotationAttribute<TypeDescriptor> configAttr = config();
            if ( !configAttr.hasValue() ) {
                return Optional.empty();
            }
            return KspMapperAnnotations.mapperConfig( context, configAttr.value().orElse( null ) );
        }

        private AnnotationAttribute<String> stringAttribute(String name) {
            KSValueArgument argument = findArgument( name );
            if ( argument == null || argument.getValue() == null ) {
                return AnnotationAttribute.absent( null );
            }
            Object value = argument.getValue();
            if ( value instanceof String ) {
                return AnnotationAttribute.present( (String) value, null );
            }
            return AnnotationAttribute.absent( null );
        }

        private AnnotationAttribute<Boolean> booleanAttribute(String name) {
            KSValueArgument argument = findArgument( name );
            if ( argument == null || argument.getValue() == null ) {
                return AnnotationAttribute.absent( null );
            }
            Object value = argument.getValue();
            if ( value instanceof Boolean ) {
                return AnnotationAttribute.present( (Boolean) value, null );
            }
            return AnnotationAttribute.absent( null );
        }

        private AnnotationAttribute<String> enumAttribute(String name) {
            KSValueArgument argument = findArgument( name );
            if ( argument == null || argument.getValue() == null ) {
                return AnnotationAttribute.absent( null );
            }
            Object value = argument.getValue();
            // In KSP, enum values are represented as KSType
            if ( value instanceof KSType ) {
                KSType enumType = (KSType) value;
                String enumName = enumType.getDeclaration().getSimpleName().asString();
                return AnnotationAttribute.present( enumName, null );
            }
            return AnnotationAttribute.absent( null );
        }

        private AnnotationAttribute<TypeDescriptor> typeAttribute(String name) {
            KSValueArgument argument = findArgument( name );
            if ( argument == null || argument.getValue() == null ) {
                return AnnotationAttribute.absent( null );
            }
            Object value = argument.getValue();
            if ( value instanceof KSType ) {
                TypeDescriptor descriptor = context.descriptorFactory().typeDescriptor( value );
                if ( descriptor != null ) {
                    return AnnotationAttribute.present( descriptor, null );
                }
            }
            return AnnotationAttribute.absent( null );
        }

        private AnnotationAttribute<List<TypeDescriptor>> typeListAttribute(String name) {
            KSValueArgument argument = findArgument( name );
            if ( argument == null || argument.getValue() == null ) {
                return AnnotationAttribute.absent( Collections.emptyList() );
            }
            Object value = argument.getValue();
            if ( value instanceof List<?> ) {
                List<?> list = (List<?>) value;
                if ( list.isEmpty() ) {
                    return AnnotationAttribute.present( Collections.emptyList(), Collections.emptyList() );
                }
                List<TypeDescriptor> descriptors = new ArrayList<>( list.size() );
                for ( Object item : list ) {
                    if ( item instanceof KSType ) {
                        TypeDescriptor descriptor = context.descriptorFactory().typeDescriptor( item );
                        if ( descriptor != null ) {
                            descriptors.add( descriptor );
                        }
                    }
                }
                return AnnotationAttribute.present( Collections.unmodifiableList( descriptors ), Collections.emptyList() );
            }
            return AnnotationAttribute.absent( Collections.emptyList() );
        }

        private KSValueArgument findArgument(String name) {
            for ( KSValueArgument argument : annotation.getArguments() ) {
                if ( argument.getName() != null && name.equals( argument.getName().asString() ) ) {
                    return argument;
                }
            }
            return null;
        }
    }

    /**
     * KSP implementation of {@link MapperConfigAnnotation}.
     */
    private static final class KspMapperConfigAnnotation implements MapperConfigAnnotation {

        private final KspLangModelContext context;
        private final KSAnnotation annotation;
        private final AnnotationDescriptor descriptor;

        KspMapperConfigAnnotation(KspLangModelContext context, KSAnnotation annotation) {
            this.context = context;
            this.annotation = annotation;
            this.descriptor = context.descriptorFactory().annotationDescriptor( annotation );
        }

        @Override
        public AnnotationDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public AnnotationAttribute<String> implementationName() {
            return stringAttribute( "implementationName" );
        }

        @Override
        public AnnotationAttribute<String> implementationPackage() {
            return stringAttribute( "implementationPackage" );
        }

        @Override
        public AnnotationAttribute<List<TypeDescriptor>> uses() {
            return typeListAttribute( "uses" );
        }

        @Override
        public AnnotationAttribute<List<TypeDescriptor>> imports() {
            return typeListAttribute( "imports" );
        }

        @Override
        public AnnotationAttribute<String> unmappedTargetPolicy() {
            return enumAttribute( "unmappedTargetPolicy" );
        }

        @Override
        public AnnotationAttribute<String> unmappedSourcePolicy() {
            return enumAttribute( "unmappedSourcePolicy" );
        }

        @Override
        public AnnotationAttribute<String> typeConversionPolicy() {
            return enumAttribute( "typeConversionPolicy" );
        }

        @Override
        public AnnotationAttribute<String> componentModel() {
            return stringAttribute( "componentModel" );
        }

        @Override
        public AnnotationAttribute<Boolean> suppressTimestampInGenerated() {
            return booleanAttribute( "suppressTimestampInGenerated" );
        }

        @Override
        public AnnotationAttribute<String> mappingInheritanceStrategy() {
            return enumAttribute( "mappingInheritanceStrategy" );
        }

        @Override
        public AnnotationAttribute<String> injectionStrategy() {
            return enumAttribute( "injectionStrategy" );
        }

        @Override
        public AnnotationAttribute<Boolean> disableSubMappingMethodsGeneration() {
            return booleanAttribute( "disableSubMappingMethodsGeneration" );
        }

        @Override
        public AnnotationAttribute<String> collectionMappingStrategy() {
            return enumAttribute( "collectionMappingStrategy" );
        }

        @Override
        public AnnotationAttribute<String> nullValueCheckStrategy() {
            return enumAttribute( "nullValueCheckStrategy" );
        }

        @Override
        public AnnotationAttribute<String> nullValuePropertyMappingStrategy() {
            return enumAttribute( "nullValuePropertyMappingStrategy" );
        }

        @Override
        public AnnotationAttribute<String> nullValueMappingStrategy() {
            return enumAttribute( "nullValueMappingStrategy" );
        }

        @Override
        public AnnotationAttribute<String> subclassExhaustiveStrategy() {
            return enumAttribute( "subclassExhaustiveStrategy" );
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> subclassExhaustiveException() {
            return typeAttribute( "subclassExhaustiveException" );
        }

        @Override
        public AnnotationAttribute<String> nullValueIterableMappingStrategy() {
            return enumAttribute( "nullValueIterableMappingStrategy" );
        }

        @Override
        public AnnotationAttribute<String> nullValueMapMappingStrategy() {
            return enumAttribute( "nullValueMapMappingStrategy" );
        }

        @Override
        public AnnotationAttribute<BuilderGem> builder() {
            // Builder is a nested annotation - requires special handling
            return AnnotationAttribute.absent( null );
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> mappingControl() {
            return typeAttribute( "mappingControl" );
        }

        @Override
        public AnnotationAttribute<TypeDescriptor> unexpectedValueMappingException() {
            return typeAttribute( "unexpectedValueMappingException" );
        }

        private AnnotationAttribute<String> stringAttribute(String name) {
            KSValueArgument argument = findArgument( name );
            if ( argument == null || argument.getValue() == null ) {
                return AnnotationAttribute.absent( null );
            }
            Object value = argument.getValue();
            if ( value instanceof String ) {
                return AnnotationAttribute.present( (String) value, null );
            }
            return AnnotationAttribute.absent( null );
        }

        private AnnotationAttribute<Boolean> booleanAttribute(String name) {
            KSValueArgument argument = findArgument( name );
            if ( argument == null || argument.getValue() == null ) {
                return AnnotationAttribute.absent( null );
            }
            Object value = argument.getValue();
            if ( value instanceof Boolean ) {
                return AnnotationAttribute.present( (Boolean) value, null );
            }
            return AnnotationAttribute.absent( null );
        }

        private AnnotationAttribute<String> enumAttribute(String name) {
            KSValueArgument argument = findArgument( name );
            if ( argument == null || argument.getValue() == null ) {
                return AnnotationAttribute.absent( null );
            }
            Object value = argument.getValue();
            if ( value instanceof KSType ) {
                KSType enumType = (KSType) value;
                String enumName = enumType.getDeclaration().getSimpleName().asString();
                return AnnotationAttribute.present( enumName, null );
            }
            return AnnotationAttribute.absent( null );
        }

        private AnnotationAttribute<TypeDescriptor> typeAttribute(String name) {
            KSValueArgument argument = findArgument( name );
            if ( argument == null || argument.getValue() == null ) {
                return AnnotationAttribute.absent( null );
            }
            Object value = argument.getValue();
            if ( value instanceof KSType ) {
                TypeDescriptor descriptor = context.descriptorFactory().typeDescriptor( value );
                if ( descriptor != null ) {
                    return AnnotationAttribute.present( descriptor, null );
                }
            }
            return AnnotationAttribute.absent( null );
        }

        private AnnotationAttribute<List<TypeDescriptor>> typeListAttribute(String name) {
            KSValueArgument argument = findArgument( name );
            if ( argument == null || argument.getValue() == null ) {
                return AnnotationAttribute.absent( Collections.emptyList() );
            }
            Object value = argument.getValue();
            if ( value instanceof List<?> ) {
                List<?> list = (List<?>) value;
                if ( list.isEmpty() ) {
                    return AnnotationAttribute.present( Collections.emptyList(), Collections.emptyList() );
                }
                List<TypeDescriptor> descriptors = new ArrayList<>( list.size() );
                for ( Object item : list ) {
                    if ( item instanceof KSType ) {
                        TypeDescriptor descriptor = context.descriptorFactory().typeDescriptor( item );
                        if ( descriptor != null ) {
                            descriptors.add( descriptor );
                        }
                    }
                }
                return AnnotationAttribute.present( Collections.unmodifiableList( descriptors ), Collections.emptyList() );
            }
            return AnnotationAttribute.absent( Collections.emptyList() );
        }

        private KSValueArgument findArgument(String name) {
            for ( KSValueArgument argument : annotation.getArguments() ) {
                if ( argument.getName() != null && name.equals( argument.getName().asString() ) ) {
                    return argument;
                }
            }
            return null;
        }
    }
}
