val buildVersion: String by project
val dockerImagePrefix: String by project

plugins {
    application
    kotlin("jvm")
    id("com.palantir.docker")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("com.robert.api.ApplicationKt")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":utils"))
    implementation(project(":persistence"))
    implementation(project(":balancer_api:client"))
    implementation(project(":scaling_controller:client"))
    implementation(libs.ktor.jwt)
    implementation(libs.ktor.cors)
    implementation(libs.ktor.logging)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.exposed)
    implementation(libs.grpc.netty)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.protobuf)
}

docker {
    name = "$dockerImagePrefix/balancer-api:$buildVersion"
    setDockerfile(file("${project.rootDir}/docker/Dockerfile_kotlin"))
    noCache(true)
}

tasks.shadowJar {
    dependsOn("prepareStaticFiles")
    mergeServiceFiles() // https://github.com/grpc/grpc-java/issues/10853
}

tasks.getByName("dockerPrepare").dependsOn("setUpDockerContext")

tasks.register<Copy>("prepareStaticFiles") {
    destinationDir = file("${project.projectDir}/build/resources/main/ui")

    dependsOn(":balancer_ui:bundle")
    from("${project.rootDir}/balancer_ui/build")
}

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
