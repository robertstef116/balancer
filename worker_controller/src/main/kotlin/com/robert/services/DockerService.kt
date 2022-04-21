package com.robert.services

import com.robert.*
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.ListContainersFilterParam
import com.spotify.docker.client.exceptions.ContainerNotFoundException
import com.spotify.docker.client.messages.*
import org.slf4j.LoggerFactory

class DockerService {
    companion object {
        private val log = LoggerFactory.getLogger(DockerService::class.java)
        private val managedContainersFilter = ListContainersFilterParam.withLabel(Constants.MANAGED_CONTAINER_LABEL)
        private val docker: DefaultDockerClient = DefaultDockerClient.fromEnv().build();
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
                container.ports()?.map {
                    DockerPortMapping(
                        it.privatePort(),
                        it.publicPort()
                    )
                } ?: emptyList()
            )
        }
    }

    fun startContainer(createContainerRequest: DockerCreateContainerRequest): DockerCreateContainerResponse {
        val portBindings = HashMap<String, List<PortBinding>>()
        createContainerRequest.ports.forEach {
            val portBinding = PortBinding.randomPort("0.0.0.0")
            portBindings[it.toString()] = listOf(portBinding)
        }

        log.debug("pulling image", createContainerRequest.image)
        docker.pull(createContainerRequest.image)

        val hostConfig = HostConfig.builder()
            .restartPolicy(HostConfig.RestartPolicy.unlessStopped())
            .portBindings(portBindings)

        if (createContainerRequest.memoryLimit != null) {
            hostConfig.memory(createContainerRequest.memoryLimit)
        }

        val containerConfig = ContainerConfig.builder()
            .hostConfig(hostConfig.build())
            .image(createContainerRequest.image)
            .labels(mapOf(Constants.MANAGED_CONTAINER_LABEL to ""))
            .exposedPorts(*createContainerRequest.ports.map { it.toString() }.toTypedArray())
            .build()

        log.debug("creating container", containerConfig)
        val creation = docker.createContainer(containerConfig)
        val id = creation.id()!!

        log.debug("starting container", id)
        docker.startContainer(id)

        log.debug("getting container info", id)
        val info = docker.inspectContainer(id)
        val ports = info.networkSettings().ports()?.entries?.map {
            DockerPortMapping(it.key.split("/")[0].toInt(), it.value.first().hostPort().toInt())
        }

        return DockerCreateContainerResponse(id, ports ?: emptyList())
    }

    fun removeContainer(id: String): Boolean {
        log.debug("removing container", id)

        return try {
            docker.removeContainer(id, DockerClient.RemoveContainerParam.forceKill())
            log.debug("container removed")
            true
        } catch (e: ContainerNotFoundException) {
            log.debug("container not found", id)
            false
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

            DockerContainerStats(
                container.id(),
                cpuUsage,
                memoryStats.limit()?.minus((memoryStats.usage() ?: 0)) ?: 0,
                memoryStats.limit() ?: 0
            )
        }
    }
}
