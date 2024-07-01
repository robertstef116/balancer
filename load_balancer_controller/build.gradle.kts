plugins {
    application
    kotlin("jvm")
//    id("com.palantir.docker")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":model"))
    implementation(project(":utils"))
    implementation(project(":scaling_controller:client"))
    implementation(libs.kotlin.coroutines)
    implementation(libs.koin.core)
    implementation(libs.grpc.netty)
    implementation(libs.slf4j)
}