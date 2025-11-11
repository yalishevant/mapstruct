/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.gem.SubclassMappingGem;
import org.mapstruct.ap.internal.model.common.Parameter;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.langmodel.api.LangTypes;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;

import static org.mapstruct.ap.internal.util.Message.SUBCLASSMAPPING_ILLEGAL_SUBCLASS;
import static org.mapstruct.ap.internal.util.Message.SUBCLASSMAPPING_NO_VALID_SUPERCLASS;
import static org.mapstruct.ap.internal.util.Message.SUBCLASSMAPPING_UPDATE_METHODS_NOT_SUPPORTED;

/**
 * Represents a subclass mapping as configured via {@code @SubclassMapping}.
 *
 * @author Ben Zegveld
 */
public class SubclassMappingOptions extends DelegatingOptions {

    private final TypeDescriptor sourceType;
    private final TypeDescriptor targetType;
    private final SelectionParameters selectionParameters;
    private final SubclassMappingGem subclassMapping;
    private final AnnotationDescriptor annotation;

    public SubclassMappingOptions(TypeDescriptor sourceType, TypeDescriptor targetType,
                                  DelegatingOptions next, SelectionParameters selectionParameters,
                                  SubclassMappingGem subclassMapping,
                                  AnnotationDescriptor annotation) {
        super( next );
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.selectionParameters = selectionParameters;
        this.subclassMapping = subclassMapping;
        this.annotation = annotation;
    }

    @Override
    public boolean hasAnnotation() {
        return annotation != null;
    }

    private static boolean isConsistent(SubclassMappingGem gem,
                                        AnnotationDescriptor annotation,
                                        ExecutableDescriptor method,
                                        FormattingMessager messager,
                                        List<Parameter> sourceParameters,
                                        Type resultType,
                                        SubclassValidator subclassValidator,
                                        TypeFactory typeFactory) {
        if ( gem == null ) {
            return false;
        }
        if ( resultType == null ) {
            messager.printMessage( method, annotation, SUBCLASSMAPPING_UPDATE_METHODS_NOT_SUPPORTED );
            return false;
        }

        TypeDescriptor sourceSubclassDescriptor = toTypeDescriptor( typeFactory, gem.source().getValue() );
        TypeDescriptor targetSubclassDescriptor = toTypeDescriptor( typeFactory, gem.target().getValue() );
        TypeDescriptor targetParentDescriptor = resultType.getTypeDescriptor();
        validateTypeDescriptors( sourceSubclassDescriptor, targetSubclassDescriptor, targetParentDescriptor );

        LangTypes langTypes = typeFactory.langTypes();

        boolean isConsistent = true;

        boolean isChildOfAParameter = false;
        for ( Parameter sourceParameter : sourceParameters ) {
            TypeDescriptor sourceParentDescriptor = sourceParameter.getType().getTypeDescriptor();
            if ( sourceParentDescriptor == null ) {
                throw new TypeHierarchyErroneousException();
            }
            isChildOfAParameter = isChildOfAParameter
                || isChildOfParent( langTypes, sourceSubclassDescriptor, sourceParentDescriptor );
        }
        if ( !isChildOfAParameter ) {
            messager
                    .printMessage(
                        method,
                        annotation,
                        SUBCLASSMAPPING_NO_VALID_SUPERCLASS,
                        sourceSubclassDescriptor.displayName() );
            isConsistent = false;
        }
        if ( !isChildOfParent( langTypes, targetSubclassDescriptor, targetParentDescriptor ) ) {
            messager
                    .printMessage(
                        method,
                        annotation,
                        SUBCLASSMAPPING_ILLEGAL_SUBCLASS,
                        targetParentDescriptor.displayName(),
                        targetSubclassDescriptor.displayName() );
            isConsistent = false;
        }
        if ( !subclassValidator.isValidUsage( method, annotation, sourceSubclassDescriptor ) ) {
            isConsistent = false;
        }
        return isConsistent;
    }

    private static void validateTypeDescriptors(TypeDescriptor... descriptors) {
        for ( TypeDescriptor descriptor : descriptors ) {
            if ( descriptor == null ) {
                throw new TypeHierarchyErroneousException();
            }
        }
    }

    private static TypeDescriptor toTypeDescriptor(TypeFactory typeFactory, Object handle) {
        if ( handle == null ) {
            throw new TypeHierarchyErroneousException();
        }
        TypeDescriptor descriptor = typeFactory.getDescriptorFactory().typeDescriptor( handle );
        if ( descriptor == null ) {
            throw new TypeHierarchyErroneousException();
        }
        return descriptor;
    }

    private static boolean isChildOfParent(LangTypes langTypes, TypeDescriptor childType, TypeDescriptor parentType) {
        return langTypes.isSubtype( childType, parentType );
    }

    public TypeDescriptor getSourceType() {
        return sourceType;
    }

    public TypeDescriptor getTargetType() {
        return targetType;
    }

    public TypeDescriptor getSource() {
        return sourceType;
    }

    public TypeDescriptor getTarget() {
        return targetType;
    }

    public SelectionParameters getSelectionParameters() {
        return selectionParameters;
    }

    public AnnotationDescriptor getAnnotation() {
        return annotation;
    }

    public static SubclassMappingOptions getInstanceOn(SubclassMappingGem subclassMapping,
                                                       AnnotationDescriptor annotation,
                                                       BeanMappingOptions beanMappingOptions,
                                                       List<Parameter> sourceParameters,
                                                       Type resultType,
                                                       ExecutableDescriptor method,
                                                       SubclassValidator subclassValidator,
                                                       FormattingMessager messager,
                                                       TypeFactory typeFactory) {
        if ( !isConsistent( subclassMapping, annotation, method, messager, sourceParameters, resultType,
            subclassValidator, typeFactory ) ) {
            return null;
        }

        TypeDescriptor sourceSubclassDescriptor = toTypeDescriptor( typeFactory, subclassMapping.source().getValue() );
        TypeDescriptor targetSubclassDescriptor = toTypeDescriptor( typeFactory, subclassMapping.target().getValue() );
        AnnotationValueDescriptor qualifiedByValue =
            annotation != null ? AnnotationDescriptorUtils.getValue( annotation, "qualifiedBy" ) : null;
        AnnotationValueDescriptor targetValue =
            annotation != null ? AnnotationDescriptorUtils.getValue( annotation, "target" ) : null;

        TypeDescriptor targetDescriptor = AnnotationValueUtils.asType( targetValue );
        if ( targetDescriptor == null ) {
            targetDescriptor = targetSubclassDescriptor;
        }

        SelectionParameters selectionParameters = new SelectionParameters(
            AnnotationValueUtils.asTypeList( qualifiedByValue ),
            subclassMapping.qualifiedByName().get(),
            targetDescriptor
        );

        return new SubclassMappingOptions(
            sourceSubclassDescriptor,
            targetDescriptor,
            beanMappingOptions,
            selectionParameters,
            subclassMapping,
            annotation
        );
    }

    public static List<SubclassMappingOptions> copyForInverseInheritance(Set<SubclassMappingOptions> mappings,
                                                                         BeanMappingOptions beanMappingOptions) {
        // we are not interested in keeping it unique at this point.
        return mappings.stream().map( mapping -> new SubclassMappingOptions(
            mapping.targetType,
            mapping.sourceType,
            beanMappingOptions,
            mapping.selectionParameters,
            mapping.subclassMapping,
            mapping.annotation
        ) ).collect( Collectors.toCollection( ArrayList::new ) );
    }

    public static List<SubclassMappingOptions> copyForInheritance(Set<SubclassMappingOptions> subclassMappings,
                                                                  BeanMappingOptions beanMappingOptions) {
         // we are not interested in keeping it unique at this point.
         List<SubclassMappingOptions> mappings = new ArrayList<>();
         for ( SubclassMappingOptions subclassMapping : subclassMappings ) {
             mappings.add(
                        new SubclassMappingOptions(
                                   subclassMapping.sourceType,
                                   subclassMapping.targetType,
                                   beanMappingOptions,
                                   subclassMapping.selectionParameters,
                                    subclassMapping.subclassMapping,
                                    subclassMapping.annotation ) );
         }
         return mappings;
     }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null || !( obj instanceof SubclassMappingOptions ) ) {
            return false;
        }
        SubclassMappingOptions other = (SubclassMappingOptions) obj;
        if ( sourceType == null || other.sourceType == null ) {
            return sourceType == other.sourceType;
        }
        return sourceType.id().equals( other.sourceType.id() );
    }

    @Override
    public int hashCode() {
        return sourceType != null ? sourceType.id().hashCode() : 0;
    }
}
