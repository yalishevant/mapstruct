/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.lang.model.element.Element;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;

final class JavaxElementUnwrapper {

    private JavaxElementUnwrapper() {
    }

    static Element unwrapElement(ElementDescriptor descriptor) {
        if ( descriptor instanceof JavaxElementDescriptor ) {
            return ( (JavaxElementDescriptor) descriptor ).element();
        }
        throw new IllegalArgumentException( "Expected Javax-backed descriptor backed by javax.lang.model" );
    }
}
