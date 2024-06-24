val buildVersion: String by project

plugins {
    application
    kotlin("jvm")
//    id("com.palantir.docker")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("com.robert.ApplicationKt")
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
    implementation("com.github.docker-java:docker-java:3.3.6")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.6")
    implementation("com.github.oshi:oshi-core:6.5.0") {
        exclude("org.slf4j", "slf4j-api")
    }
//    implementation("com.spotify:docker-client:8.16.0"){
//        exclude("org.slf4j", "slf4j-api")
//    }
}

tasks.register<Task>("prepareKotlinBuildScriptModel"){}

tasks.register<Copy>("setUpDockerContext") {
    val contextPath = "${project.projectDir}/build/docker"
    destinationDir=file(contextPath)

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
//
//tasks.dockerPrepare {
//    dependsOn("setUpDockerContext")
//}

//docker {
//    name = "$dockerImagePrefix/balancer-worker:$buildVersion"
//    buildArgs(mapOf("PARENT_VERSION" to dockerJdkBaseVersion))
//    setDockerfile(file("${project.rootDir}/docker/Dockerfile_kotlin"))
//    noCache(true)
//}
