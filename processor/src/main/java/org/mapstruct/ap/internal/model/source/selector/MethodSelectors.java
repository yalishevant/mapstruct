/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source.selector;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.model.source.Method;
import org.mapstruct.ap.internal.option.Options;
import org.mapstruct.ap.internal.util.FormattingMessager;

/**
 * Applies all known {@link MethodSelector}s in order.
 *
 * @author Sjaak Derksen
 */
public class MethodSelectors {

    private final List<MethodSelector> selectors;

    public MethodSelectors(TypeFactory typeFactory, FormattingMessager messager, Options options) {
        List<MethodSelector> selectorList = new ArrayList<>();
        selectorList.add( new MethodFamilySelector() );
        selectorList.add( new TypeSelector( messager ) );
        selectorList.add( new QualifierSelector( typeFactory ) );
        selectorList.add( new TargetTypeSelector( typeFactory.langTypes() ) );
        selectorList.add( new JavaxXmlElementDeclSelector( typeFactory.langElements(), typeFactory.langTypes() ) );
        selectorList.add( new JakartaXmlElementDeclSelector( typeFactory.langElements(), typeFactory.langTypes() ) );
        selectorList.add( new InheritanceSelector() );
        if ( options != null && !options.isDisableLifecycleOverloadDeduplicateSelector() ) {
            selectorList.add( new LifecycleOverloadDeduplicateSelector() );
        }

        selectorList.add( new CreateOrUpdateSelector() );
        selectorList.add( new SourceRhsSelector() );
        selectorList.add( new FactoryParameterSelector() );
        selectorList.add( new MostSpecificResultTypeSelector() );
        this.selectors = selectorList;
    }

    /**
     * Selects those methods which match the given types and other criteria
     *
     * @param <T> either SourceMethod or BuiltInMethod
     * @param methods list of available methods
     * @param context the selection context that should be used in the matching process
     * @return list of methods that passes the matching process
     */
    public <T extends Method> List<SelectedMethod<T>> getMatchingMethods(List<T> methods,
                                                                         SelectionContext context) {

        List<SelectedMethod<T>> candidates = new ArrayList<>( methods.size() );
        for ( T method : methods ) {
            candidates.add( new SelectedMethod<>( method ) );
        }

        for ( MethodSelector selector : selectors ) {
            candidates = selector.getMatchingMethods( candidates, context );
        }
        return candidates;
    }
}
