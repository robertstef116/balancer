val buildVersion: String by project
val dockerImagePrefix: String by project

plugins {
    application
    kotlin("jvm")
    id("com.palantir.docker")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("com.robert.MainKt")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":utils"))
    implementation(project(":model"))
    implementation(project(":scaling_controller:api"))
    implementation(project(":scaling_controller:client"))
    implementation(libs.slf4j)
    implementation(libs.postgresql)
    implementation(libs.koin.core)
    implementation(libs.kotlin.coroutines)
    implementation(libs.grpc.netty)
    implementation(libs.bundles.grpc)
    implementation(libs.bundles.exposed)
}

docker {
    name = "$dockerImagePrefix/balancer-scaling-controller:$buildVersion"
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

    doLast {
        val versionFile = file("${contextPath}/version.txt")
        if (!versionFile.exists()) versionFile.createNewFile()
        versionFile.writeText(buildVersion)
    }
}
