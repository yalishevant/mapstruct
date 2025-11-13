/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.lang.model.element.Name;

import org.mapstruct.ap.internal.langmodel.descriptor.NameDescriptor;

final class JavaxNameDescriptor implements NameDescriptor {

    private final Name name;

    JavaxNameDescriptor(Name name) {
        this.name = name;
    }

    @Override
    public String content() {
        return name.toString();
    }
}
