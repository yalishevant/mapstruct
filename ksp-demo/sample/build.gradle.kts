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

tasks.test {
    useJUnitPlatform()
}
