/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import org.mapstruct.ap.internal.langmodel.api.BuilderIntrospectorContext;
import org.mapstruct.ap.internal.langmodel.spi.BuilderIntrospector;

/**
 * Optional capability providing {@link BuilderIntrospector} access.
 */
public interface BuilderIntrospectorCapability {

    /**
     * Creates a builder introspector for the supplied context.
     *
     * @param context builder introspector context
     *
     * @return builder introspector implementation
     */
    BuilderIntrospector builderIntrospector(BuilderIntrospectorContext context);
}
