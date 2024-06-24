val ktorVersion: String by project
val buildVersion: String by project
val dockerImagePrefix: String by project
val dockerJdkBaseVersion: String by project

plugins {
    application
    kotlin("jvm")
//    id("com.palantir.docker")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("com.robert.MainKt")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":utils"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.postgresql:postgresql:42.5.4")
    implementation("io.ktor:ktor-client-core-jvm:2.2.4")
    implementation("io.ktor:ktor-client-cio-jvm:2.2.4")
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

//tasks.dockerPrepare {
//    dependsOn("setUpDockerContext")
//}

//docker {
//    name = "$dockerImagePrefix/load_balancer:$buildVersion"
//    buildArgs(mapOf("PARENT_VERSION" to dockerJdkBaseVersion))
//    setDockerfile(file("${project.rootDir}/docker/Dockerfile_kotlin"))
//    noCache(true)
//}
