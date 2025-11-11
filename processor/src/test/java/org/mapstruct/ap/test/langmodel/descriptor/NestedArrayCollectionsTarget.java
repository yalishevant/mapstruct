/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import java.util.List;

/**
 * Target DTO for {@link NestedArrayCollectionsSource} mapping validation.
 */
public class NestedArrayCollectionsTarget {

    private List<PersonDto[]> matrix;

    public List<PersonDto[]> getMatrix() {
        return matrix;
    }

    public void setMatrix(List<PersonDto[]> matrix) {
        this.matrix = matrix;
    }
}
