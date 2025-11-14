/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.mapstruct.ap.internal.model.common.Assignment;
import org.mapstruct.ap.internal.model.common.FormattingParameters;
import org.mapstruct.ap.internal.model.common.SourceRHS;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.langmodel.api.EnumMappingSupport;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.MissingLangModelCapabilityException;
import org.mapstruct.ap.internal.langmodel.api.MappingExclusionSupport;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.model.source.Method;
import org.mapstruct.ap.internal.model.source.SourceMethod;
import org.mapstruct.ap.internal.model.source.selector.SelectionCriteria;
import org.mapstruct.ap.internal.option.Options;
import org.mapstruct.ap.internal.util.AccessorNamingUtils;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.util.Services;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.spi.EnumMappingStrategy;
import org.mapstruct.ap.spi.EnumTransformationStrategy;
import org.mapstruct.ap.internal.langmodel.spi.EnumMappingCapability;
import org.mapstruct.ap.internal.langmodel.spi.MappingExclusionCapability;
import org.mapstruct.ap.spi.MappingExclusionProvider;
import org.mapstruct.ap.internal.spi.DefaultMappingExclusionProvider;

/**
 * This class provides the context for the builders.
 * <p>
 * The context provides:
 * <ul>
 * <li>Input for the building process, such as the source model (mapping methods found) and mapper references.</li>
 * <li>Required factory, utility, reporting methods for building the mappings.</li>
 * <li>Means to harbor results produced by the builders, such as forged- and supported mapping methods that should be
 * generated in a later stage.</li>
 * </ul>
 *
 * @author Sjaak Derksen
 */
public class MappingBuilderContext {

    private static final MappingExclusionProvider SUB_MAPPING_EXCLUSION_PROVIDER = Services.get(
        MappingExclusionProvider.class,
        new DefaultMappingExclusionProvider()
    );

    /**
     * Resolves the most suitable way for mapping an element (property, iterable element etc.) from source to target.
     * There are 2 basic types of mappings:
     * <ul>
     * <li>conversions</li>
     * <li>methods</li>
     * </ul>
     * conversions are essentially one line mappings, such as String to Integer and Integer to Long methods come in some
     * varieties:
     * <ul>
     * <li>referenced mapping methods, these are methods implemented (or referenced) by the user. Sometimes indicated
     * with the 'uses' in the mapping annotations or part of the abstract mapper class</li>
     * <li>generated mapping methods (by means of MapStruct)</li>
     * <li>built in methods</li>
     * </ul>
     *
     * @author Sjaak Derksen
     */
    public interface MappingResolver {

        /**
         * returns a parameter assignment
         *
         * @param mappingMethod target mapping method
         * @param description the description source
         * @param targetType return type to match
         * @param formattingParameters used for formatting dates and numbers
         * @param criteria parameters criteria in the selection process
         * @param sourceRHS source information
         * @param positionHint the annotation for reporting problems
         * @param forger the supplier of the callback method to forge a method
         *
         * @return an assignment to a method parameter, which can either be:
         * <ol>
         * <li>MethodReference</li>
         * <li>TypeConversion</li>
         * <li>SourceRHS Assignment (empty TargetAssignment)</li>
         * <li>null, no assignment found</li>
         * </ol>
         */
        Assignment getTargetAssignment(Method mappingMethod, ForgedMethodHistory description, Type targetType,
                                       FormattingParameters formattingParameters,
                                       SelectionCriteria criteria, SourceRHS sourceRHS,
                                       AnnotationDescriptor positionHint,
                                       Supplier<Assignment> forger);

        Set<SupportingMappingMethod> getUsedSupportedMappings();

        Set<Field> getUsedSupportedFields();
    }

    private final TypeFactory typeFactory;
    private final LangModelContext langModelContext;
    private final LangElements langElements;
    private final FormattingMessager messager;
    private final AccessorNamingUtils accessorNaming;
    private final EnumMappingStrategy enumMappingStrategy;
    private final EnumMappingSupport enumMappingSupport;
    private final MappingExclusionSupport mappingExclusionSupport;
    private final Map<String, EnumTransformationStrategy> enumTransformationStrategies;
    private final Options options;
    private final TypeElementDescriptor mapperTypeDescriptor;
    private final List<SourceMethod> sourceModel;
    private final List<MapperReference> mapperReferences;
    private final MappingResolver mappingResolver;
    private final List<MappingMethod> mappingsToGenerate = new ArrayList<>();
    private final Map<ForgedMethod, ForgedMethod> forgedMethodsUnderCreation =
        new HashMap<>();

    //CHECKSTYLE:OFF
    public MappingBuilderContext(TypeFactory typeFactory,
                          LangModelContext langModelContext,
                          FormattingMessager messager,
                          AccessorNamingUtils accessorNaming,
                          EnumMappingStrategy enumMappingStrategy,
                          Map<String, EnumTransformationStrategy> enumTransformationStrategies,
                          Options options,
                          MappingResolver mappingResolver,
                          TypeElementDescriptor mapperDescriptor,
                          List<SourceMethod> sourceModel,
                          List<MapperReference> mapperReferences) {
        this.typeFactory = typeFactory;
        this.langModelContext = langModelContext;
        this.langElements = langModelContext.elementQuery().elements();
        this.messager = messager;
        this.accessorNaming = accessorNaming;
        this.enumMappingStrategy = enumMappingStrategy;
        EnumMappingCapability enumMappingCapability = langModelContext.optional( EnumMappingCapability.class )
            .orElseThrow( () -> MissingLangModelCapabilityException.required( EnumMappingCapability.class ) );
        this.enumMappingSupport = enumMappingCapability.enumMappingSupport( enumMappingStrategy );
        MappingExclusionCapability mappingExclusionCapability =
            langModelContext.optional( MappingExclusionCapability.class ).orElse( null );
        if ( mappingExclusionCapability != null ) {
            this.mappingExclusionSupport = mappingExclusionCapability.mappingExclusionSupport(
                SUB_MAPPING_EXCLUSION_PROVIDER
            );
        }
        else {
            this.mappingExclusionSupport = descriptor -> false;
            messager.printMessage(
                Message.OPTIONAL_CAPABILITY_MISSING,
                "MappingExclusionCapability",
                "Mapping exclusion configuration"
            );
        }
        this.enumTransformationStrategies = enumTransformationStrategies;
        this.options = options;
        this.mappingResolver = mappingResolver;
        this.mapperTypeDescriptor = mapperDescriptor;
        this.sourceModel = sourceModel;
        this.mapperReferences = mapperReferences;
    }
    //CHECKSTYLE:ON

    /**
     * Returns a map which is used to track which forged methods are under creation.
     * Used for cutting the possible infinite recursion of forged method creation.
     *
     * Map is used instead of set because not all fields of ForgedMethods are used in equals/hashCode and we are
     * interested only in the first created ForgedMethod
     *
     * @return map of forged methods
     */
    public Map<ForgedMethod, ForgedMethod> getForgedMethodsUnderCreation() {
        return forgedMethodsUnderCreation;
    }

    public List<SourceMethod> getSourceModel() {
        return sourceModel;
    }

    public List<MapperReference> getMapperReferences() {
        return mapperReferences;
    }

    public TypeFactory getTypeFactory() {
        return typeFactory;
    }

    public FormattingMessager getMessager() {
        return messager;
    }

    public AccessorNamingUtils getAccessorNaming() {
        return accessorNaming;
    }

    public EnumMappingStrategy getEnumMappingStrategy() {
        return enumMappingStrategy;
    }

    public EnumMappingSupport getEnumMappingSupport() {
        return enumMappingSupport;
    }

    public LangModelContext getLangModelContext() {
        return langModelContext;
    }

    public LangElements getLangElements() {
        return langElements;
    }

    public TypeElementDescriptor getMapperTypeDescriptor() {
        return mapperTypeDescriptor;
    }

    public Map<String, EnumTransformationStrategy> getEnumTransformationStrategies() {
        return enumTransformationStrategies;
    }

    public Options getOptions() {
        return options;
    }

    public MappingResolver getMappingResolver() {
        return mappingResolver;
    }

    public List<MappingMethod> getMappingsToGenerate() {
        return mappingsToGenerate;
    }

    public List<String> getReservedNames() {
        Set<String> nameSet = new HashSet<>();
        for ( MappingMethod method : mappingsToGenerate ) {
            nameSet.add( method.getName() );
        }
        // add existing names
        for ( SourceMethod method : sourceModel) {
            if ( method.isAbstract() ) {
                nameSet.add( method.getName() );
            }
        }
        return new ArrayList<>( nameSet );
    }

    public MappingMethod getExistingMappingMethod(MappingMethod newMappingMethod) {
        MappingMethod existingMappingMethod = null;
        for ( MappingMethod mappingMethod : mappingsToGenerate ) {
            if ( newMappingMethod.equals( mappingMethod ) ) {
                existingMappingMethod = mappingMethod;
                break;
            }
        }
        return existingMappingMethod;
    }

    public Set<SupportingMappingMethod> getUsedSupportedMappings() {
        return mappingResolver.getUsedSupportedMappings();
    }

    public Set<Field> getUsedSupportedFields() {
        return mappingResolver.getUsedSupportedFields();
    }

    /**
     * @param sourceType from which an automatic sub-mapping needs to be generated
     * @param targetType to which an automatic sub-mapping needs to be generated
     *
     * @return {@code true} if MapStruct is allowed to try and generate an automatic sub-mapping between the
     * source and target {@link Type}
     */
    public boolean canGenerateAutoSubMappingBetween(Type sourceType, Type targetType) {
        return canGenerateAutoSubMappingFor( sourceType ) && canGenerateAutoSubMappingFor( targetType );
    }

    /**
     * @param type that MapStruct wants to use to generate an automatic sub-mapping for/from
     *
     * @return {@code true} if the type is not excluded from the {@link MappingExclusionProvider}
     */
    private boolean canGenerateAutoSubMappingFor(Type type) {
        TypeDescriptor descriptor = type.getTypeDescriptor();
        return descriptor != null && !mappingExclusionSupport.isExcluded( descriptor );
    }

    public boolean isErroneous() {
        return messager.isErroneous();
    }
}
