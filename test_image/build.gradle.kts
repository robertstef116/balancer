val ktorVersion: String by project
val kotlinVersion: String by project

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
    implementation(project(":model"))
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
}

tasks.register<Task>("prepareKotlinBuildScriptModel") {}
