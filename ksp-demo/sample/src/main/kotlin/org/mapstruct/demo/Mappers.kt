package org.mapstruct.demo

import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * Simple mapper - same field names.
 */
@Mapper
interface PersonMapper {
    fun toDto(person: Person): PersonDto
}

/**
 * Mapper with field renaming.
 */
@Mapper
interface AddressMapper {
    fun toDto(address: Address): AddressDto
}
