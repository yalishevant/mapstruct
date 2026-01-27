plugins {
    kotlin("jvm") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}

allprojects {
    group = "org.mapstruct.ksp.demo"
    version = "1.0-SNAPSHOT"
}
