plugins {
    kotlin("jvm") version "2.1.21" apply false
    id("io.ktor.plugin") version "3.1.1" apply false
}

subprojects {
    group = "dev.repost"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(project.property("javaVersion").toString().toInt())
        }
    }
}
