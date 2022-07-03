package com.robert

import com.robert.api.request.DockerCreateContainerRequest
import com.robert.api.response.DockerCreateContainerResponse
import com.robert.balancing.TargetData
import com.robert.docker.DockerContainerPorts
import com.robert.resources.Deployment
import com.robert.resources.Worker
import com.robert.resources.Workflow
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class Service(private val storage: Storage) {
    companion object {
        private val log = LoggerFactory.getLogger(DeploymentsManager::class.java)
    }

    private lateinit var updateAwareServicesMetadata: Map<String, Long>

    fun deployWorkflow(worker: Worker, workflow: Workflow): Deployment? {
        return runBlocking {
            val url = "http://${worker.host}:${worker.port}/docker"
            var dockerContainerResponse: DockerCreateContainerResponse? = null
            val deploymentId = UUID.randomUUID().toString()
            try {
                dockerContainerResponse = HttpClient.post(
                    url,
                    DockerCreateContainerRequest(
                        deploymentId,
                        workflow.image,
                        workflow.memoryLimit,
                        workflow.pathsMapping.values.toList().distinct()
                    )
                )
                return@runBlocking storage.addDeployment(
                    deploymentId,
                    worker.id,
                    workflow.id,
                    dockerContainerResponse.id,
                    dockerContainerResponse.ports
                )
            } catch (e: Exception) {
                log.error("error deploying workflow, err = {}", e.message)
                if (dockerContainerResponse != null) {
                    HttpClient.delete<String>("$url?id=${dockerContainerResponse.id}")
                } else {
                    // empty
                }
            }
            return@runBlocking null
        }
    }

    fun removeDeployment(worker: Worker, id: String, containerId: String): Boolean {
        return runBlocking {
            var response = false
            val url = "http://${worker.host}:${worker.port}/docker/$containerId"
            try {
                val res = HttpClient.delete<String>(url)
                if (res == "OK") {
                    storage.deleteDeployment(id)
                    response = true
                }
            } catch (e: Exception) {
                log.error("error removing deployment err = {}", e.message)
            }
            try {
                storage.deleteDeployment(id)
            } catch (e: Exception) {
                log.error("error removing deployment from db, err = {}", e.message)
            }
            return@runBlocking response
        }
    }

    @Synchronized
    fun getUpdatedServicesConfig(): List<String>? {
        try {
            val newMetadata = storage.getConfigChangeTimestampMetadata()
            val updatedServicesKeys = mutableListOf<String>()
            if (this::updateAwareServicesMetadata.isInitialized) {
                for ((key, value) in newMetadata.entries) {
                    if (updateAwareServicesMetadata[key] != value) {
                        updatedServicesKeys.add(key)
                    }
                }
            }
            updateAwareServicesMetadata = newMetadata
            return updatedServicesKeys
        } catch (e: Exception) {
            log.error("error getting updated services config timestamps, err = {}", e.message)
        }
        return null
    }

    fun persistAnalytics(targetResource: TargetData) {
        try {
            storage.persistAnalytics(targetResource.workerId, targetResource.workflowId, targetResource.deploymentId, Instant.now().epochSecond)
        } catch (e: Exception) {
            log.warn("unable to persist analytics data, err = {}", e.message)
        }
    }

    @Synchronized
    fun syncWorkers() {
        val workers = storage.getWorkers()
        runBlocking {
            withContext(Dispatchers.IO) {// waits for all child coroutines to finish
                for (worker in workers) {
                    launch {
                        for (i in 1..10) {
                            try {
                                syncWorker(worker)
                                break;
                            } catch (e: Exception) {
                                log.warn("unable to sync worker {} {}, try again...", worker, e.message)
                                delay(i * 5000L)
                            }
                        }
                    }
                }
            }
        }
        log.debug("workers synced")
    }

    private suspend fun syncWorker(worker: Worker) {
        log.debug("syncing worker {}", worker)
        val url = "http://${worker.host}:${worker.port}/docker/ports"
        val res = HttpClient.get<List<DockerContainerPorts>>(url, 60000)
        for (deployment in res) {
            storage.updateDeploymentMapping(deployment.deploymentId, deployment.ports)
        }
        log.debug("done syncing worker {}", worker)
    }
}
