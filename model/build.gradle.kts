val logback_version: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("ch.qos.logback:logback-classic:$logback_version")
}
