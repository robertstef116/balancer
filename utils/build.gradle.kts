val logback_version: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":model"))
    implementation(kotlin("stdlib"))
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.typesafe:config:1.4.1")
}
