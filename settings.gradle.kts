plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("koin", "3.5.3")
            version("grpc", "1.62.2")
            version("exposed", "0.48.0")
            version("postgresql", "42.7.3")

            library("grpc-protobuf","io.grpc", "grpc-protobuf").versionRef("grpc")
            library("grpc-netty","io.grpc", "grpc-netty").versionRef("grpc")
            library("grpc-kotlin-stub", "io.grpc:grpc-kotlin-stub:1.4.1")
            bundle("grpc", listOf(
                "grpc-protobuf",
                "grpc-kotlin-stub"
            ))

            library("exposed-core","org.jetbrains.exposed", "exposed-core").versionRef("exposed")
            library("exposed-dao","org.jetbrains.exposed", "exposed-dao").versionRef("exposed")
            library("exposed-jdbc","org.jetbrains.exposed", "exposed-jdbc").versionRef("exposed")
            library("hikari", "com.zaxxer:HikariCP:5.1.0")
            bundle("exposed", listOf(
                "exposed-core",
                "exposed-dao",
                "exposed-jdbc",
                "hikari"
            ))

            library("protobuf", "com.google.protobuf:protobuf-kotlin:3.25.3")
            library("koin-core","io.insert-koin", "koin-core").versionRef("koin")
            library("koin-ktor","io.insert-koin", "koin-ktor").versionRef("koin")
            library("logback", "ch.qos.logback:logback-classic:1.5.2")
            library("kotlin-reflect", "org.jetbrains.kotlin:kotlin-reflect:1.9.22")
            library("kotlin-coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            library("slf4j", "org.slf4j:slf4j-api:2.0.12")

            library("protoc", "com.google.protobuf:protoc:3.25.3")
            library("protoc-gen-java", "io.grpc:protoc-gen-grpc-java:1.62.2")

            library("postgresql", "org.postgresql", "postgresql").versionRef("postgresql")
//            library("protoc-gen-kotlin", "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar")
//            library("protoc-gen-kotlin", "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar")
        }
    }
}

rootProject.name = "balancer"

include("model")
include("utils")
include("worker_controller")
include("master_controller")
include("load_balancer")
include("test_image")
include("performance_tester")
include("balancer-ui")
include("persistence")
include("scaling_controller")
include("scaling_controller:client")
include("scaling_controller:api")
include("master_controller")
include("master_controller:client")
include("load_balancer_controller")
