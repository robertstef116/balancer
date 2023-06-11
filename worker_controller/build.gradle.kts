val ktorVersion: String by project
val buildVersion: String by project
val kotlinVersion: String by project
val dockerImagePrefix: String by project
val dockerJdkBaseVersion: String by project
val logbackVersion: String by project

plugins {
    application
    kotlin("jvm")
    id("com.palantir.docker")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("com.robert.ApplicationKt")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":utils"))
    implementation("com.github.oshi:oshi-core:6.4.0") {
        exclude("org.slf4j", "slf4j-api")
    }
    implementation("com.spotify:docker-client:8.16.0"){
        exclude("org.slf4j", "slf4j-api")
    }
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
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

tasks.dockerPrepare {
    dependsOn("setUpDockerContext")
}

docker {
    name = "$dockerImagePrefix/balancer-worker:$buildVersion"
    buildArgs(mapOf("PARENT_VERSION" to dockerJdkBaseVersion))
    setDockerfile(file("${project.rootDir}/docker/Dockerfile_kotlin"))
    noCache(true)
}
