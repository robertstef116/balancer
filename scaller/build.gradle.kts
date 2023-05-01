val ktorVersion: String by project
val koinVersion: String by project

plugins {
    application
    kotlin("jvm")
}

application {
    mainClass.set("com.robert.MainKt")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":utils"))
    implementation("org.slf4j:slf4j-simple:2.0.6")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("org.postgresql:postgresql:42.5.1")
}
