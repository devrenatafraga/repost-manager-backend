plugins {
    kotlin("jvm")
}

dependencies {
    implementation("io.ktor:ktor-server-core:${property("ktorVersion")}")
    implementation("io.github.smiley4:ktor-openapi:${property("ktorOpenApiVersion")}")
}
