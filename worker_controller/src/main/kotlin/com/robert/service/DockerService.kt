package com.robert.service

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.core.InvocationBuilder.AsyncResultCallback
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.robert.Constants
import com.robert.Env
import com.robert.docker.DockerContainer
import com.robert.docker.DockerPortMapping
import com.robert.logger
import java.time.Duration
import java.util.*

class DockerService {
    companion object {
        val LOG by logger()
    }

    private val managedContainersFilter = mapOf(Constants.MANAGED_CONTAINER_LABEL to "true")
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

    init {
        // TODO: check if this will fail when docker is not available
        dockerClient.pingCmd().exec()
    }

    fun getManagedContainers(): List<DockerContainer> {
        LOG.debug("get managed containers")
        // TODO containers should be up
        return dockerClient.listContainersCmd()
            .withLabelFilter(managedContainersFilter)
            .exec()
            .filter { it.labels?.get(Constants.WORKFLOW_ID_KEY_LABEL) != null }
            .map { container ->
                val containerId = container.id
                var containerDetails: Statistics
                AsyncResultCallback<Statistics>().use { callback ->
                    dockerClient.statsCmd(containerId).exec(callback)
                    containerDetails = callback.awaitResult()
                }
                val cpuDelta = containerDetails.cpuStats.cpuUsage?.totalUsage?.minus(containerDetails.preCpuStats.cpuUsage?.totalUsage ?: 0) ?: 0
                val systemDelta = containerDetails.cpuStats.systemCpuUsage?.minus(containerDetails.preCpuStats.systemCpuUsage ?: 0) ?: 0
                val cpuUsage = if (cpuDelta > 0 && systemDelta > 0) cpuDelta.toFloat() / systemDelta * 100.0 else 0.0

                val memoryLimit = containerDetails.memoryStats.limit ?: 0
                val memoryUsage = containerDetails.memoryStats.usage ?: 0
                val memoryUsagePercentage = if (memoryLimit > 0 && memoryUsage > 0) memoryUsage / memoryLimit * 100.0 else 0.0

                val workflowId = container.labels?.get(Constants.WORKFLOW_ID_KEY_LABEL)
                DockerContainer(
                    containerId,
                    UUID.fromString(workflowId),
                    cpuUsage,
                    memoryUsagePercentage,
                    container.ports
                        ?.filter { it.publicPort != null && it.privatePort != null }
                        ?.map { DockerPortMapping(it.publicPort!!, it.privatePort!!) }
                        ?: emptyList()
                )
            }
    }

    // TODO: remove container cpu limits if needed, but keep the stat
    fun startContainer(workflowId: String, image: String, memoryLimit: Long, cpuLimit: Long, ports: List<Int>) {
        LOG.info("Pulling image {}", image)
        PullImageResultCallback().use {
            dockerClient.pullImageCmd(image)
                .exec<ResultCallback<PullResponseItem>>(it)
            it.awaitCompletion()
        }

        LOG.info("Creating container for workflow {}", workflowId)
        val hostConfig = HostConfig.newHostConfig()
            .withRestartPolicy(RestartPolicy.unlessStoppedRestart())
            .withPortBindings(ports.map { PortBinding.parse(it.toString()) })
            .withMemory(memoryLimit)
            .withCpuPercent(cpuLimit)

        val container = dockerClient.createContainerCmd(image)
            .withPortSpecs()
            .withHostConfig(hostConfig)
            .withLabels(
                mapOf(
                    Constants.MANAGED_CONTAINER_LABEL to "true",
                    Constants.WORKFLOW_ID_KEY_LABEL to workflowId
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
    }
}
