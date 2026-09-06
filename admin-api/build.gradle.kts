plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":persistence"))
    implementation("io.ktor:ktor-server-core:${property("ktorVersion")}")
    implementation("io.ktor:ktor-server-auth:${property("ktorVersion")}")
    implementation("io.ktor:ktor-server-auth-jwt:${property("ktorVersion")}")
    implementation("io.github.smiley4:ktor-openapi:${property("ktorOpenApiVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("com.auth0:java-jwt:${property("auth0JwtVersion")}")
}
