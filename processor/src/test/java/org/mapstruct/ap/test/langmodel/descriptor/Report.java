/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import java.util.ArrayList;
import java.util.List;

public class Report {

    private final List<PersonDto> attendees;

    private Report(Builder builder) {
        this.attendees = builder.attendees;
    }

    public List<PersonDto> getAttendees() {
        return attendees;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<PersonDto> attendees = new ArrayList<>();

        public Builder attendees(List<PersonDto> attendees) {
            this.attendees = new ArrayList<>( attendees );
            return this;
        }

        public Report build() {
            return new Report( this );
        }
    }
}
