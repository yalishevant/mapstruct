plugins {
    kotlin("jvm")
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.27")
    // JavaPoet for generating Java source code
    implementation("com.squareup:javapoet:1.13.0")
}

kotlin {
    jvmToolchain(17)
}
