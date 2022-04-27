package com.robert

import kotlinx.coroutines.runBlocking

class Service(private val storage: Storage) {
    fun deployWorkflow(worker: WorkerNode, workflow: Workflow): Deployment? {
        return runBlocking {
            val url = "http://${worker.host}:${worker.port}/docker"
            var dockerContainerResponse: DockerCreateContainerResponse? = null
            try {
                dockerContainerResponse = HttpClient.post(
                    url,
                    DockerCreateContainerRequest(
                        workflow.image,
                        workflow.memoryLimit,
                        (workflow.pathsMapping.values as List<Int>).distinct()
                    )
                )
                return@runBlocking storage.addDeployment(
                    worker.id,
                    workflow.id,
                    dockerContainerResponse.id,
                    dockerContainerResponse.ports
                )
            } catch (_: Exception) {
                if (dockerContainerResponse != null) {
                    HttpClient.delete<String>("$url?id=${dockerContainerResponse.id}")
                } else {
                    // empty
                }
            }
            return@runBlocking null
        }
    }

    fun removeContainer(worker: WorkerNode, id: String): Boolean {
        return runBlocking {
            val url = "http://${worker.host}:${worker.port}/docker?/$id"
            try {
                val res = HttpClient.delete<String>(url)
                if (res == "OK") {
                    return@runBlocking true
                }
            } catch (_: Exception) {
            }
            return@runBlocking false
        }
    }
}
