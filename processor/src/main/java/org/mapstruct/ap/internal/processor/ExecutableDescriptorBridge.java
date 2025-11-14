/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.processor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.mapstruct.ap.internal.gem.BeanMappingGem;
import org.mapstruct.ap.internal.gem.ConditionGem;
import org.mapstruct.ap.internal.gem.IgnoredGem;
import org.mapstruct.ap.internal.gem.IgnoredListGem;
import org.mapstruct.ap.internal.gem.IterableMappingGem;
import org.mapstruct.ap.internal.gem.MapMappingGem;
import org.mapstruct.ap.internal.gem.MappingGem;
import org.mapstruct.ap.internal.gem.MappingsGem;
import org.mapstruct.ap.internal.gem.SubclassMappingGem;
import org.mapstruct.ap.internal.gem.SubclassMappingsGem;
import org.mapstruct.ap.internal.gem.ValueMappingGem;
import org.mapstruct.ap.internal.gem.ValueMappingsGem;
import org.mapstruct.ap.internal.model.common.Parameter;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.model.source.BeanMappingOptions;
import org.mapstruct.ap.internal.model.source.ConditionOptions;
import org.mapstruct.ap.internal.model.source.EnumMappingOptions;
import org.mapstruct.ap.internal.model.source.IterableMappingOptions;
import org.mapstruct.ap.internal.model.source.MapMappingOptions;
import org.mapstruct.ap.internal.model.source.MapperOptions;
import org.mapstruct.ap.internal.model.source.MappingOptions;
import org.mapstruct.ap.internal.model.source.SubclassMappingOptions;
import org.mapstruct.ap.internal.model.source.SubclassValidator;
import org.mapstruct.ap.internal.model.source.ValueMappingOptions;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.langmodel.AnnotationGemFactory;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.util.RepeatableAnnotations;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.api.LangElements;

final class ExecutableDescriptorBridge {

    private ExecutableDescriptorBridge() {
    }

    static BeanMappingOptions beanMappingOptions(ExecutableDescriptor descriptor,
                                                 MapperOptions mapperOptions,
                                                 LangElements langElements,
                                                 AnnotationGemFactory annotationGems,
                                                 FormattingMessager messager,
                                                 TypeFactory typeFactory) {
        AnnotationDescriptor annotation = AnnotationDescriptorUtils.findAnnotation(
            langElements,
            descriptor,
            "org.mapstruct.BeanMapping"
        ).orElse( null );
        BeanMappingGem beanMapping = annotationGems.beanMapping( annotation );
        return BeanMappingOptions.getInstanceOn(
            beanMapping,
            mapperOptions,
            descriptor,
            messager,
            typeFactory
        );
    }

    static IterableMappingOptions iterableMappingOptions(ExecutableDescriptor descriptor,
                                                         MapperOptions mapperOptions,
                                                         LangElements langElements,
                                                         AnnotationGemFactory annotationGems,
                                                         FormattingMessager messager,
                                                         TypeFactory typeFactory) {
        AnnotationDescriptor annotation = AnnotationDescriptorUtils.findAnnotation(
            langElements,
            descriptor,
            "org.mapstruct.IterableMapping"
        ).orElse( null );
        IterableMappingGem iterableMapping = annotationGems.iterableMapping( annotation );
        return IterableMappingOptions.fromGem(
            iterableMapping,
            mapperOptions,
            descriptor,
            messager,
            typeFactory
        );
    }

    static MapMappingOptions mapMappingOptions(ExecutableDescriptor descriptor,
                                               MapperOptions mapperOptions,
                                               LangElements langElements,
                                               AnnotationGemFactory annotationGems,
                                               FormattingMessager messager,
                                               TypeFactory typeFactory) {
        AnnotationDescriptor annotation = AnnotationDescriptorUtils.findAnnotation(
            langElements,
            descriptor,
            "org.mapstruct.MapMapping"
        ).orElse( null );
        MapMappingGem mapMapping = annotationGems.mapMapping( annotation );
        return MapMappingOptions.fromGem(
            mapMapping,
            mapperOptions,
            descriptor,
            messager,
            typeFactory
        );
    }

    static EnumMappingOptions enumMappingOptions(ExecutableDescriptor descriptor,
                                                 MapperOptions mapperOptions,
                                                 LangElements langElements,
                                                 AnnotationGemFactory annotationGems,
                                                 FormattingMessager messager,
                                                 java.util.Map<String, org.mapstruct.ap.spi.EnumTransformationStrategy>
                                                     enumTransformationStrategies) {
        AnnotationDescriptor annotation = AnnotationDescriptorUtils.findAnnotation(
            langElements,
            descriptor,
            "org.mapstruct.EnumMapping"
        ).orElse( null );
        return EnumMappingOptions.getInstanceOn(
            annotation,
            mapperOptions,
            descriptor,
            enumTransformationStrategies,
            messager,
            annotationGems
        );
    }

    static Set<MappingOptions> mappingOptions(ExecutableDescriptor descriptor,
                                              BeanMappingOptions beanMappingOptions,
                                              LangElements langElements,
                                              AnnotationGemFactory annotationGems,
                                              FormattingMessager messager,
                                              TypeFactory typeFactory) {
        if ( descriptor == null ) {
            return Collections.emptySet();
        }
        Set<MappingOptions> processedAnnotations = new RepeatableMappings(
            langElements,
            beanMappingOptions,
            messager,
            typeFactory,
            annotationGems
        ).getProcessedAnnotations( descriptor );
        processedAnnotations.addAll( new IgnoredConditions(
            langElements,
            processedAnnotations,
            messager,
            annotationGems
        ).getProcessedAnnotations( descriptor ) );
        return processedAnnotations;
    }

    static Set<SubclassMappingOptions> subclassMappings(ExecutableDescriptor descriptor,
                                                        List<Parameter> sourceParameters,
                                                        Type resultType,
                                                        BeanMappingOptions beanMappingOptions,
                                                        TypeFactory typeFactory,
                                                        SubclassValidator validator,
                                                        LangElements langElements,
                                                        AnnotationGemFactory annotationGems,
                                                        FormattingMessager messager) {
        if ( descriptor == null ) {
            return Collections.emptySet();
        }
        return new RepeatableSubclassMappings(
            langElements,
            beanMappingOptions,
            sourceParameters,
            resultType,
            typeFactory,
            validator,
            messager,
            annotationGems
        ).getProcessedAnnotations( descriptor );
    }

    static Set<ConditionOptions> conditionOptions(ExecutableDescriptor descriptor,
                                                  List<Parameter> parameters,
                                                  LangElements langElements,
                                                  AnnotationGemFactory annotationGems,
                                                  FormattingMessager messager) {
        if ( descriptor == null ) {
            return Collections.emptySet();
        }
        return new MetaConditions( langElements, parameters, messager, annotationGems ).getProcessedAnnotations(
            descriptor
        );
    }

    static List<ValueMappingOptions> valueMappings(ExecutableDescriptor descriptor,
                                                   LangElements langElements,
                                                   AnnotationGemFactory annotationGems,
                                                   FormattingMessager messager) {
        if ( descriptor == null ) {
            return Collections.emptyList();
        }
        Set<ValueMappingOptions> mappings = new RepeatableValueMappings(
            langElements,
            messager,
            annotationGems
        ).getProcessedAnnotations( descriptor );
        return new java.util.ArrayList<>( mappings );
    }

    static boolean hasFactoryAnnotation(ExecutableDescriptor descriptor, LangElements langElements) {
        return AnnotationDescriptorUtils.findAnnotation(
            langElements,
            descriptor,
            "org.mapstruct.ObjectFactory"
        ).isPresent();
    }

    static boolean hasConditionAnnotation(ExecutableDescriptor descriptor, LangElements langElements) {
        return AnnotationDescriptorUtils.findAnnotation(
            langElements,
            descriptor,
            "org.mapstruct.Condition"
        ).isPresent();
    }

    static boolean isLifecycleCallbackMethod(ExecutableDescriptor descriptor, LangElements langElements) {
        return isBeforeMappingMethod( descriptor, langElements ) || isAfterMappingMethod( descriptor, langElements );
    }

    static boolean isAfterMappingMethod(ExecutableDescriptor descriptor, LangElements langElements) {
        return AnnotationDescriptorUtils.findAnnotation(
            langElements,
            descriptor,
            "org.mapstruct.AfterMapping"
        ).isPresent();
    }

    static boolean isBeforeMappingMethod(ExecutableDescriptor descriptor, LangElements langElements) {
        return AnnotationDescriptorUtils.findAnnotation(
            langElements,
            descriptor,
            "org.mapstruct.BeforeMapping"
        ).isPresent();
    }

    private static final class RepeatableMappings
        extends RepeatableAnnotations<MappingGem, MappingsGem, MappingOptions> {

        private final BeanMappingOptions beanMappingOptions;
        private final FormattingMessager messager;
        private final TypeFactory typeFactory;
        private final AnnotationGemFactory annotationGems;

        private RepeatableMappings(LangElements langElements,
                                   BeanMappingOptions beanMappingOptions,
                                   FormattingMessager messager,
                                   TypeFactory typeFactory,
                                   AnnotationGemFactory annotationGems) {
            super( langElements, "org.mapstruct.Mapping", "org.mapstruct.Mappings" );
            this.beanMappingOptions = beanMappingOptions;
            this.messager = messager;
            this.typeFactory = typeFactory;
            this.annotationGems = annotationGems;
        }

        @Override
        protected MappingGem singularInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
            return annotationGems.mapping( annotation );
        }

        @Override
        protected MappingsGem multipleInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
            return annotationGems.mappings( annotation );
        }

        @Override
        protected void addInstance(MappingGem mapping,
                                   AnnotationDescriptor annotation,
                                   ElementDescriptor source,
                                   Set<MappingOptions> mappings) {
            if ( !( source instanceof ExecutableDescriptor ) ) {
                return;
            }
            MappingOptions.addAnnotation(
                mapping,
                (ExecutableDescriptor) source,
                beanMappingOptions,
                messager,
                typeFactory,
                mappings
            );
        }

        @Override
        protected void addInstances(MappingsGem mappingsGem,
                                    AnnotationDescriptor annotation,
                                    ElementDescriptor source,
                                    Set<MappingOptions> mappings) {
            if ( !( source instanceof ExecutableDescriptor ) ) {
                return;
            }
            if ( mappingsGem != null && mappingsGem.value().hasValue() ) {
                for ( MappingGem mapping : mappingsGem.value().getValue() ) {
                    MappingOptions.addAnnotation(
                        mapping,
                        (ExecutableDescriptor) source,
                        beanMappingOptions,
                        messager,
                        typeFactory,
                        mappings
                    );
                }
            }
            else {
                MappingOptions.addAnnotations(
                    org.mapstruct.ap.internal.util.AnnotationDescriptorUtils.getAnnotationList( annotation, "value" ),
                    (ExecutableDescriptor) source,
                    beanMappingOptions,
                    messager,
                    typeFactory,
                    annotationGems,
                    mappings
                );
            }
        }
    }

    private static final class RepeatableSubclassMappings extends
        RepeatableAnnotations<SubclassMappingGem, SubclassMappingsGem, SubclassMappingOptions> {

        private final BeanMappingOptions beanMappingOptions;
        private final List<Parameter> sourceParameters;
        private final Type resultType;
        private final TypeFactory typeFactory;
        private final SubclassValidator validator;
        private final FormattingMessager messager;
        private final AnnotationGemFactory annotationGems;

        private RepeatableSubclassMappings(LangElements langElements,
                                           BeanMappingOptions beanMappingOptions,
                                           List<Parameter> sourceParameters,
                                           Type resultType,
                                           TypeFactory typeFactory,
                                           SubclassValidator validator,
                                           FormattingMessager messager,
                                           AnnotationGemFactory annotationGems) {
            super( langElements, "org.mapstruct.SubclassMapping", "org.mapstruct.SubclassMappings" );
            this.beanMappingOptions = beanMappingOptions;
            this.sourceParameters = sourceParameters;
            this.resultType = resultType;
            this.typeFactory = typeFactory;
            this.validator = validator;
            this.messager = messager;
            this.annotationGems = annotationGems;
        }

        @Override
        protected SubclassMappingGem singularInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
            return annotationGems.subclassMapping( annotation );
        }

        @Override
        protected SubclassMappingsGem multipleInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
            return annotationGems.subclassMappings( annotation );
        }

        @Override
        protected void addInstance(SubclassMappingGem gem,
                                   AnnotationDescriptor annotation,
                                   ElementDescriptor source,
                                   Set<SubclassMappingOptions> values) {
            if ( !( source instanceof ExecutableDescriptor ) ) {
                return;
            }
            SubclassMappingOptions options = SubclassMappingOptions.getInstanceOn(
                gem,
                annotation,
                beanMappingOptions,
                sourceParameters,
                resultType,
                (ExecutableDescriptor) source,
                validator,
                messager,
                typeFactory
            );
            if ( options != null ) {
                values.add( options );
            }
        }

        @Override
        protected void addInstances(SubclassMappingsGem gems,
                                    AnnotationDescriptor annotation,
                                    ElementDescriptor source,
                                    Set<SubclassMappingOptions> values) {
            if ( annotation == null || !( source instanceof ExecutableDescriptor ) ) {
                return;
            }
            for ( AnnotationDescriptor nested :
                org.mapstruct.ap.internal.util.AnnotationDescriptorUtils.getAnnotationList( annotation, "value" ) ) {
                SubclassMappingGem nestedGem = annotationGems.subclassMapping( nested );
                if ( nestedGem == null ) {
                    continue;
                }
                SubclassMappingOptions option = SubclassMappingOptions.getInstanceOn(
                    nestedGem,
                    nested,
                    beanMappingOptions,
                    sourceParameters,
                    resultType,
                    (ExecutableDescriptor) source,
                    validator,
                    messager,
                    typeFactory
                );
                if ( option != null ) {
                    values.add( option );
                }
            }
        }
    }

    private static final class RepeatableValueMappings
        extends RepeatableAnnotations<ValueMappingGem, ValueMappingsGem, ValueMappingOptions> {

        private final FormattingMessager messager;
        private boolean anyFound;
        private final AnnotationGemFactory annotationGems;

        private RepeatableValueMappings(LangElements langElements,
                                        FormattingMessager messager,
                                        AnnotationGemFactory annotationGems) {
            super( langElements, "org.mapstruct.ValueMapping", "org.mapstruct.ValueMappings" );
            this.messager = messager;
            this.anyFound = false;
            this.annotationGems = annotationGems;
        }

        @Override
        protected ValueMappingGem singularInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
            return annotationGems.valueMapping( annotation );
        }

        @Override
        protected ValueMappingsGem multipleInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
            return annotationGems.valueMappings( annotation );
        }

        @Override
        protected void addInstance(ValueMappingGem gem,
                                   AnnotationDescriptor annotation,
                                   ElementDescriptor source,
                                   Set<ValueMappingOptions> valueMappings) {
            if ( !( source instanceof ExecutableDescriptor ) ) {
                return;
            }
            ValueMappingOptions.collect(
                java.util.Collections.singletonList( annotation ),
                (ExecutableDescriptor) source,
                messager,
                valueMappings
            );
            updateAnyFlag( annotation, (ExecutableDescriptor) source );
        }

        @Override
        protected void addInstances(ValueMappingsGem gems,
                                    AnnotationDescriptor annotation,
                                    ElementDescriptor source,
                                    Set<ValueMappingOptions> valueMappings) {
            if ( !( source instanceof ExecutableDescriptor ) ) {
                return;
            }
            ValueMappingOptions.collect(
                org.mapstruct.ap.internal.util.AnnotationDescriptorUtils.getAnnotationList( annotation, "value" ),
                (ExecutableDescriptor) source,
                messager,
                valueMappings
            );
            updateAnyFlagFromSet( valueMappings );
        }

        private void updateAnyFlag(AnnotationDescriptor annotation, ExecutableDescriptor method) {
            ValueMappingOptions mapping = ValueMappingOptions.fromAnnotation( annotation );
            if ( mapping != null && mapping.isAnyMapping() ) {
                if ( anyFound ) {
                    messager.printMessage(
                        method,
                        mapping.getAnnotation(),
                        mapping.getTargetAnnotationValue(),
                        Message.VALUEMAPPING_ANY_AREADY_DEFINED,
                        mapping.getSource()
                    );
                }
                anyFound = true;
            }
        }

        private void updateAnyFlagFromSet(Set<ValueMappingOptions> valueMappings) {
            for ( ValueMappingOptions option : valueMappings ) {
                if ( option.isAnyMapping() ) {
                    anyFound = true;
                    return;
                }
            }
        }
    }

    private static final class MetaConditions
        extends RepeatableAnnotations<ConditionGem, ConditionGem, ConditionOptions> {

        private final List<Parameter> parameters;
        private final FormattingMessager messager;
        private final AnnotationGemFactory annotationGems;

        private MetaConditions(LangElements langElements,
                               List<Parameter> parameters,
                               FormattingMessager messager,
                               AnnotationGemFactory annotationGems) {
            super( langElements, "org.mapstruct.Condition", "org.mapstruct.Condition" );
            this.parameters = parameters;
            this.messager = messager;
            this.annotationGems = annotationGems;
        }

        @Override
        protected ConditionGem singularInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
            return annotationGems.condition( annotation );
        }

        @Override
        protected ConditionGem multipleInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
            return annotationGems.condition( annotation );
        }

        @Override
        protected void addInstance(ConditionGem gem,
                                   AnnotationDescriptor annotation,
                                   ElementDescriptor source,
                                   Set<ConditionOptions> values) {
            if ( !( source instanceof ExecutableDescriptor ) ) {
                return;
            }
            ConditionOptions options = ConditionOptions.getInstanceOn(
                gem,
                annotation,
                (ExecutableDescriptor) source,
                parameters,
                messager
            );
            if ( options != null ) {
                values.add( options );
            }
        }

        @Override
        protected void addInstances(ConditionGem gem,
                                    AnnotationDescriptor annotation,
                                    ElementDescriptor source,
                                    Set<ConditionOptions> values) {
            addInstance( gem, annotation, source, values );
        }
    }

    private static final class IgnoredConditions
        extends RepeatableAnnotations<IgnoredGem, IgnoredListGem, MappingOptions> {

        private final Set<MappingOptions> processedAnnotations;
        private final FormattingMessager messager;
        private final AnnotationGemFactory annotationGems;

        private IgnoredConditions(LangElements langElements,
                                  Set<MappingOptions> processedAnnotations,
                                  FormattingMessager messager,
                                  AnnotationGemFactory annotationGems) {
            super( langElements, "org.mapstruct.Ignored", "org.mapstruct.IgnoredList" );
            this.processedAnnotations = processedAnnotations;
            this.messager = messager;
            this.annotationGems = annotationGems;
        }

        @Override
        protected IgnoredGem singularInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
            return annotationGems.ignored( annotation );
        }

        @Override
        protected IgnoredListGem multipleInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
            return annotationGems.ignoredList( annotation );
        }

        @Override
        protected void addInstance(IgnoredGem gem,
                                   AnnotationDescriptor annotation,
                                   ElementDescriptor method,
                                   Set<MappingOptions> mappings) {
            if ( !( method instanceof ExecutableDescriptor ) ) {
                return;
            }
            IgnoredGem ignoredGem = gem != null ? gem : annotationGems.ignored( annotation );
            String prefix = ignoredGem.prefix().get();
            for ( String target : ignoredGem.targets().get() ) {
                String realTarget = target;
                if ( !prefix.isEmpty() ) {
                    realTarget = prefix + "." + target;
                }
                MappingOptions mappingOptions = MappingOptions.forIgnore( realTarget );
                if ( processedAnnotations.contains( mappingOptions ) || mappings.contains( mappingOptions ) ) {
                    messager.printMessage(
                        (ExecutableDescriptor) method,
                        Message.PROPERTYMAPPING_DUPLICATE_TARGETS,
                        realTarget
                    );
                }
                else {
                    mappings.add( mappingOptions );
                }
            }
        }

        @Override
        protected void addInstances(IgnoredListGem ignoredListGem,
                                    AnnotationDescriptor annotation,
                                    ElementDescriptor method,
                                    Set<MappingOptions> mappings) {
            for ( IgnoredGem ignored : ignoredListGem.value().getValue() ) {
                addInstance( ignored, annotation, method, mappings );
            }
        }
    }
}
