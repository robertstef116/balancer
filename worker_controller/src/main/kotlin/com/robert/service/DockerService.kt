package com.robert.service

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.core.InvocationBuilder.AsyncResultCallback
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.robert.Env
import com.robert.resources.DockerContainer
import com.robert.resources.DockerPortMapping
import com.robert.logger
import java.time.Duration
import java.util.*

class DockerService {
    companion object {
        val LOG by logger()

        const val MANAGED_CONTAINER_LABEL = "MANAGED_BY_BALANCER"
        const val WORKFLOW_ID_KEY_LABEL = "WORKFLOW_ID"
    }

    private var previousCpusUsage = mutableMapOf<String, Long>()

    private val managedContainersFilter = mapOf(MANAGED_CONTAINER_LABEL to "true")
    private val dockerConfig = DefaultDockerClientConfig
        .createDefaultConfigBuilder()
        .build()
    private val httpClient = ApacheDockerHttpClient.Builder()
        .dockerHost(dockerConfig.dockerHost)
        .sslConfig(dockerConfig.sslConfig)
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(Env.getLong("DOCKER_CONNECTION_TIMEOUT_SECONDS", 30L)))
        .responseTimeout(Duration.ofSeconds(Env.getLong("DOCKER_TIMEOUT_SECONDS", 45L)))
        .build()
    private val dockerClient = DockerClientImpl.getInstance(dockerConfig, httpClient);

    fun pingDocker() {
        dockerClient.pingCmd().exec()
    }

    fun getManagedContainers(): List<DockerContainer> {
        LOG.debug("Getting managed containers")
        val currentCpusUsage = mutableMapOf<String, Long>()
        val stats = dockerClient.listContainersCmd()
            .withLabelFilter(managedContainersFilter)
            .exec()
            .filter {
                it.labels?.get(WORKFLOW_ID_KEY_LABEL) != null && it.state.lowercase() == "running"
            }
            .map { container ->
                val containerId = container.id
                var containerDetails: Statistics
                AsyncResultCallback<Statistics>().use { callback ->
                    dockerClient.statsCmd(containerId).exec(callback)
                    containerDetails = callback.awaitResult()
                }

                val containerInfo = dockerClient.inspectContainerCmd(containerId).exec()
                val cpuLimit = containerInfo.hostConfig.cpuQuota ?: 0
                val previousCpuUsage = previousCpusUsage[containerId] ?: 0
                val cpuUsage = containerDetails.cpuStats.cpuUsage?.totalUsage?.minus(previousCpuUsage) ?: 0
                val cpuUsagePercentage = if (cpuUsage > 0 && previousCpuUsage > 0 && cpuLimit > 0) cpuUsage.toDouble() / (cpuLimit * 1_000_000) else 0.0
                currentCpusUsage[containerId] = containerDetails.cpuStats.cpuUsage?.totalUsage ?: 0

                val memoryLimit = containerDetails.memoryStats.limit ?: 0
                val memoryUsage = containerDetails.memoryStats.usage ?: 0
                val memoryUsagePercentage = if (memoryLimit > 0 && memoryUsage > 0) memoryUsage / memoryLimit.toDouble() else 0.0

                val workflowId = container.labels?.get(WORKFLOW_ID_KEY_LABEL)
                DockerContainer(
                    containerId,
                    UUID.fromString(workflowId),
                    cpuUsagePercentage,
                    memoryUsagePercentage,
                    container.ports
                        ?.filter { it.publicPort != null && it.privatePort != null }
                        ?.map { DockerPortMapping(it.publicPort!!, it.privatePort!!) }
                        ?: emptyList()
                )
            }
        previousCpusUsage = currentCpusUsage
        return stats
    }

    fun startContainer(workflowId: String, image: String, memoryLimit: Long, cpuLimit: Long, ports: List<Int>) {
        LOG.info("Pulling image {}", image)
        PullImageResultCallback().use {
            dockerClient.pullImageCmd(image)
                .exec<ResultCallback<PullResponseItem>>(it)
            it.awaitCompletion()
        }

        LOG.info("Creating container for workflow {}", workflowId)
        val exposedPortBindings = Ports()
        val exposedPorts = mutableListOf<ExposedPort>()
        ports.forEach { port ->
            val exposedPort = ExposedPort.tcp(port)
            exposedPorts.add(exposedPort)
            exposedPortBindings.bind(exposedPort, Ports.Binding.empty())
        }

        val hostConfig = HostConfig.newHostConfig()
            .withRestartPolicy(RestartPolicy.unlessStoppedRestart())
            .withPortBindings(exposedPortBindings)
            .withMemory(memoryLimit)
            .withCpuQuota(cpuLimit)
            .withCpuPeriod(100_000)
            .withPublishAllPorts(true)

        val container = dockerClient.createContainerCmd(image)
            .withExposedPorts(exposedPorts)
            .withHostConfig(hostConfig)
            .withLabels(
                mapOf(
                    MANAGED_CONTAINER_LABEL to "true",
                    WORKFLOW_ID_KEY_LABEL to workflowId
                )
            )
            .exec()

        LOG.info("Starting container for workflow {}", workflowId)
        dockerClient.startContainerCmd(container.id).exec();

        LOG.info("Container for workflow {} started", workflowId)
    }

    fun removeContainer(id: String) {
        LOG.info("Removing container with id {}", id)
        dockerClient.killContainerCmd(id).exec();
        dockerClient.removeContainerCmd(id).exec()
    }
}
