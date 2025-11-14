/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.gem.ConditionGem;
import org.mapstruct.ap.internal.gem.ConditionStrategyGem;
import org.mapstruct.ap.internal.model.common.Parameter;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * @author Filip Hrisafov
 */
public class ConditionOptions {

    private final Set<ConditionStrategyGem> conditionStrategies;

    private ConditionOptions(Set<ConditionStrategyGem> conditionStrategies) {
        this.conditionStrategies = conditionStrategies;
    }

    public Collection<ConditionStrategyGem> getConditionStrategies() {
        return conditionStrategies;
    }

    public static ConditionOptions getInstanceOn(ConditionGem condition,
                                                 AnnotationDescriptor annotation,
                                                 ExecutableDescriptor method,
                                                 List<Parameter> parameters,
                                                 FormattingMessager messager) {
        if ( condition == null ) {
            return null;
        }

        TypeDescriptor returnType = method.returnType();
        if ( !isBooleanReturnType( returnType ) ) {
            return null;
        }

        Set<ConditionStrategyGem> strategies = condition.appliesTo().get()
            .stream()
            .map( ConditionStrategyGem::valueOf )
            .collect( Collectors.toCollection( () -> EnumSet.noneOf( ConditionStrategyGem.class ) ) );

        if ( strategies.isEmpty() ) {
            AnnotationValueDescriptor appliesToValue = AnnotationDescriptorUtils.getValue( annotation, "appliesTo" );
            messager.printMessage(
                method,
                annotation,
                appliesToValue,
                Message.CONDITION_MISSING_APPLIES_TO_STRATEGY
            );

            return null;
        }

        boolean allStrategiesValid = true;

        for ( ConditionStrategyGem strategy : strategies ) {
            boolean isStrategyValid = isValid( strategy, condition, annotation, method, parameters, messager );
            allStrategiesValid &= isStrategyValid;
        }

        return allStrategiesValid ? new ConditionOptions( strategies ) : null;
    }

    protected static boolean isValid(ConditionStrategyGem strategy, ConditionGem condition,
                                     AnnotationDescriptor annotation,
                                     ExecutableDescriptor method, List<Parameter> parameters,
                                     FormattingMessager messager) {
        if ( strategy == ConditionStrategyGem.SOURCE_PARAMETERS ) {
            return hasValidStrategyForSourceProperties( condition, annotation, method, parameters, messager );
        }
        else if ( strategy == ConditionStrategyGem.PROPERTIES ) {
            return hasValidStrategyForProperties( condition, annotation, method, parameters, messager );
        }
        else {
            throw new IllegalStateException( "Invalid condition strategy: " + strategy );
        }
    }

    protected static boolean hasValidStrategyForSourceProperties(ConditionGem condition,
                                                                 AnnotationDescriptor annotation,
                                                                 ExecutableDescriptor method,
                                                                 List<Parameter> parameters,
                                                                 FormattingMessager messager) {
        for ( Parameter parameter : parameters ) {
            if ( parameter.isSourceParameter() ) {
                // source parameter is a valid parameter for a source condition check
                continue;
            }

            if ( parameter.isMappingContext() ) {
                // mapping context parameter is a valid parameter for a source condition check
                continue;
            }

            messager.printMessage(
                method,
                annotation,
                Message.CONDITION_SOURCE_PARAMETERS_INVALID_PARAMETER,
                parameter.describe()
            );
            return false;
        }
        return true;
    }

    protected static boolean hasValidStrategyForProperties(ConditionGem condition,
                                                           AnnotationDescriptor annotation,
                                                           ExecutableDescriptor method,
                                                           List<Parameter> parameters,
                                                           FormattingMessager messager) {
        for ( Parameter parameter : parameters ) {
            if ( parameter.isSourceParameter() ) {
                // source parameter is a valid parameter for a property condition check
                continue;
            }

            if ( parameter.isMappingContext() ) {
                // mapping context parameter is a valid parameter for a property condition check
                continue;
            }

            if ( parameter.isTargetType() ) {
                // target type parameter is a valid parameter for a property condition check
                continue;
            }

            if ( parameter.isMappingTarget() ) {
                // mapping target parameter is a valid parameter for a property condition check
                continue;
            }

            if ( parameter.isSourcePropertyName() ) {
                // source property name parameter is a valid parameter for a property condition check
                continue;
            }

            if ( parameter.isTargetPropertyName() ) {
                // target property name parameter is a valid parameter for a property condition check
                continue;
            }

            messager.printMessage( method, annotation, Message.CONDITION_PROPERTIES_INVALID_PARAMETER, parameter );
            return false;
        }
        return true;
    }

    private static boolean isBooleanReturnType(TypeDescriptor returnType) {
        if ( returnType == null ) {
            return false;
        }
        if ( returnType.isPrimitive() ) {
            return "boolean".equals( returnType.displayName() );
        }
        if ( returnType.kind() == LangTypeKind.DECLARED ) {
            return returnType.qualifiedName()
                .map( Boolean.class.getCanonicalName()::equals )
                .orElse( false );
        }
        return false;
    }
}
