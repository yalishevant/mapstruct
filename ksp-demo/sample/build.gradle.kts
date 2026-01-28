plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":processor"))
    ksp(project(":processor"))

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

tasks.test {
    useJUnitPlatform()
}
