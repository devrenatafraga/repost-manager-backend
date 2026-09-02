plugins {
    kotlin("jvm")
    id("org.flywaydb.flyway") version "11.1.0"
}

flyway {
    url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/repost"
    user = System.getenv("DATABASE_USER") ?: "repost"
    password = System.getenv("DATABASE_PASSWORD") ?: "repost"
    locations = arrayOf("classpath:db/migration")
}

dependencies {
    implementation("org.flywaydb:flyway-core:${property("flywayVersion")}")
    implementation("org.flywaydb:flyway-database-postgresql:${property("flywayVersion")}")
    implementation("org.postgresql:postgresql:${property("postgresqlVersion")}")

    testImplementation("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
    testImplementation("org.testcontainers:postgresql:${property("testcontainersVersion")}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
}

tasks.test {
    useJUnitPlatform()
}
