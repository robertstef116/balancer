plugins {
    application
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("com.robert.test.performance.MainKt")
}

dependencies {
    implementation(project(":utils"))
    implementation(libs.logback)
    implementation(libs.kotlin.coroutines)
    implementation(libs.ktor.client)
}
