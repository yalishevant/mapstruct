/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util.accessor;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * @author Filip Hrisafov
 */
public interface ReadAccessor extends Accessor {

    String getReadValueSource();

    static ReadAccessor fromField(ElementDescriptor element, TypeDescriptor accessedType) {
        return new ReadDelegateAccessor( new ElementAccessor( element, accessedType ) ) {
            @Override
            public String getReadValueSource() {
                return getSimpleName();
            }
        };
    }

    static ReadAccessor fromRecordComponent(ElementDescriptor element, TypeDescriptor accessedType) {
        return new ReadDelegateAccessor( new ElementAccessor( element, accessedType, AccessorType.GETTER ) ) {
            @Override
            public String getReadValueSource() {
                return getSimpleName() + "()";
            }
        };
    }

    static ReadAccessor fromGetter(ElementDescriptor element, TypeDescriptor accessedType) {
        return new ReadDelegateAccessor( new ElementAccessor( element, accessedType, AccessorType.GETTER ) ) {
            @Override
            public String getReadValueSource() {
                return getSimpleName() + "()";
            }
        };
    }
}
