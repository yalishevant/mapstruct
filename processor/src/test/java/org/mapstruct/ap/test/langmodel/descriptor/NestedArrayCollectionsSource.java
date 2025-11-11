/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import java.util.List;

/**
 * Source DTO with nested array structures wrapped in collections.
 */
public class NestedArrayCollectionsSource {

    private List<? extends Person[]> matrix;

    public List<? extends Person[]> getMatrix() {
        return matrix;
    }

    public void setMatrix(List<? extends Person[]> matrix) {
        this.matrix = matrix;
    }
}
