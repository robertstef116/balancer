val rabbitMqClientVersion: String by project
val koinVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":utils"))
    implementation("com.rabbitmq:amqp-client:$rabbitMqClientVersion")
//    implementation("org.slf4j:slf4j-simple:2.0.6")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.reflections:reflections:0.10.2")
    implementation("io.insert-koin:koin-core:$koinVersion")
}
