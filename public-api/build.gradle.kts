plugins {
    kotlin("jvm")
}

dependencies {
    implementation("io.ktor:ktor-server-core:${property("ktorVersion")}")
}

kotlin {
    jvmToolchain(25)
}
