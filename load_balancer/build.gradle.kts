val logback_version: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":utils"))
    implementation(kotlin("stdlib"))
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    implementation("org.apache.httpcomponents:httpcore:4.4.15")
    implementation("org.postgresql:postgresql:42.3.3")
}
