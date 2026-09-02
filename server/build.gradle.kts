plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("dev.repost.server.ApplicationKt")
}

dependencies {
    implementation(project(":admin-api"))
    implementation(project(":public-api"))

    implementation(project(":persistence"))

    implementation("io.ktor:ktor-server-core:${property("ktorVersion")}")
    implementation("io.ktor:ktor-server-netty:${property("ktorVersion")}")
    implementation("io.ktor:ktor-server-content-negotiation:${property("ktorVersion")}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${property("ktorVersion")}")
    implementation("io.ktor:ktor-server-status-pages:${property("ktorVersion")}")
    implementation("io.github.smiley4:ktor-openapi:${property("ktorOpenApiVersion")}")
    implementation("io.github.smiley4:schema-kenerator-serialization:${property("schemaKeneratorVersion")}")
    implementation("ch.qos.logback:logback-classic:${property("logbackVersion")}")

    testImplementation("io.ktor:ktor-server-test-host:${property("ktorVersion")}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
}

tasks.test {
    useJUnitPlatform()
}
