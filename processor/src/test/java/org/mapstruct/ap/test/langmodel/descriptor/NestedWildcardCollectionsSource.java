/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import java.util.List;

/**
 * Source DTO with nested wildcard collections to validate descriptor-first collection handling.
 */
public class NestedWildcardCollectionsSource {

    private List<List<? extends Person>> groups;

    public List<List<? extends Person>> getGroups() {
        return groups;
    }

    public void setGroups(List<List<? extends Person>> groups) {
        this.groups = groups;
    }
}
