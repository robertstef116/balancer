package com.robert.controller

import com.robert.HttpClient
import com.robert.api.request.DockerCreateContainerRequest
import com.robert.api.response.DockerCreateContainerResponse
import com.robert.persistence.DAORepository
import com.robert.scaller.DeploymentR
import com.robert.scaller.Worker
import com.robert.scaller.Workflow
import io.ktor.http.*
import io.ktor.util.logging.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class DeploymentService : KoinComponent {
    companion object {
        private val LOG = KtorSimpleLogger(this::class.java.name)
    }

    private val healthChecker: HealthChecker by inject()
    private val storage: DAORepository by inject()

    fun createDeployment(worker: Worker, workflow: Workflow): DeploymentR? {
        val url = "http://${worker.host}:${worker.port}/docker"
        val deploymentId = UUID.randomUUID()
        var dockerContainerResponse: DockerCreateContainerResponse? = null
        try {
            dockerContainerResponse = HttpClient.blockingPost(
                url,
                DockerCreateContainerRequest(
                    deploymentId,
                    workflow.image,
                    workflow.memoryLimit,
                    workflow.pathsMapping.values.toList().distinct()
                )
            )
            val deployment = DeploymentR(
                deploymentId,
                worker.id,
                workflow.id,
                dockerContainerResponse!!.id,
                dockerContainerResponse.ports
            )
            storage.createDeployment(deployment)
            return deployment
        } catch (e: Exception) {
            LOG.error("Unable to deploy workflow {}", workflow.id, e)
            if (dockerContainerResponse != null) {
                HttpClient.blockingDelete("$url?id=${dockerContainerResponse.id}")
                storage.deleteDeployment(deploymentId)
            }
        }
        return null
    }

    fun removeDeployment(workerId: UUID, deploymentId: UUID, containerId: String): Boolean {
        val worker = healthChecker.getWorker(workerId)
        try {
            if (worker == null) {
                LOG.warn("Worker with id {} not found, removing the deployment data", workerId)
                storage.deleteDeployment(deploymentId)
                return true
            }

            val url = "http://${worker.host}:${worker.port}/docker/$containerId"
            val res = HttpClient.blockingDelete(url)
            if (res.status == HttpStatusCode.NotFound) {
                LOG.warn("Container for deployment {} not found on worker {}, removing deployment data", deploymentId, worker.alias)
            }
            storage.deleteDeployment(deploymentId)
        } catch (e: Exception) {
            LOG.error("Error removing deployment", e)
            return false
        }
        return true
    }
}
