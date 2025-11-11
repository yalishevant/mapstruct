/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import java.util.Collections;
import java.util.Set;

import org.mapstruct.ap.langmodel.AnnotationGemsCapability;

public final class MissingAnnotationGemsLangModelContextFactory
    extends AbstractMissingCapabilityLangModelContextFactory {

    @Override
    protected Set<Class<?>> missingCapabilities() {
        return Collections.singleton( AnnotationGemsCapability.class );
    }

    @Override
    public String backendId() {
        return "missing-annotation-gems";
    }
}
