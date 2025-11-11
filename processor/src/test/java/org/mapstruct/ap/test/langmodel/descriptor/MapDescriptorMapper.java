/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import org.mapstruct.Mapper;

@Mapper
public interface MapDescriptorMapper {

    MapTarget map(MapSource source);

    default PersonDto map(Person person) {
        if ( person == null ) {
            return null;
        }

        return new PersonDto( person.getName() );
    }
}
