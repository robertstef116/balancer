plugins {
    application
    kotlin("jvm")
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
