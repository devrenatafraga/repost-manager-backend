plugins {
    kotlin("jvm")
}

dependencies {
    implementation("io.ktor:ktor-server-core:${property("ktorVersion")}")
}
