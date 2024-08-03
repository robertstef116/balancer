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
            version("ktor", "2.3.12")
            version("docker", "3.4.0")

            library("grpc-protobuf", "io.grpc", "grpc-protobuf").versionRef("grpc")
            library("grpc-netty", "io.grpc", "grpc-netty-shaded").versionRef("grpc")
            library("grpc-kotlinStub", "io.grpc:grpc-kotlin-stub:1.4.1")
            bundle(
                "grpc", listOf(
                    "grpc-protobuf",
                    "grpc-kotlinStub"
                )
            )

            library("exposed-core", "org.jetbrains.exposed", "exposed-core").versionRef("exposed")
            library("exposed-dao", "org.jetbrains.exposed", "exposed-dao").versionRef("exposed")
            library("exposed-jdbc", "org.jetbrains.exposed", "exposed-jdbc").versionRef("exposed")
            library("hikari", "com.zaxxer:HikariCP:5.1.0")
            bundle(
                "exposed", listOf(
                    "exposed-core",
                    "exposed-dao",
                    "exposed-jdbc",
                    "hikari"
                )
            )

            library("ktor-client", "io.ktor", "ktor-client-core-jvm").versionRef("ktor")
            library("ktor-client-cio", "io.ktor", "ktor-client-cio-jvm").versionRef("ktor")
            library("ktor-client-contentnegotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor")
            library("ktor-client-serialization", "io.ktor", "ktor-serialization-kotlinx-json").versionRef("ktor")
            library("ktor-statusPages", "io.ktor", "ktor-server-status-pages").versionRef("ktor")
            library("ktor-contentNegotiation", "io.ktor", "ktor-server-content-negotiation").versionRef("ktor")
            library("ktor-gson", "io.ktor", "ktor-serialization-gson").versionRef("ktor")
            library("ktor-netty", "io.ktor", "ktor-server-netty-jvm").versionRef("ktor")
            library("ktor-jwt", "io.ktor", "ktor-server-auth-jwt").versionRef("ktor")
            library("ktor-logging", "io.ktor", "ktor-server-call-logging").versionRef("ktor")
            library("ktor-cors", "io.ktor", "ktor-server-cors").versionRef("ktor")
            bundle(
                "ktor", listOf(
                    "ktor-statusPages",
                    "ktor-contentNegotiation",
                    "ktor-gson",
                    "ktor-netty",
                )
            )

            library("docker-java", "com.github.docker-java", "docker-java").versionRef("docker")
            library("docker-transport", "com.github.docker-java", "docker-java-transport-httpclient5").versionRef("docker")
            bundle(
                "docker", listOf(
                    "docker-java",
                    "docker-transport",
                )
            )

            library("protobuf", "com.google.protobuf:protobuf-kotlin:3.25.3")
            library("koin-core", "io.insert-koin", "koin-core").versionRef("koin")
            library("koin-ktor", "io.insert-koin", "koin-ktor").versionRef("koin")
            library("logback", "ch.qos.logback:logback-classic:1.5.2")
            library("kotlin-reflect", "org.jetbrains.kotlin:kotlin-reflect:1.9.22")
            library("kotlin-coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            library("slf4j", "org.slf4j:slf4j-api:2.0.12")
            library("oshi", "com.github.oshi:oshi-core:6.6.1")

            library("protoc", "com.google.protobuf:protoc:3.25.3")
            library("protoc-gen-java", "io.grpc:protoc-gen-grpc-java:1.62.2")

            library("postgresql", "org.postgresql", "postgresql").versionRef("postgresql")
        }
    }
}

rootProject.name = "balancer"

include("model")
include("utils")
include("worker_controller")
include("test_image")
include("performance_tester")
include("persistence")
include("balancer_ui")
include("scaling_controller")
include("scaling_controller:client")
include("scaling_controller:api")
include("balancer_api")
include("balancer_api:client")
include("load_balancer")
