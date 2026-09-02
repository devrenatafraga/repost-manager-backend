plugins {
    kotlin("jvm") version "2.0.21" apply false
    id("io.ktor.plugin") version "3.0.3" apply false
}

subprojects {
    group = "dev.repost"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
