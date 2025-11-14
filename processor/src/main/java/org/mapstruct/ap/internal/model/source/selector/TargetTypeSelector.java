/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source.selector;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.source.Method;
import org.mapstruct.ap.internal.langmodel.api.LangTypes;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * This selector selects a best match based on the result type.
 * <p>
 * Suppose: Sedan -&gt; Car -&gt; Vehicle, MotorCycle -&gt; Vehicle By means of this selector one can pinpoint the exact
 * desired return type (Sedan, Car, MotorCycle, Vehicle)
 *
 * @author Sjaak Derksen
 */
public class TargetTypeSelector implements MethodSelector {

    private final LangTypes langTypes;

    public TargetTypeSelector(LangTypes langTypes) {
        this.langTypes = langTypes;
    }

    @Override
    public <T extends Method> List<SelectedMethod<T>> getMatchingMethods(List<SelectedMethod<T>> methods,
                                                                         SelectionContext context) {
        SelectionCriteria criteria = context.getSelectionCriteria();

        TypeDescriptor qualifyingType = criteria.getQualifyingResultType();
        if ( qualifyingType != null && !qualifyingType.isVoid() && !criteria.isLifecycleCallbackRequired() ) {

            List<SelectedMethod<T>> candidatesWithQualifyingTargetType =
                new ArrayList<>( methods.size() );

            for ( SelectedMethod<T> method : methods ) {
                Type resultType = method.getMethod().getResultType();
                TypeDescriptor resultTypeDescriptor = resultType != null ? resultType.getTypeDescriptor() : null;
                if ( resultTypeDescriptor != null && langTypes.isSameType( qualifyingType, resultTypeDescriptor ) ) {
                    candidatesWithQualifyingTargetType.add( method );
                }
            }

            return candidatesWithQualifyingTargetType;
        }
        else {
            return methods;
        }
    }
}
