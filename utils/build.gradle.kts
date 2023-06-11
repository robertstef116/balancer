val ktorVersion: String by project
val kotlinVersion: String by project
val koinVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":model"))
    implementation(kotlin("stdlib"))
    implementation("com.typesafe:config:1.4.2")
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}
