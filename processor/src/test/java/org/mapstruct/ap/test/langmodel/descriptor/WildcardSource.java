/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import java.util.List;

public class WildcardSource {

    private List<? extends Person> people;

    public List<? extends Person> getPeople() {
        return people;
    }

    public void setPeople(List<? extends Person> people) {
        this.people = people;
    }
}
