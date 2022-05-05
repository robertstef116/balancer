val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("com.palantir.docker")
}

application {
    mainClass.set("com.robert.ApplicationKt")
}

docker {
    name = "docker.io/r0bb3rt17/test-image:latest"
    tag("latest", "docker.io/r0bb3rt17/test-image:latest")
    files("./build/libs/test_image-all.jar")
}

tasks.dockerPrepare {
    dependsOn("shadowJar")
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

tasks.register<Task>("prepareKotlinBuildScriptModel") {}
