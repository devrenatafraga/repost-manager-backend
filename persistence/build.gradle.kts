plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("dev.repost.db.FlywayMigrateCliKt")
}

dependencies {
    implementation("org.flywaydb:flyway-core:${property("flywayVersion")}")
    implementation("org.flywaydb:flyway-database-postgresql:${property("flywayVersion")}")
    implementation("org.postgresql:postgresql:${property("postgresqlVersion")}")
    implementation("org.jetbrains.exposed:exposed-core:${property("exposedVersion")}")
    implementation("org.jetbrains.exposed:exposed-dao:${property("exposedVersion")}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${property("exposedVersion")}")
    implementation("org.jetbrains.exposed:exposed-java-time:${property("exposedVersion")}")
    implementation("org.mindrot:jbcrypt:${property("jbcryptVersion")}")

    testImplementation("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
    testImplementation("org.testcontainers:postgresql:${property("testcontainersVersion")}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("flywayMigrate") {
    group = "database"
    description = "Apply Flyway migrations (requires DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.repost.db.FlywayMigrateCliKt")
}
