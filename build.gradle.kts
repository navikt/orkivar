val ktor_version = "3.0.1"
val kotlin_version = "2.0.21"
val logback_version = "1.4.12"
val logstash_encoder_version = "7.4"
val tokensupport_version = "5.0.24"
val mockoauth_version = "1.0.0"
val dab_common_version = "2024.11.14-10.46.174740baf5c7"
val hikaricp_version = "6.1.0"
val embedded_postgres_version = "2.0.7"
val postgres_driver_version = "42.7.4"
val flyway_version = "9.22.3"
val embeddedPostgresBinaries_version = "16.1.1"
val exposed_version = "0.56.0"
val kotest_version = "5.8.0"
val prometeus_version = "1.6.3"

plugins {
    kotlin("jvm") version "2.0.21"
    id("io.ktor.plugin") version "3.0.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

group = "dab.poao.nav.no"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-client-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-encoding:$ktor_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktor_version")

    implementation("no.nav.poao.dab:ktor-oauth-client:$dab_common_version")

    implementation("no.nav.security:token-validation-ktor-v3:$tokensupport_version")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometeus_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstash_encoder_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("com.zaxxer:HikariCP:$hikaricp_version")
    implementation("org.postgresql:postgresql:$postgres_driver_version")
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("io.zonky.test.postgres:embedded-postgres-binaries-bom:$embeddedPostgresBinaries_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")

    testImplementation("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation("io.kotest:kotest-assertions-core:$kotest_version")
    testImplementation("io.kotest:kotest-assertions-json:$kotest_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("no.nav.security:mock-oauth2-server:$mockoauth_version")
    testImplementation("io.ktor:ktor-client-mock:$ktor_version")
    testImplementation("io.zonky.test:embedded-postgres:$embedded_postgres_version")
}
