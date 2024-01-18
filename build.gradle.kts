
val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val logstash_encoder_version: String by project
val tokensupport_version: String by project
val mockoauth_version: String by project
val dab_common_version: String by project

val prometeus_version: String by project
plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
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
    implementation("io.ktor:ktor-server-config-yaml:2.3.7")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-client-logging-jvm:2.3.7")
    implementation("io.ktor:ktor-client-encoding:2.3.7")
    testImplementation("io.ktor:ktor-server-tests-jvm")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp-jvm:2.3.7")

    implementation("no.nav.poao.dab:ktor-oauth-client:$dab_common_version")

    implementation("no.nav.security:token-validation-ktor-v2:$tokensupport_version")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometeus_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstash_encoder_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.6")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("no.nav.security:mock-oauth2-server:$mockoauth_version")
    testImplementation("io.ktor:ktor-client-mock:$ktor_version")
}
