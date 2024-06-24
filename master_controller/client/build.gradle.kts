plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":model"))
}

//tasks.named<Jar>("jar") {
//    archiveBaseName.set("master-controller-client")
//    archiveVersion.set(project.version.toString())
//}
