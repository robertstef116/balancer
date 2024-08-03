val ktorVersion: String by project
val kotlinVersion: String by project
val koinVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":model"))
    implementation("com.typesafe:config:1.4.2")
    implementation("org.reflections:reflections:0.10.2")

    implementation(libs.koin.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentnegotiation)
    implementation(libs.ktor.client.serialization)
    implementation(libs.logback)

    runtimeOnly(libs.kotlin.reflect) //for logger
}
