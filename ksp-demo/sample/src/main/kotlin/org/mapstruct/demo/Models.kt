package org.mapstruct.demo

/**
 * Source data class.
 * Kotlin data classes generate getXxx() methods when compiled.
 */
data class Person(
    val firstName: String,
    val lastName: String,
    val age: Int,
    val email: String?
)

/**
 * Target DTO - needs var properties and default constructor for Java-style setters.
 */
class PersonDto {
    var firstName: String = ""
    var lastName: String = ""
    var age: Int = 0
    var email: String? = null
}

/**
 * Source with different field names.
 */
data class Address(
    val street: String,
    val city: String,
    val zipCode: String,
    val country: String
)

/**
 * Target with renamed fields - mutable for Java-style setting.
 */
class AddressDto {
    var street: String = ""
    var city: String = ""
    var postalCode: String = ""
    var countryName: String = ""
}
