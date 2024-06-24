plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":utils"))
    implementation(libs.logback)
    implementation(libs.bundles.exposed)
}
