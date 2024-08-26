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
    implementation(project(":model"))
    implementation(project(":utils"))
    implementation(project(":scaling_controller:client"))
    implementation(project(":scaling_controller:api"))
    implementation(libs.slf4j)
    implementation(libs.koin.core)
    implementation(libs.kotlin.coroutines)
    implementation(libs.grpc.netty)
    implementation(libs.bundles.docker)
    implementation(libs.oshi)
}

docker {
    name = "$dockerImagePrefix/balancer-worker:$buildVersion"
    setDockerfile(file("${project.rootDir}/docker/Dockerfile_kotlin"))
    noCache(true)
}

tasks.shadowJar {
    mergeServiceFiles() // https://github.com/grpc/grpc-java/issues/10853
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
