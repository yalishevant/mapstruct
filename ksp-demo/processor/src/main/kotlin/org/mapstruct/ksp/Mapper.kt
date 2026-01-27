package org.mapstruct.ksp

/**
 * Marks an interface as a mapper.
 * The KSP processor will generate an implementation class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Mapper

/**
 * Specifies a mapping between source and target property.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Mapping(
    val source: String,
    val target: String
)
