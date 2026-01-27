package org.mapstruct.demo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MapperTest {

    @Test
    fun `PersonMapper should map all fields`() {
        val mapper = PersonMapperImpl()
        val person = Person(
            firstName = "John",
            lastName = "Doe",
            age = 30,
            email = "john@example.com"
        )

        val dto = mapper.toDto(person)

        assertThat(dto.firstName).isEqualTo("John")
        assertThat(dto.lastName).isEqualTo("Doe")
        assertThat(dto.age).isEqualTo(30)
        assertThat(dto.email).isEqualTo("john@example.com")
    }

    @Test
    fun `PersonMapper should handle null email`() {
        val mapper = PersonMapperImpl()
        val person = Person(
            firstName = "Jane",
            lastName = "Smith",
            age = 25,
            email = null
        )

        val dto = mapper.toDto(person)

        assertThat(dto.email).isNull()
    }

    @Test
    fun `AddressMapper should map with renamed fields`() {
        val mapper = AddressMapperImpl()
        val address = Address(
            street = "123 Main St",
            city = "New York",
            zipCode = "10001",
            country = "USA"
        )

        val dto = mapper.toDto(address)

        assertThat(dto.street).isEqualTo("123 Main St")
        assertThat(dto.city).isEqualTo("New York")
        assertThat(dto.postalCode).isEqualTo("10001")
        assertThat(dto.countryName).isEqualTo("USA")
    }
}
