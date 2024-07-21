import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("com.github.node-gradle.node")
}

tasks.register("bundle", NpmTask::class) {
    dependsOn("npmInstall")
    args.addAll(listOf("run", "build"))
}
