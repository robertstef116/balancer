val ktorVersion: String by project
val kotlinVersion: String by project
val buildVersion: String by project
val dockerImagePrefix: String by project
val dockerJdkBaseVersion: String by project

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
    implementation("commons-validator:commons-validator:1.7")
    implementation("org.postgresql:postgresql:42.5.1")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:2.2.4")
    implementation("io.ktor:ktor-server-sessions-jvm:2.2.4")
    implementation("io.ktor:ktor-server-netty-jvm:2.2.4")
}

tasks.register<Task>("prepareKotlinBuildScriptModel"){}

tasks.register<Copy>("prepareStaticFiles") {
    destinationDir=file("${project.projectDir}/build/resources/main/ui")

    dependsOn(":balancer-ui:bundle")
    from("${project.rootDir}/balancer-ui/build")
}

tasks.getByName("shadowJar").dependsOn("prepareStaticFiles")

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
    name = "$dockerImagePrefix/balancer-master:$buildVersion"
    buildArgs(mapOf("PARENT_VERSION" to dockerJdkBaseVersion))
    setDockerfile(file("${project.rootDir}/docker/Dockerfile_kotlin"))
    noCache(true)
}
