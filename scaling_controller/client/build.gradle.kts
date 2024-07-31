plugins {
    kotlin("jvm")
}

group = "com.robert.scaling"

dependencies {
    implementation(project(":model"))
    implementation(project(":utils"))
    implementation(project(":scaling_controller:api"))
    implementation(libs.slf4j)
    implementation(libs.bundles.grpc)
    implementation(libs.kotlin.coroutines)
}
