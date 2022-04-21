
buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version  "1.6.10" apply false
}

allprojects {
    group = "com.robert"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "1.8"
        }
    }
}
