plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

val mapstructVersion = "1.7.0-SNAPSHOT"

dependencies {
    // MapStruct core annotations
    implementation("org.mapstruct:mapstruct:$mapstructVersion")

    // MapStruct KSP processor
    ksp("org.mapstruct:mapstruct-processor-ksp:$mapstructVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.24.2")
}

kotlin {
    jvmToolchain(17)

    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}

// Add generated Java sources to compilation
sourceSets.main {
    java.srcDir("build/generated/ksp/main/java")
}

ksp {
    arg("mapstruct.verbose", "true")
}

tasks.test {
    useJUnitPlatform()
}
