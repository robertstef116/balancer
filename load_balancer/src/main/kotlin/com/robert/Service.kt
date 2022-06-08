package com.robert

import DockerContainerPorts
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class Service(private val storage: Storage) {
    companion object {
        private val log = LoggerFactory.getLogger(DeploymentsManager::class.java)
    }

    fun deployWorkflow(worker: WorkerNode, workflow: Workflow): Deployment? {
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
                e.printStackTrace()
                log.error("error deploying workflow {}", e.message)
                if (dockerContainerResponse != null) {
                    HttpClient.delete<String>("$url?id=${dockerContainerResponse.id}")
                } else {
                    // empty
                }
            }
            return@runBlocking null
        }
    }

    fun removeDeployment(worker: WorkerNode, id: String, containerId: String): Boolean {
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
                e.printStackTrace()
                log.error("error removing deployment {}", e.message)
            }
            try {
                storage.deleteDeployment(id)
            } catch (e: Exception) {
                e.printStackTrace()
                log.error("error removing deployment from db {}", e.message)
            }
            return@runBlocking response
        }
    }

    suspend fun getMasterChanges(host: String, port: Int): Map<String, Any>? {
        val url = "http://$host:$port/change"
        try {
            return HttpClient.get(url)
        } catch (e: Exception) {
            e.printStackTrace()
            log.error("error getting master changes {}", e.message)
        }
        return null
    }

    fun persistAnalytics(targetResource: PathTargetResource) {
        try {
            storage.persistAnalytics(targetResource.workerId, targetResource.workflowId, targetResource.deploymentId, Instant.now().epochSecond)
        } catch (e: Exception) {
            log.warn("unable to persist analytics data {}", e.message)
        }
    }

    @Synchronized
    fun syncWorkers() {
        val workers = storage.getWorkers()
        runBlocking {
            for (worker in workers) {
                CoroutineScope(Dispatchers.IO).launch {
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
        log.debug("workers synced")
    }

    private suspend fun syncWorker(worker: WorkerNode) {
        log.debug("syncing worker {}", worker)
        val url = "http://${worker.host}:${worker.port}/docker/ports"
        val res = HttpClient.get<List<DockerContainerPorts>>(url, 60000)
        for (deployment in res) {
            storage.updateDeploymentMapping(deployment.deploymentId, deployment.ports)
        }
    }
}
