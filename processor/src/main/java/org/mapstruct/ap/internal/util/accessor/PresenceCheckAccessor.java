/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util.accessor;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;

/**
 * @author Filip Hrisafov
 */
public interface PresenceCheckAccessor {

    String getPresenceCheckSuffix();

    static PresenceCheckAccessor methodInvocation(ElementDescriptor element) {
        String simpleName = element != null && element.simpleName() != null
            ? element.simpleName().content()
            : "";
        return suffix( "." + simpleName + "()" );
    }

    static PresenceCheckAccessor mapContainsKey(String propertyName) {
        return suffix( ".containsKey( \"" + propertyName + "\" )" );
    }

    static PresenceCheckAccessor suffix(String suffix) {
        return () -> suffix;
    }
}
