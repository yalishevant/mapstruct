/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.gem.MappingGem;
import org.mapstruct.ap.internal.gem.NullValueCheckStrategyGem;
import org.mapstruct.ap.internal.gem.NullValuePropertyMappingStrategyGem;
import org.mapstruct.ap.internal.model.common.FormattingParameters;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.langmodel.AnnotationGemFactory;
import org.mapstruct.ap.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.descriptor.ElementDescriptor;
import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.tools.gem.GemValue;

/**
 * Represents a property mapping as configured via {@code @Mapping} (no intermediate state).
 *
 * @author Gunnar Morling
 */
public class MappingOptions extends DelegatingOptions {

    private static final Pattern JAVA_EXPRESSION = Pattern.compile( "^\\s*java\\((.*)\\)\\s*$", Pattern.DOTALL );

    private final String sourceName;
    private final String constant;
    private final String javaExpression;
    private final String defaultJavaExpression;
    private final String conditionJavaExpression;
    private final String targetName;
    private final String defaultValue;
    private final FormattingParameters formattingParameters;
    private final SelectionParameters selectionParameters;

    private final boolean isIgnored;
    private final Set<String> dependsOn;

    private final ElementDescriptor element;
    private final AnnotationValueDescriptor sourceAnnotationValue;
    private final AnnotationValueDescriptor targetAnnotationValue;
    private final MappingGem mapping;

    private final InheritContext inheritContext;
    private final AnnotationDescriptor annotation;
    private final AnnotationValueDescriptor dependsOnAnnotationValue;

    public static class InheritContext {

        private final boolean isReversed;
        private final boolean isForwarded;
        private final Method templateMethod;

        public InheritContext(boolean isReversed, boolean isForwarded, Method templateMethod) {
            this.isReversed = isReversed;
            this.isForwarded = isForwarded;
            this.templateMethod = templateMethod;
        }

        public boolean isReversed() {
            return isReversed;
        }

        public boolean isForwarded() {
            return isForwarded;
        }

        public Method getTemplateMethod() {
            return templateMethod;
        }
    }

    public static Set<String> getMappingTargetNamesBy(Predicate<MappingOptions> predicate,
                                                      Set<MappingOptions> mappings) {
        return mappings.stream()
            .filter( predicate )
            .map( MappingOptions::getTargetName )
            .collect( Collectors.toCollection( LinkedHashSet::new ) );
    }

    public static void addAnnotation(MappingGem mapping,
                                     ExecutableDescriptor method,
                                     BeanMappingOptions beanMappingOptions,
                                     FormattingMessager messager,
                                     TypeFactory typeFactory,
                                     Set<MappingOptions> mappings) {
        Objects.requireNonNull( typeFactory, "typeFactory" );
        if ( mapping == null || method == null ) {
            return;
        }
        addInstance(
            mapping,
            method,
            null,
            beanMappingOptions,
            messager,
            typeFactory,
            mappings
        );
    }

    public static void addAnnotation(AnnotationDescriptor annotation,
                                     ExecutableDescriptor method,
                                     BeanMappingOptions beanMappingOptions,
                                     FormattingMessager messager,
                                     TypeFactory typeFactory,
                                     AnnotationGemFactory annotationGems,
                                     Set<MappingOptions> mappings) {
        Objects.requireNonNull( typeFactory, "typeFactory" );
        if ( annotation == null ) {
            return;
        }
        MappingGem mapping = annotationGems.mapping( annotation );
        addInstance(
            mapping,
            method,
            annotation,
            beanMappingOptions,
            messager,
            typeFactory,
            mappings
        );
    }

    public static void addAnnotations(Iterable<AnnotationDescriptor> annotations,
                                      ExecutableDescriptor method,
                                      BeanMappingOptions beanMappingOptions,
                                      FormattingMessager messager,
                                      TypeFactory typeFactory,
                                      AnnotationGemFactory annotationGems,
                                      Set<MappingOptions> mappings) {
        Objects.requireNonNull( typeFactory, "typeFactory" );
        if ( annotations == null ) {
            return;
        }
        for ( AnnotationDescriptor annotation : annotations ) {
            addAnnotation( annotation, method, beanMappingOptions, messager, typeFactory, annotationGems, mappings );
        }
    }

    public static void addInstance(MappingGem mapping, ExecutableDescriptor method,
                                   AnnotationDescriptor annotation,
                                   BeanMappingOptions beanMappingOptions, FormattingMessager messager,
                                   TypeFactory typeFactory,
                                   Set<MappingOptions> mappings) {

        String source = mapping.source().getValue();
        String constant = mapping.constant().getValue();
        String dateFormat = mapping.dateFormat().getValue();
        String numberFormat = mapping.numberFormat().getValue();
        String locale = mapping.locale().getValue();

        String defaultValue = mapping.defaultValue().getValue();

        Set<String> dependsOn = mapping.dependsOn().hasValue() ?
            new LinkedHashSet<>( mapping.dependsOn().getValue() ) :
            Collections.emptySet();

        AnnotationDescriptor mappingAnnotation = annotation != null ? annotation
            : typeFactory.getDescriptorFactory().annotationDescriptor( mapping.mirror() );

        if ( !isConsistent( mapping, method, messager, typeFactory, mappingAnnotation ) ) {
            return;
        }

        String expression = getExpression( mapping, method, messager, typeFactory, mappingAnnotation );
        String defaultExpression = getDefaultExpression( mapping, method, messager, typeFactory, mappingAnnotation );
        String conditionExpression =
            getConditionExpression( mapping, method, messager, typeFactory, mappingAnnotation );
        FormattingParameters formattingParam = new FormattingParameters(
            dateFormat,
            numberFormat,
            mappingAnnotation,
            typeFactory.getDescriptorFactory().annotationValueDescriptor( mapping.dateFormat().getAnnotationValue() ),
            method,
            locale
        );
        List<TypeDescriptor> qualifiers;
        if ( mapping.qualifiedBy().hasValue() ) {
            qualifiers = AnnotationValueUtils.asTypeList(
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( mapping.qualifiedBy().getAnnotationValue() )
            );
        }
        else {
            qualifiers = Collections.<TypeDescriptor>emptyList();
        }
        List<TypeDescriptor> conditionQualifiers;
        if ( mapping.conditionQualifiedBy().hasValue() ) {
            conditionQualifiers = AnnotationValueUtils.asTypeList(
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( mapping.conditionQualifiedBy().getAnnotationValue() )
            );
        }
        else {
            conditionQualifiers = Collections.<TypeDescriptor>emptyList();
        }
        TypeDescriptor resultType = mapping.resultType().hasValue()
            ? AnnotationValueUtils.asType(
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( mapping.resultType().getAnnotationValue() )
            )
            : null;

        SelectionParameters selectionParams = new SelectionParameters(
            qualifiers,
            mapping.qualifiedByName().get(),
            conditionQualifiers,
            mapping.conditionQualifiedByName().get(),
            resultType
        );

        MappingOptions options = new MappingOptions(
            mapping.target().getValue(),
            method,
            typeFactory.getDescriptorFactory()
                .annotationValueDescriptor( mapping.target().getAnnotationValue() ),
            source,
            typeFactory.getDescriptorFactory()
                .annotationValueDescriptor( mapping.source().getAnnotationValue() ),
            constant,
            expression,
            defaultExpression,
            conditionExpression,
            defaultValue,
            mapping.ignore().get(),
            formattingParam,
            selectionParams,
            dependsOn,
            mapping,
            mapping.dependsOn().hasValue()
                ? typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( mapping.dependsOn().getAnnotationValue() )
                : null,
            null,
            mappingAnnotation,
            beanMappingOptions
        );

        if ( mappings.contains( options ) ) {
            messager.printMessage( method, Message.PROPERTYMAPPING_DUPLICATE_TARGETS, mapping.target().get() );
        }
        else {
            mappings.add( options );
        }
    }

    public static MappingOptions forIgnore(String targetName) {
        return new MappingOptions(
            targetName,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            SelectionParameters.empty(),
            Collections.emptySet(),
            null,
            null,
            null,
            null,
            null
        );
    }

    private static boolean isConsistent(MappingGem gem, ExecutableDescriptor method,
                                        FormattingMessager messager, TypeFactory typeFactory,
                                        AnnotationDescriptor mappingAnnotation) {

        if ( !gem.target().hasValue() ) {
            messager.printMessage(
                method,
                mappingAnnotation,
                typeFactory.getDescriptorFactory().annotationValueDescriptor( gem.target().getAnnotationValue() ),
                Message.PROPERTYMAPPING_EMPTY_TARGET
            );
            return false;
        }

        Message message = null;
        if ( gem.source().hasValue() && gem.constant().hasValue() ) {
            message = Message.PROPERTYMAPPING_SOURCE_AND_CONSTANT_BOTH_DEFINED;
        }
        else if ( gem.expression().hasValue() && gem.conditionQualifiedByName().hasValue() ) {
            message = Message.PROPERTYMAPPING_EXPRESSION_AND_CONDITION_QUALIFIED_BY_NAME_BOTH_DEFINED;
        }
        else if ( gem.source().hasValue() && gem.expression().hasValue() ) {
            message = Message.PROPERTYMAPPING_SOURCE_AND_EXPRESSION_BOTH_DEFINED;
        }
        else if ( gem.expression().hasValue() && gem.constant().hasValue() ) {
            message = Message.PROPERTYMAPPING_EXPRESSION_AND_CONSTANT_BOTH_DEFINED;
        }
        else if ( gem.expression().hasValue() && gem.defaultValue().hasValue() ) {
            message = Message.PROPERTYMAPPING_EXPRESSION_AND_DEFAULT_VALUE_BOTH_DEFINED;
        }
        else if ( gem.constant().hasValue() && gem.defaultValue().hasValue() ) {
            message = Message.PROPERTYMAPPING_CONSTANT_AND_DEFAULT_VALUE_BOTH_DEFINED;
        }
        else if ( gem.expression().hasValue() && gem.defaultExpression().hasValue() ) {
            message = Message.PROPERTYMAPPING_EXPRESSION_AND_DEFAULT_EXPRESSION_BOTH_DEFINED;
        }
        else if ( gem.expression().hasValue() && gem.conditionExpression().hasValue() ) {
            message = Message.PROPERTYMAPPING_EXPRESSION_AND_CONDITION_EXPRESSION_BOTH_DEFINED;
        }
        else if ( gem.constant().hasValue() && gem.defaultExpression().hasValue() ) {
            message = Message.PROPERTYMAPPING_CONSTANT_AND_DEFAULT_EXPRESSION_BOTH_DEFINED;
        }
        else if ( gem.constant().hasValue() && gem.conditionExpression().hasValue() ) {
            message = Message.PROPERTYMAPPING_CONSTANT_AND_CONDITION_EXPRESSION_BOTH_DEFINED;
        }
        else if ( gem.defaultValue().hasValue() && gem.defaultExpression().hasValue() ) {
            message = Message.PROPERTYMAPPING_DEFAULT_VALUE_AND_DEFAULT_EXPRESSION_BOTH_DEFINED;
        }
        else if ( gem.expression().hasValue()
            && ( gem.qualifiedByName().hasValue() || gem.qualifiedBy().hasValue() ) ) {
            message = Message.PROPERTYMAPPING_EXPRESSION_AND_QUALIFIER_BOTH_DEFINED;
        }
        else if ( gem.nullValuePropertyMappingStrategy().hasValue() && gem.defaultValue().hasValue() ) {
            message = Message.PROPERTYMAPPING_DEFAULT_VALUE_AND_NVPMS;
        }
        else if ( gem.nullValuePropertyMappingStrategy().hasValue() && gem.constant().hasValue() ) {
            message = Message.PROPERTYMAPPING_CONSTANT_VALUE_AND_NVPMS;
        }
        else if ( gem.nullValuePropertyMappingStrategy().hasValue() && gem.expression().hasValue() ) {
            message = Message.PROPERTYMAPPING_EXPRESSION_VALUE_AND_NVPMS;
        }
        else if ( gem.nullValuePropertyMappingStrategy().hasValue() && gem.defaultExpression().hasValue() ) {
            message = Message.PROPERTYMAPPING_DEFAULT_EXPERSSION_AND_NVPMS;
        }
        else if ( gem.nullValuePropertyMappingStrategy().hasValue()
            && gem.ignore().hasValue() && gem.ignore().getValue() ) {
            message = Message.PROPERTYMAPPING_IGNORE_AND_NVPMS;
        }
        else if ( ".".equals( gem.target().get() ) && gem.ignore().hasValue() && gem.ignore().getValue() ) {
            message = Message.PROPERTYMAPPING_TARGET_THIS_AND_IGNORE;
        }
        else if ( ".".equals( gem.target().get() ) && !gem.source().hasValue() ) {
            message = Message.PROPERTYMAPPING_TARGET_THIS_NO_SOURCE;
        }

        if ( message == null ) {
            return true;
        }
        else {
            messager.printMessage( method, mappingAnnotation, message );
            return false;
        }
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private MappingOptions(String targetName,
                           ElementDescriptor element,
                           AnnotationValueDescriptor targetAnnotationValue,
                           String sourceName,
                           AnnotationValueDescriptor sourceAnnotationValue,
                           String constant,
                           String javaExpression,
                           String defaultJavaExpression,
                           String conditionJavaExpression,
                           String defaultValue,
                           boolean isIgnored,
                           FormattingParameters formattingParameters,
                           SelectionParameters selectionParameters,
                           Set<String> dependsOn,
                           MappingGem mapping,
                           AnnotationValueDescriptor dependsOnAnnotationValue,
                           InheritContext inheritContext,
                           AnnotationDescriptor annotation,
                           DelegatingOptions next
    ) {
        super( next );
        this.targetName = targetName;
        this.element = element;
        this.targetAnnotationValue = targetAnnotationValue;
        this.sourceName = sourceName;
        this.sourceAnnotationValue = sourceAnnotationValue;
        this.constant = constant;
        this.javaExpression = javaExpression;
        this.defaultJavaExpression = defaultJavaExpression;
        this.conditionJavaExpression = conditionJavaExpression;
        this.defaultValue = defaultValue;
        this.isIgnored = isIgnored;
        this.formattingParameters = formattingParameters;
        this.selectionParameters = selectionParameters;
        this.dependsOn = dependsOn;
        this.mapping = mapping;
        this.dependsOnAnnotationValue = dependsOnAnnotationValue;
        this.inheritContext = inheritContext;
        this.annotation = annotation;
    }

    private static String getExpression(MappingGem mapping, ExecutableDescriptor element,
                                        FormattingMessager messager, TypeFactory typeFactory,
                                        AnnotationDescriptor mappingAnnotation) {
        if ( !mapping.expression().hasValue() ) {
            return null;
        }

        Matcher javaExpressionMatcher = JAVA_EXPRESSION.matcher( mapping.expression().get() );

        if ( !javaExpressionMatcher.matches() ) {
            messager.printMessage(
                element,
                mappingAnnotation,
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( mapping.expression().getAnnotationValue() ),
                Message.PROPERTYMAPPING_INVALID_EXPRESSION
            );
            return null;
        }

        return javaExpressionMatcher.group( 1 ).trim();
    }

    private static String getDefaultExpression(MappingGem mapping,
                                               ExecutableDescriptor element,
                                               FormattingMessager messager,
                                               TypeFactory typeFactory,
                                               AnnotationDescriptor mappingAnnotation) {
        if ( !mapping.defaultExpression().hasValue() ) {
            return null;
        }

        Matcher javaExpressionMatcher = JAVA_EXPRESSION.matcher( mapping.defaultExpression().get() );

        if ( !javaExpressionMatcher.matches() ) {
            messager.printMessage(
                element,
                mappingAnnotation,
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( mapping.defaultExpression().getAnnotationValue() ),
                Message.PROPERTYMAPPING_INVALID_DEFAULT_EXPRESSION
            );
            return null;
        }

        return javaExpressionMatcher.group( 1 ).trim();
    }

    private static String getConditionExpression(MappingGem mapping,
                                                 ExecutableDescriptor element,
                                                 FormattingMessager messager,
                                                 TypeFactory typeFactory,
                                                 AnnotationDescriptor mappingAnnotation) {
        if ( !mapping.conditionExpression().hasValue() ) {
            return null;
        }

        Matcher javaExpressionMatcher = JAVA_EXPRESSION.matcher( mapping.conditionExpression().get() );

        if ( !javaExpressionMatcher.matches() ) {
            messager.printMessage(
                element,
                mappingAnnotation,
                typeFactory.getDescriptorFactory()
                    .annotationValueDescriptor( mapping.conditionExpression().getAnnotationValue() ),
                Message.PROPERTYMAPPING_INVALID_CONDITION_EXPRESSION
            );
            return null;
        }

        return javaExpressionMatcher.group( 1 ).trim();
    }

    public String getTargetName() {
        return targetName;
    }

    public AnnotationValueDescriptor getTargetAnnotationValue() {
        return targetAnnotationValue;
    }

    /**
     * Returns the complete source name of this mapping, either a qualified (e.g. {@code parameter1.foo}) or
     * unqualified (e.g. {@code foo}) property reference.
     *
     * @return The complete source name of this mapping.
     */
    public String getSourceName() {
        return sourceName;
    }

    public AnnotationValueDescriptor getSourceAnnotationValue() {
        return sourceAnnotationValue;
    }

    public String getConstant() {
        return constant;
    }

    public String getJavaExpression() {
        return javaExpression;
    }

    public String getDefaultJavaExpression() {
        return defaultJavaExpression;
    }

    public String getConditionJavaExpression() {
        return conditionJavaExpression;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public FormattingParameters getFormattingParameters() {
        return formattingParameters;
    }

    public SelectionParameters getSelectionParameters() {
        return selectionParameters;
    }

    public boolean isIgnored() {
        return isIgnored;
    }

    public ElementDescriptor getElement() {
        return element;
    }

    public AnnotationDescriptor getAnnotation() {
        return annotation;
    }

    public AnnotationValueDescriptor getDependsOnAnnotationValue() {
        return dependsOnAnnotationValue;
    }

    public Set<String> getDependsOn() {
        return dependsOn;
    }

    public InheritContext getInheritContext() {
        return inheritContext;
    }

    @Override
    public NullValueCheckStrategyGem getNullValueCheckStrategy() {
        return Optional.ofNullable( mapping ).map( MappingGem::nullValueCheckStrategy )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( NullValueCheckStrategyGem::valueOf )
            .orElse( next().getNullValueCheckStrategy() );
    }

    @Override
    public NullValuePropertyMappingStrategyGem getNullValuePropertyMappingStrategy() {
        return Optional.ofNullable( mapping ).map( MappingGem::nullValuePropertyMappingStrategy )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( NullValuePropertyMappingStrategyGem::valueOf )
            .orElse( next().getNullValuePropertyMappingStrategy() );
    }

    @Override
    public MappingControl getMappingControl(TypeFactory typeFactory) {
        return Optional.ofNullable( mapping ).map( MappingGem::mappingControl )
            .filter( GemValue::hasValue )
            .map( GemValue::getValue )
            .map( mc -> MappingControl.fromTypeDescriptor(
                typeFactory.getDescriptorFactory().typeDescriptor( mc ),
                typeFactory.langElements() ) )
            .orElse( next().getMappingControl( typeFactory ) );
    }

    /**
     *  Mapping can only be inversed if the source was not a constant nor an expression
     *
     * @return true when the above applies
     */
    public boolean canInverse() {
        return constant == null && javaExpression == null;
    }

    public MappingOptions copyForInverseInheritance(SourceMethod templateMethod,
                                                    BeanMappingOptions beanMappingOptions ) {

        MappingOptions mappingOptions = new MappingOptions(
            sourceName != null ? sourceName : targetName,
            templateMethod.getExecutableDescriptor(),
            targetAnnotationValue,
            sourceName != null ? targetName : null,
            sourceAnnotationValue,
            null, // constant
            null, // expression
            null, // defaultExpression
            null, // conditionExpression
            null,
            isIgnored,
            formattingParameters,
            selectionParameters,
            Collections.emptySet(),
            mapping,
            dependsOnAnnotationValue,
            new InheritContext( true, false, templateMethod ),
            annotation,
            beanMappingOptions
        );
        return mappingOptions;

    }

    /**
     * Creates a copy of this mapping
     *
     * @param templateMethod the template method for the inheritance
     * @param beanMappingOptions the bean mapping options
     *
     * @return the copy
     */
    public MappingOptions copyForForwardInheritance(SourceMethod templateMethod,
                                                    BeanMappingOptions beanMappingOptions ) {
        MappingOptions mappingOptions = new MappingOptions(
            targetName,
            templateMethod.getExecutableDescriptor(),
            targetAnnotationValue,
            sourceName,
            sourceAnnotationValue,
            constant,
            javaExpression,
            defaultJavaExpression,
            conditionJavaExpression,
            defaultValue,
            isIgnored,
            formattingParameters,
            selectionParameters,
            dependsOn,
            mapping,
            dependsOnAnnotationValue,
            new InheritContext( false, true, templateMethod ),
            annotation,
            beanMappingOptions
        );
        return mappingOptions;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( ".".equals( this.targetName ) ) {
            // target this will never be equal to any other target this or any other.
            return false;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }
        MappingOptions mapping = (MappingOptions) o;
        return targetName.equals( mapping.targetName );
    }

    @Override
    public int hashCode() {
        return Objects.hash( targetName );
    }

    @Override
    public String toString() {
        return "Mapping {" +
            "\n    sourceName='" + sourceName + "\'," +
            "\n    targetName='" + targetName + "\'," +
            "\n}";
    }

    @Override
    public boolean hasAnnotation() {
        return mapping != null;
    }

}
