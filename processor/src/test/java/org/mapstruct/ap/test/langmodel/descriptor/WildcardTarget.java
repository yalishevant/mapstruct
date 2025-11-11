/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import java.util.List;

public class WildcardTarget {

    private List<PersonDto> people;

    public List<PersonDto> getPeople() {
        return people;
    }

    public void setPeople(List<PersonDto> people) {
        this.people = people;
    }
}
