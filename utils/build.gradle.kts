val ktorVersion: String by project
val kotlinVersion: String by project
val pulsarVersion: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":model"))
    implementation(kotlin("stdlib"))
    implementation("com.typesafe:config:1.4.2")
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("org.apache.pulsar:pulsar-client:$pulsarVersion")
    implementation("org.slf4j:slf4j-simple:2.0.6")
}
