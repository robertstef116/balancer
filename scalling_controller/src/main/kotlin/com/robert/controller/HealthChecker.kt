package com.robert.controller

import com.robert.Constants
import com.robert.Env
import com.robert.HttpClient
import com.robert.api.response.WorkerResourceResponse
import com.robert.scaller.WorkerR
import com.robert.persistance.DAORepository
import io.ktor.client.call.*
import io.ktor.util.logging.*
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class HealthChecker : KoinComponent {
    companion object {
        private val LOG =  KtorSimpleLogger(this::class.java.name)
        private val checkTimeout = Env.getLong(Constants.HEALTH_CHECK_TIMEOUT, 10000L)

        private suspend fun doHealthCheck(worker: WorkerR): WorkerResourceResponse {
            LOG.debug("Health check on worker {}", worker.alias)
            val url = "http://${worker.host}:${worker.port}/resource"
            return HttpClient.get(url, checkTimeout).body()
        }
    }

    private val workersRepository: DAORepository by inject()

    var workers: List<WorkerR> = workersRepository.getWorkers()
        private set

    val workersHealthData = mutableMapOf<UUID, Health>()

    fun reloadWorkers() {
        workers = workersRepository.getWorkers()
    }

    fun checkWorkers(): Boolean = runBlocking(Dispatchers.IO) {
        return@runBlocking workers.map { worker ->
            async {
                val health = getWorkerHealthData(worker.id)
                try {
                    val workerData = doHealthCheck(worker)
                    health.addWorkerPerformanceData(workerData.performanceData)
                    health.addContainersPerformanceData(workerData.containersStats)
                    health.cleanRemovedContainersData(workerData.containersStats)
                    health.resetFailures()
                } catch (e: Exception) {
                    LOG.warn("Health data for worker {} failed", worker.alias, e)
                    return@async health.increaseFailures()
                }
                return@async false
            }
        }.awaitAll().any { it }
    }

    @Synchronized
    fun getWorkerHealthData(workerId: UUID): Health {
        return workersHealthData.computeIfAbsent(workerId) {
            Health(workerId) {
                healthStatusChanged(workerId, it)
            }
        }
    }

    private fun healthStatusChanged(workerId: UUID, newStatus: HealthStatus) {

    }
}
