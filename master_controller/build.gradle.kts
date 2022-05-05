val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm")
}

//group = "com.robert"
//version = "0.0.1"
//application {
//    mainClass.set("com.robert.ApplicationKt")
//
//    val isDevelopment: Boolean = project.ext.has("development")
//    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
//}

dependencies {
    implementation(project(":model"))
    implementation(project(":utils"))
    implementation("commons-validator:commons-validator:1.7")
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
    implementation("io.ktor:ktor-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-sessions:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}

tasks.register<Task>("prepareKotlinBuildScriptModel"){}
