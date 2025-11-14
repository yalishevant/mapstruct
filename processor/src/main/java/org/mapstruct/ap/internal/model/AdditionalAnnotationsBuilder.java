/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.gem.AnnotateWithGem;
import org.mapstruct.ap.internal.gem.AnnotateWithsGem;
import org.mapstruct.ap.internal.gem.ElementGem;
import org.mapstruct.ap.internal.model.annotation.AnnotationElement;
import org.mapstruct.ap.internal.model.annotation.AnnotationElement.AnnotationElementType;
import org.mapstruct.ap.internal.model.annotation.EnumAnnotationElementHolder;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.util.RepeatableAnnotations;
import org.mapstruct.ap.internal.util.Strings;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.AnnotationGemFactory;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;
import org.mapstruct.tools.gem.GemValue;

/**
 * @author Ben Zegveld
 * @since 1.5
 */
public class AdditionalAnnotationsBuilder
    extends RepeatableAnnotations<AnnotateWithGem, AnnotateWithsGem, Annotation> {
    private static final String ANNOTATE_WITH_FQN = "org.mapstruct.AnnotateWith";
    private static final String ANNOTATE_WITHS_FQN = "org.mapstruct.AnnotateWiths";
    private TypeFactory typeFactory;
    private FormattingMessager messager;
    private final LangElements langElements;
    private final LangDescriptorFactory descriptorFactory;
    private final AnnotationGemFactory annotationGems;

    public AdditionalAnnotationsBuilder(TypeFactory typeFactory,
                                        FormattingMessager messager) {
        super( typeFactory.langElements(), ANNOTATE_WITH_FQN, ANNOTATE_WITHS_FQN );
        this.typeFactory = typeFactory;
        this.messager = messager;
        this.langElements = typeFactory.langElements();
        this.descriptorFactory = typeFactory.getDescriptorFactory();
        this.annotationGems = typeFactory.annotationGems();
    }

    @Override
    protected AnnotateWithGem singularInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
        return annotationGems.annotateWith( annotation );
    }

    @Override
    protected AnnotateWithsGem multipleInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation) {
        return annotationGems.annotateWiths( annotation );
    }

    @Override
    protected void addInstance(AnnotateWithGem gem,
                               AnnotationDescriptor annotation,
                               ElementDescriptor source,
                               Set<Annotation> mappings) {
        if ( gem == null ) {
            return;
        }
        AnnotationDescriptor reportingAnnotation = descriptorFor( gem );
        buildAnnotation( gem, source )
            .ifPresent( t -> addAndValidateMapping(
                mappings,
                source,
                reportingAnnotation != null ? reportingAnnotation : annotation,
                t
            ) );
    }

    @Override
    protected void addInstances(AnnotateWithsGem gem,
                                AnnotationDescriptor annotation,
                                ElementDescriptor source,
                                Set<Annotation> mappings) {
        if ( gem == null ) {
            return;
        }
        for ( AnnotateWithGem annotateWithGem : gem.value().get() ) {
            AnnotationDescriptor nestedDescriptor = descriptorFor( annotateWithGem );
            buildAnnotation( annotateWithGem, source )
                .ifPresent( t -> addAndValidateMapping(
                    mappings,
                    source,
                    nestedDescriptor != null ? nestedDescriptor : annotation,
                    t
                ) );
        }
    }

    @Override
    public Set<Annotation> getProcessedAnnotations(ElementDescriptor source) {
        Set<Annotation> processedAnnotations = super.getProcessedAnnotations( source );
        return addDeprecatedAnnotation( source, processedAnnotations );
    }

    private Set<Annotation> addDeprecatedAnnotation(ElementDescriptor source, Set<Annotation> annotations) {
        AnnotationDescriptor deprecatedAnnotation = langElements.annotationMirrors( source ).stream()
            .filter( annotation -> AnnotationDescriptorUtils.hasQualifiedName(
                annotation,
                Deprecated.class.getName()
            ) )
            .findFirst()
            .orElse( null );
        if ( deprecatedAnnotation == null ) {
            return annotations;
        }
        Type deprecatedType = typeFactory.getType( Deprecated.class );
        if ( annotations.stream().anyMatch( annotation -> annotation.getType().equals( deprecatedType ) ) ) {
            messager.printMessage(
                source,
                deprecatedAnnotation,
                Message.ANNOTATE_WITH_DUPLICATE,
                deprecatedType.describe()
            );
            return annotations;
        }
        List<AnnotationElement> annotationElements = new ArrayList<>();
        AnnotationValueDescriptor sinceDescriptor = deprecatedAnnotation.elementValues().get( "since" );
        String sinceValue = AnnotationValueUtils.asString( sinceDescriptor );
        if ( !Strings.isEmpty( sinceValue ) ) {
            annotationElements.add( new AnnotationElement(
                AnnotationElementType.STRING,
                "since",
                Collections.singletonList( sinceValue )
            ) );
        }
        AnnotationValueDescriptor forRemovalDescriptor = deprecatedAnnotation.elementValues().get( "forRemoval" );
        if ( AnnotationValueUtils.asBoolean( forRemovalDescriptor, false ) ) {
            annotationElements.add( new AnnotationElement(
                AnnotationElementType.BOOLEAN,
                "forRemoval",
                Collections.singletonList( Boolean.TRUE )
            ) );
        }
        annotations.add( new Annotation( deprecatedType, annotationElements ) );
        return annotations;
    }

    private void addAndValidateMapping(Set<Annotation> mappings,
                                       ElementDescriptor source,
                                       AnnotationDescriptor reportingAnnotation,
                                       Annotation anno) {
        TypeElementDescriptor annotationTypeDescriptor = anno.getType().getTypeElementDescriptor();
        boolean repeatable = annotationTypeDescriptor != null && isRepeatable( annotationTypeDescriptor );

        if ( !repeatable && mappings.stream().anyMatch( existing -> existing.getType().equals( anno.getType() ) ) ) {
            messager.printMessage(
                source,
                reportingAnnotation,
                Message.ANNOTATE_WITH_ANNOTATION_IS_NOT_REPEATABLE,
                anno.getType().describe()
            );
            return;
        }
        if ( mappings.stream().anyMatch( existing -> existing.getType().equals( anno.getType() )
            && existing.getProperties().equals( anno.getProperties() ) ) ) {
            messager.printMessage(
                source,
                reportingAnnotation,
                Message.ANNOTATE_WITH_DUPLICATE,
                anno.getType().describe()
            );
            return;
        }
        mappings.add( anno );
    }

    private boolean isRepeatable(TypeElementDescriptor annotationTypeDescriptor) {
        return langElements.annotationMirrors( annotationTypeDescriptor ).stream()
            .anyMatch( annotation -> AnnotationDescriptorUtils.hasQualifiedName(
                annotation,
                Repeatable.class.getCanonicalName()
            ) );
    }

    private Optional<Annotation> buildAnnotation(AnnotateWithGem annotationGem, ElementDescriptor element) {
        if ( annotationGem == null ) {
            return Optional.empty();
        }
        GemValue<?> annotationTypeValue = annotationGem.value();
        if ( annotationTypeValue == null || !annotationTypeValue.hasValue() ) {
            return Optional.empty();
        }
        Type annotationType = typeFactory.getType( getTypeDescriptor( annotationGem.value() ) );
        List<ElementGem> eleGems = annotationGem.elements().get();
        AnnotationDescriptor annotationDescriptor = descriptorFor( annotationGem );
        if ( isValid( annotationType, eleGems, element, annotationDescriptor ) ) {
            return Optional.of( new Annotation( annotationType, convertToProperties( eleGems ) ) );
        }
        return Optional.empty();
    }

    private boolean isValid(Type annotationType, List<ElementGem> eleGems,
                            ElementDescriptor element, AnnotationDescriptor annotation) {
        return isValidInternal( annotationType, eleGems, element, annotation );
    }

    private List<AnnotationElement> convertToProperties(List<ElementGem> eleGems) {
        return eleGems.stream().map( gem -> convertToProperty( gem, typeFactory ) ).collect( Collectors.toList() );
    }

    private enum ConvertToProperty {
        BOOLEAN(
            AnnotationElementType.BOOLEAN,
            (eleGem, typeFactory) -> eleGem.booleans().get(),
            eleGem -> eleGem.booleans().hasValue()
        ),
        BYTE(
            AnnotationElementType.BYTE,
            (eleGem, typeFactory) -> eleGem.bytes().get(),
            eleGem -> eleGem.bytes().hasValue()
        ),
        CHARACTER(
            AnnotationElementType.CHARACTER,
            (eleGem, typeFactory) -> eleGem.chars().get(),
            eleGem -> eleGem.chars().hasValue()
        ),
        CLASSES(
            AnnotationElementType.CLASS,
            (eleGem, typeFactory) -> {
                List<?> classHandles = eleGem.classes().get();
                if ( classHandles == null || classHandles.isEmpty() ) {
                    return Collections.emptyList();
                }
                LangDescriptorFactory descriptorFactory = typeFactory.getDescriptorFactory();
                return classHandles.stream()
                    .filter( Objects::nonNull )
                    .map( descriptorFactory::typeDescriptor )
                    .map( typeFactory::getType )
                    .collect( Collectors.toList() );
            },
            eleGem -> eleGem.classes().hasValue()
        ),
        DOUBLE(
            AnnotationElementType.DOUBLE,
            (eleGem, typeFactory) -> eleGem.doubles().get(),
            eleGem -> eleGem.doubles().hasValue()
        ),
        ENUM(
            AnnotationElementType.ENUM,
            (eleGem, typeFactory) -> {
                List<EnumAnnotationElementHolder> values = new ArrayList<>();
                Object enumClassHandle = eleGem.enumClass().get();
                if ( enumClassHandle == null ) {
                    return values;
                }
                Type enumType = typeFactory.getType(
                    typeFactory.getDescriptorFactory().typeDescriptor( enumClassHandle )
                );
                for ( String enumName : eleGem.enums().get() ) {
                    values.add( new EnumAnnotationElementHolder( enumType, enumName ) );
                }
                return values;
            },
            eleGem -> eleGem.enums().hasValue() && eleGem.enumClass().hasValue()
        ),
        FLOAT(
            AnnotationElementType.FLOAT,
            (eleGem, typeFactory) -> eleGem.floats().get(),
            eleGem -> eleGem.floats().hasValue()
        ),
        INT(
            AnnotationElementType.INTEGER,
            (eleGem, typeFactory) -> eleGem.ints().get(),
            eleGem -> eleGem.ints().hasValue()
        ),
        LONG(
            AnnotationElementType.LONG,
            (eleGem, typeFactory) -> eleGem.longs().get(),
            eleGem -> eleGem.longs().hasValue()
        ),
        SHORT(
            AnnotationElementType.SHORT,
            (eleGem, typeFactory) -> eleGem.shorts().get(),
            eleGem -> eleGem.shorts().hasValue()
        ),
        STRING(
            AnnotationElementType.STRING,
            (eleGem, typeFactory) -> eleGem.strings().get(),
            eleGem -> eleGem.strings().hasValue()
        );

        private final AnnotationElementType type;
        private final BiFunction<ElementGem, TypeFactory, List<? extends Object>> factory;
        private final Predicate<ElementGem> usabilityChecker;

        ConvertToProperty(AnnotationElementType type,
                          BiFunction<ElementGem, TypeFactory, List<? extends Object>> factory,
                          Predicate<ElementGem> usabilityChecker) {
            this.type = type;
            this.factory = factory;
            this.usabilityChecker = usabilityChecker;
        }

        AnnotationElement toProperty(ElementGem eleGem, TypeFactory typeFactory) {
            return new AnnotationElement(
                type,
                eleGem.name().get(),
                factory.apply( eleGem, typeFactory )
            );
        }

        boolean isUsable(ElementGem eleGem) {
            return usabilityChecker.test( eleGem );
        }
    }

    private AnnotationElement convertToProperty(ElementGem eleGem, TypeFactory typeFactory) {
        for ( ConvertToProperty convertToJava : ConvertToProperty.values() ) {
            if ( convertToJava.isUsable( eleGem ) ) {
                return convertToJava.toProperty( eleGem, typeFactory );
            }
        }
        return null;
    }

    private boolean isValidInternal(Type annotationType,
                                    List<ElementGem> eleGems,
                                    ElementDescriptor element,
                                    AnnotationDescriptor annotationDescriptor) {
        boolean isValid = true;
        if ( !annotationIsAllowed( annotationType, element, annotationDescriptor ) ) {
            isValid = false;
        }

        TypeElementDescriptor annotationTypeDescriptor = annotationType.getTypeElementDescriptor();
        if ( annotationTypeDescriptor == null ) {
            return false;
        }

        List<ExecutableDescriptor> annotationElements = annotationMethods( annotationTypeDescriptor );
        if ( !allRequiredElementsArePresent(
            annotationType,
            annotationElements,
            eleGems,
            element,
            annotationDescriptor
        ) ) {
            isValid = false;
        }
        if ( !allElementsAreKnownInAnnotation( annotationType, annotationElements, eleGems, element ) ) {
            isValid = false;
        }
        if ( !allElementsAreOfCorrectType( annotationType, annotationElements, eleGems, element ) ) {
            isValid = false;
        }
        if ( !enumConstructionIsCorrectlyUsed( eleGems, element ) ) {
            isValid = false;
        }
        if ( !allElementsAreUnique( eleGems, element ) ) {
            isValid = false;
        }
        return isValid;
    }

    private List<ExecutableDescriptor> annotationMethods(TypeElementDescriptor annotationTypeDescriptor) {
        if ( annotationTypeDescriptor == null ) {
            return Collections.emptyList();
        }
        String annotationTypeId = annotationTypeDescriptor.id();
        return langElements.enclosedExecutables( annotationTypeDescriptor ).stream()
            .filter( descriptor -> descriptor.kind() == LangElementKind.METHOD )
            .filter( descriptor -> descriptor.enclosingElement()
                .map( ElementDescriptor::id )
                .map( annotationTypeId::equals )
                .orElse( false ) )
            .collect( Collectors.toList() );
    }

    private boolean allElementsAreUnique(List<ElementGem> eleGems, ElementDescriptor element) {
        boolean isValid = true;
        List<String> checkedElements = new ArrayList<>();
        for ( ElementGem elementGem : eleGems ) {
            String elementName = elementGem.name().get();
            if ( checkedElements.contains( elementName ) ) {
                isValid = false;
                messager
                        .printMessage(
                            element,
                            descriptorFor( elementGem ),
                            Message.ANNOTATE_WITH_DUPLICATE_PARAMETER,
                            elementName );
            }
            else {
                checkedElements.add( elementName );
            }
        }
        return isValid;
    }

    private boolean enumConstructionIsCorrectlyUsed(List<ElementGem> eleGems, ElementDescriptor element) {
        boolean isValid = true;
        for ( ElementGem elementGem : eleGems ) {
            if ( elementGem.enums().hasValue() ) {
                if ( elementGem.enumClass().getValue() == null ) {
                    isValid = false;
                    messager
                            .printMessage(
                                element,
                                descriptorFor( elementGem ),
                                Message.ANNOTATE_WITH_ENUM_CLASS_NOT_DEFINED );
                }
                else {
                    Type type = typeFactory.getType( getTypeDescriptor( elementGem.enumClass() ) );
                    if ( type.isEnumType() ) {
                        List<String> enumConstants = type.getEnumConstants();
                        for ( String enumName : elementGem.enums().get() ) {
                            if ( !enumConstants.contains( enumName ) ) {
                                isValid = false;
                                messager
                                        .printMessage(
                                            element,
                                            descriptorFor( elementGem ),
                                            valueDescriptor( elementGem.enums() ),
                                            Message.ANNOTATE_WITH_ENUM_VALUE_DOES_NOT_EXIST,
                                            type.describe(),
                                            enumName );
                            }
                        }
                    }
                }
            }
            else if ( elementGem.enumClass().getValue() != null ) {
                isValid = false;
                messager.printMessage(
                    element,
                    descriptorFor( elementGem ),
                    Message.ANNOTATE_WITH_ENUMS_NOT_DEFINED
                );
            }
        }
        return isValid;
    }

    private boolean annotationIsAllowed(Type annotationType,
                                        ElementDescriptor element,
                                        AnnotationDescriptor annotationDescriptor) {
        TypeElementDescriptor annotationTypeDescriptor = annotationType.getTypeElementDescriptor();
        if ( annotationTypeDescriptor == null ) {
            return true;
        }

        AnnotationDescriptor targetDescriptor = langElements.annotationMirrors( annotationTypeDescriptor ).stream()
            .filter( descriptor -> AnnotationDescriptorUtils.hasQualifiedName(
                descriptor,
                Target.class.getCanonicalName()
            ) )
            .findFirst()
            .orElse( null );
        if ( targetDescriptor == null ) {
            return true;
        }

        Set<ElementType> annotationTargets = AnnotationDescriptorUtils.getValueList( targetDescriptor, "value" )
            .stream()
            .map( value -> AnnotationValueUtils.asEnum( value, ElementType.class ) )
            .filter( Objects::nonNull )
            .collect( Collectors.toCollection( () -> EnumSet.noneOf( ElementType.class ) ) );

        boolean isValid = true;
        if ( isTypeTarget( element ) && !annotationTargets.contains( ElementType.TYPE ) ) {
            isValid = false;
            messager.printMessage(
                element,
                annotationDescriptor,
                Message.ANNOTATE_WITH_NOT_ALLOWED_ON_CLASS,
                annotationType.describe()
            );
        }
        if ( isMethodTarget( element ) && !annotationTargets.contains( ElementType.METHOD ) ) {
            isValid = false;
            messager.printMessage(
                element,
                annotationDescriptor,
                Message.ANNOTATE_WITH_NOT_ALLOWED_ON_METHODS,
                annotationType.describe()
            );
        }
        return isValid;
    }

    private boolean isTypeTarget(ElementDescriptor element) {
        if ( element == null ) {
            return false;
        }
        LangElementKind kind = element.kind();
        return kind == LangElementKind.CLASS
            || kind == LangElementKind.INTERFACE
            || kind == LangElementKind.ENUM
            || kind == LangElementKind.RECORD
            || kind == LangElementKind.ANNOTATION_TYPE;
    }

    private boolean isMethodTarget(ElementDescriptor element) {
        return element != null && element.kind() == LangElementKind.METHOD;
    }

    private boolean allElementsAreKnownInAnnotation(Type annotationType,
                                                    List<ExecutableDescriptor> annotationParameters,
                                                    List<ElementGem> eleGems,
                                                    ElementDescriptor element) {
        Set<String> allowedAnnotationParameters = annotationParameters.stream()
            .map( descriptor -> descriptor.simpleName().content() )
            .collect( Collectors.toSet() );
        boolean isValid = true;
        for ( ElementGem eleGem : eleGems ) {
            if ( eleGem.name().isValid()
                && !allowedAnnotationParameters.contains( eleGem.name().get() ) ) {
                isValid = false;
                messager
                        .printMessage(
                            element,
                            descriptorFor( eleGem ),
                            valueDescriptor( eleGem.name() ),
                            Message.ANNOTATE_WITH_UNKNOWN_PARAMETER,
                            eleGem.name().get(),
                            annotationType.describe(),
                            Strings.getMostSimilarWord( eleGem.name().get(), allowedAnnotationParameters )
                        );
            }
        }
        return isValid;
    }

    private boolean allRequiredElementsArePresent(Type annotationType,
                                                  List<ExecutableDescriptor> annotationParameters,
                                                  List<ElementGem> elements,
                                                  ElementDescriptor element,
                                                  AnnotationDescriptor annotationDescriptor) {
        boolean valid = true;
        for ( ExecutableDescriptor annotationParameter : annotationParameters ) {
            if ( annotationParameter.defaultValue() == null ) {
                // Mandatory parameter, must be present in the elements
                String parameterName = annotationParameter.simpleName().content();
                boolean elementGemDefined = false;
                for ( ElementGem elementGem : elements ) {
                    if ( elementGem.isValid() && elementGem.name().get().equals( parameterName ) ) {
                        elementGemDefined = true;
                        break;
                    }
                }

                if ( !elementGemDefined ) {
                    valid = false;
                    messager
                        .printMessage(
                            element,
                            annotationDescriptor,
                            Message.ANNOTATE_WITH_MISSING_REQUIRED_PARAMETER,
                            parameterName,
                            annotationType.describe()
                        );
                }
            }
        }
        return valid;
    }

    private boolean allElementsAreOfCorrectType(Type annotationType,
                                                List<ExecutableDescriptor> annotationParameters,
                                                List<ElementGem> elements,
                                                ElementDescriptor element) {
        Map<String, ExecutableDescriptor> annotationParametersByName =
            annotationParameters.stream()
                .collect( Collectors.toMap(
                    descriptor -> descriptor.simpleName().content(),
                    Function.identity()
                ) );
        boolean isValid = true;
        for ( ElementGem eleGem : elements ) {
            Type annotationParameterType = getAnnotationParameterType( annotationParametersByName, eleGem );
            Type annotationParameterTypeSingular = getNonArrayType( annotationParameterType );
            if ( annotationParameterTypeSingular == null ) {
                continue;
            }
            if ( hasTooManyDifferentTypes( eleGem ) ) {
                isValid = false;
                messager.printMessage(
                    element,
                    descriptorFor( eleGem ),
                    valueDescriptor( eleGem.name() ),
                    Message.ANNOTATE_WITH_TOO_MANY_VALUE_TYPES,
                    eleGem.name().get(),
                    annotationParameterType.describe(),
                    annotationType.describe()
                );
            }
            else {
                Map<Type, Integer> elementTypes = getParameterTypes( eleGem );
                Set<ElementGem> reportedSizeError = new HashSet<>();
                for ( Type eleGemType : elementTypes.keySet() ) {
                    if ( !sameTypeOrAssignableClass( annotationParameterTypeSingular, eleGemType ) ) {
                        isValid = false;
                        messager.printMessage(
                            element,
                            descriptorFor( eleGem ),
                            valueDescriptor( eleGem.name() ),
                            Message.ANNOTATE_WITH_WRONG_PARAMETER,
                            eleGem.name().get(),
                            eleGemType.describe(),
                            annotationParameterType.describe(),
                            annotationType.describe()
                        );
                    }
                    else if ( !annotationParameterType.isArrayType()
                        && elementTypes.get( eleGemType ) > 1
                        && !reportedSizeError.contains( eleGem ) ) {
                        isValid = false;
                        messager.printMessage(
                            element,
                            descriptorFor( eleGem ),
                            Message.ANNOTATE_WITH_PARAMETER_ARRAY_NOT_EXPECTED,
                            eleGem.name().get(),
                            annotationType.describe()
                        );
                        reportedSizeError.add( eleGem );
                    }
                }
            }
        }
        return isValid;
    }

    private boolean hasTooManyDifferentTypes( ElementGem eleGem ) {
        return Arrays.stream( ConvertToProperty.values() )
                     .filter( anotationElement -> anotationElement.isUsable( eleGem ) )
                     .count() > 1;
    }

    private AnnotationDescriptor descriptorFor(AnnotateWithGem gem) {
        return gem == null ? null : descriptorFactory.annotationDescriptor( gem.mirror() );
    }

    private AnnotationDescriptor descriptorFor(ElementGem gem) {
        return gem == null ? null : descriptorFactory.annotationDescriptor( gem.mirror() );
    }

    private AnnotationValueDescriptor valueDescriptor(GemValue<?> gemValue) {
        return gemValue == null ? null : descriptorFactory.annotationValueDescriptor( gemValue.getAnnotationValue() );
    }

    private Type getNonArrayType(Type annotationParameterType) {
        if ( annotationParameterType == null ) {
            return null;
        }
        if ( annotationParameterType.isArrayType() ) {
            return annotationParameterType.getComponentType();
        }

        return annotationParameterType;
    }

    private boolean sameTypeOrAssignableClass(Type annotationParameterType, Type eleGemType) {
        return annotationParameterType.equals( eleGemType )
            || eleGemType.isAssignableTo( getTypeBound( annotationParameterType ) );
    }

    private Type getTypeBound(Type annotationParameterType) {
        List<Type> typeParameters = annotationParameterType.getTypeParameters();
        if ( typeParameters.size() != 1 ) {
            return annotationParameterType;
        }
        return typeParameters.get( 0 ).getTypeBound();
    }

    private Map<Type, Integer> getParameterTypes(ElementGem eleGem) {
        Map<Type, Integer> suppliedParameterTypes = new HashMap<>();
        if ( eleGem.booleans().hasValue() ) {
            suppliedParameterTypes.put(
                                      typeFactory.getType( boolean.class ),
                                      eleGem.booleans().get().size() );
        }
        if ( eleGem.bytes().hasValue() ) {
            suppliedParameterTypes.put(
                                      typeFactory.getType( byte.class ),
                                      eleGem.bytes().get().size() );
        }
        if ( eleGem.chars().hasValue() ) {
            suppliedParameterTypes.put(
                                      typeFactory.getType( char.class ),
                                      eleGem.chars().get().size() );
        }
        if ( eleGem.classes().hasValue() ) {
            List<?> classHandles = eleGem.classes().get();
            if ( classHandles != null ) {
                for ( Object handle : classHandles ) {
                    TypeDescriptor descriptor = typeFactory.getDescriptorFactory().typeDescriptor( handle );
                    if ( descriptor != null ) {
                        Type type = typeFactory.getType( descriptor );
                        suppliedParameterTypes.put( type, classHandles.size() );
                    }
                }
            }
        }
        if ( eleGem.doubles().hasValue() ) {
            suppliedParameterTypes.put(
                                      typeFactory.getType( double.class ),
                                      eleGem.doubles().get().size() );
        }
        if ( eleGem.floats().hasValue() ) {
            suppliedParameterTypes.put(
                                      typeFactory.getType( float.class ),
                                      eleGem.floats().get().size() );
        }
        if ( eleGem.ints().hasValue() ) {
            suppliedParameterTypes.put(
                                      typeFactory.getType( int.class ),
                                      eleGem.ints().get().size() );
        }
        if ( eleGem.longs().hasValue() ) {
            suppliedParameterTypes.put(
                                      typeFactory.getType( long.class ),
                                      eleGem.longs().get().size() );
        }
        if ( eleGem.shorts().hasValue() ) {
            suppliedParameterTypes.put(
                                      typeFactory.getType( short.class ),
                                      eleGem.shorts().get().size() );
        }
        if ( eleGem.strings().hasValue() ) {
            suppliedParameterTypes.put(
                                      typeFactory.getType( String.class ),
                                      eleGem.strings().get().size() );
        }
        if ( eleGem.enums().hasValue() && eleGem.enumClass().hasValue() ) {
            suppliedParameterTypes.put(
                                      typeFactory.getType( getTypeDescriptor( eleGem.enumClass() ) ),
                                      eleGem.enums().get().size() );
        }
        return suppliedParameterTypes;
    }

    private Type getAnnotationParameterType(Map<String, ExecutableDescriptor> annotationParameters,
                                            ElementGem element) {
        ExecutableDescriptor descriptor = annotationParameters.get( element.name().get() );
        if ( descriptor == null ) {
            return null;
        }
        TypeDescriptor returnType = descriptor.returnType();
        return returnType == null ? null : typeFactory.getType( returnType );
    }

    private TypeDescriptor getTypeDescriptor(GemValue<?> gemValue) {
        Object nativeType = gemValue != null ? gemValue.getValue() : null;
        if ( nativeType == null ) {
            throw new TypeHierarchyErroneousException();
        }
        return descriptorFactory.typeDescriptor( nativeType );
    }

}
