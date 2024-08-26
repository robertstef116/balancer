val buildVersion: String by project
val dockerImagePrefix: String by project

plugins {
    application
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("com.palantir.docker")
}

application {
    mainClass.set("com.robert.test.image.ApplicationKt")
}

dependencies {
    implementation(project(":utils"))
    implementation(libs.logback)
    implementation(libs.bundles.ktor)
}

docker {
    name = "$dockerImagePrefix/test-image:latest"
    setDockerfile(file("${project.rootDir}/docker/Dockerfile_kotlin"))
    noCache(true)
}

tasks.getByName("dockerPrepare").dependsOn("setUpDockerContext")

tasks.register<Copy>("setUpDockerContext") {
    val contextPath = "${project.projectDir}/build/docker"
    destinationDir = file(contextPath)

    dependsOn("shadowJar")
    from("${project.projectDir}/build/libs") {
        into("app")
        include("*-all.jar")
    }
}
