val buildVersion: String by project

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.9.22" apply false
    id("io.ktor.plugin") version "2.3.0" apply false
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("com.palantir.docker") version "0.33.0" apply false
    id("com.github.node-gradle.node") version "3.5.0" apply false
    kotlin("plugin.serialization") version "1.8.21" apply false
    id("com.google.protobuf") version "0.9.4" apply false
}

allprojects {
    group = "com.robert"
    version = buildVersion

    repositories {
        mavenCentral()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }
}
