/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import java.util.List;

/**
 * Target DTO mirroring {@link NestedWildcardCollectionsSource} but with concrete DTO element types.
 */
public class NestedWildcardCollectionsTarget {

    private List<List<PersonDto>> groups;

    public List<List<PersonDto>> getGroups() {
        return groups;
    }

    public void setGroups(List<List<PersonDto>> groups) {
        this.groups = groups;
    }
}
