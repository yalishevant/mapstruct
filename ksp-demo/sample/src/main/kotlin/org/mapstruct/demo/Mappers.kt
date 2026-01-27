package org.mapstruct.demo

import org.mapstruct.ksp.Mapper
import org.mapstruct.ksp.Mapping

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
    @Mapping(source = "zipCode", target = "postalCode")
    @Mapping(source = "country", target = "countryName")
    fun toDto(address: Address): AddressDto
}
