package com.robert.services

import DockerContainerPorts
import com.robert.*
import com.robert.exceptions.NotFoundException
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.ListContainersFilterParam
import com.spotify.docker.client.exceptions.ContainerNotFoundException
import com.spotify.docker.client.messages.*
import org.slf4j.LoggerFactory

class DockerService {
    companion object {
        private const val DOCKER_TIMEOUT = 30000L
        private val log = LoggerFactory.getLogger(DockerService::class.java)
        private val managedContainersFilter = ListContainersFilterParam.withLabel(Constants.MANAGED_CONTAINER_LABEL)
        private val docker: DefaultDockerClient = DefaultDockerClient.fromEnv()
            .readTimeoutMillis(DOCKER_TIMEOUT)
            .connectTimeoutMillis(DOCKER_TIMEOUT)
            .build()
        private val registry = RegistryAuth.builder()
            .serverAddress(ConfigProperties.getString("docker.registry") ?: "hub.docker.com")
            .build()
    }

    fun getManagedContainers(): List<DockerContainer> {
        log.debug("get managed containers")
        return docker.listContainers(managedContainersFilter).map { container ->
            DockerContainer(
                container.id(),
                container.names()?.get(0)?.substring(1) ?: "unknown",
                container.image(),
                container.created(),
                container.status(),
                container.ports()?.associateBy({ it.publicPort() }, { it.privatePort() }) ?: emptyMap()
            )
        }
    }

    fun startContainer(
        deploymentId: String,
        image: String,
        memoryLimit: Long?,
        ports: List<Int>
    ): DockerCreateContainerResponse {
        // TODO: test image has tag, e.g. latest

        val portBindings = HashMap<String, List<PortBinding>>()
        ports.forEach {
            val portBinding = PortBinding.randomPort("0.0.0.0")
            portBindings[it.toString()] = listOf(portBinding)
        }

        log.debug("pulling image", image)
        docker.pull(image, registry)

        // TODO: set docker registry
        val hostConfig = HostConfig.builder()
            .restartPolicy(HostConfig.RestartPolicy.unlessStopped())
            .portBindings(portBindings)

        if (memoryLimit != null) {
            hostConfig.memory(memoryLimit)
        }

        val containerConfig = ContainerConfig.builder()
            .hostConfig(hostConfig.build())
            .image(image)
            .labels(mapOf(Constants.MANAGED_CONTAINER_LABEL to "", Constants.DEPLOYMENT_ID_KEY_LABEL to deploymentId))
            .exposedPorts(*ports.map { it.toString() }.toTypedArray())
            .build()

        log.debug("creating container", containerConfig)
        val creation = docker.createContainer(containerConfig)
        val id = creation.id()!!

        log.debug("starting container", id)
        docker.startContainer(id)

        log.debug("getting container info", id)
        val info = docker.inspectContainer(id)
        val containerPorts = info.networkSettings().ports()?.entries?.associate {
            it.value.first().hostPort().toInt() to it.key.split("/")[0].toInt()
        } ?: emptyMap()

        return DockerCreateContainerResponse(id, containerPorts)
    }

    fun removeContainer(id: String) {
        log.debug("removing container", id)

        try {
            docker.removeContainer(id, DockerClient.RemoveContainerParam.forceKill())
            log.debug("container removed")
        } catch (e: ContainerNotFoundException) {
            log.debug("container not found", id)
            throw NotFoundException()
        }
    }

    fun managedContainersStats(): List<DockerContainerStats> {
        log.debug("get managed containers stats")

        return docker.listContainers(managedContainersFilter).map { container ->
            val stats = docker.stats(container.id())
            val memoryStats = stats.memoryStats()
            val cpuStats = stats.cpuStats()
            val preCpuStats = stats.precpuStats()

            val cpuDelta = cpuStats.cpuUsage().totalUsage() - preCpuStats.cpuUsage().totalUsage()
            val systemDelta = cpuStats.systemCpuUsage()?.minus(preCpuStats.systemCpuUsage() ?: 0) ?: 0
            val cpuUsage = if (cpuDelta > 0 && systemDelta > 0) cpuDelta.toDouble() / systemDelta * 100 else 0.0
            val deploymentId = container.labels()?.get(Constants.DEPLOYMENT_ID_KEY_LABEL)!!

            DockerContainerStats(
                deploymentId,
                container.id(),
                cpuUsage,
                memoryStats.limit()?.minus((memoryStats.usage() ?: 0)) ?: 0,
                memoryStats.limit() ?: 0
            )
        }
    }

    fun managedContainersPorts(): List<DockerContainerPorts> {
        log.debug("get managed containers ports")

        return docker.listContainers(managedContainersFilter).map { container ->
            val deploymentId = container.labels()?.get(Constants.DEPLOYMENT_ID_KEY_LABEL)!!
            val containerPorts = container.ports()?.associate {
                it.publicPort() to it.privatePort()
            } ?: emptyMap<Int, Int>()

            DockerContainerPorts(deploymentId, containerPorts)
        }
    }
}
