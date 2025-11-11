/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import java.util.List;
import java.util.stream.Stream;

import org.mapstruct.Mapper;

@Mapper
public interface StreamDescriptorMapper {

    List<PersonDto> map(Stream<Person> people);
}
