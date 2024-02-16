package com.robert.controller

import com.robert.Constants
import com.robert.Env
import com.robert.HttpClient
import com.robert.api.response.WorkerResourceResponse
import com.robert.persistance.DAORepository
import com.robert.scaller.WorkerR
import com.robert.scaller.WorkerStatusR
import io.ktor.client.call.*
import io.ktor.util.logging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class WorkerData(
    val worker: WorkerR
) {
    val lock = ReentrantLock()
    var removed = false
}

class HealthChecker : KoinComponent {
    companion object {
        private val log = KtorSimpleLogger(this::class.java.name)
        private val checkTimeout = Env.getLong(Constants.HEALTH_CHECK_TIMEOUT, 10000L)

        private suspend fun doHealthCheck(worker: WorkerR): WorkerResourceResponse {
            log.debug("Health check on worker {}", worker.alias)
            val url = "http://${worker.host}:${worker.port}/resource"
            return HttpClient.get(url, checkTimeout).body()
        }
    }

    private val storage: DAORepository by inject()

    private var workers = loadWorkers()

    val workersHealthData = mutableMapOf<UUID, Health>()

    private fun loadWorkers(): MutableMap<UUID, WorkerData> {
        val workers = mutableMapOf<UUID, WorkerData>()
        for (worker in storage.getWorkers()) {
            workers[worker.id] = WorkerData(worker)
        }
        return workers
    }

    fun checkWorkers(): Boolean = runBlocking(Dispatchers.IO) {
        return@runBlocking workers.keys.map { workerId ->
            async {
                workers[workerId]?.let {
                    val health = getWorkerHealthData(workerId)
                    if (it.worker.status == WorkerStatusR.DISABLED) {
                        return@async false
                    }
                    try {
                        val workerData = doHealthCheck(it.worker)
                        it.lock.withLock {
                            if (it.removed || it.worker.status == WorkerStatusR.DISABLED) {
                                workersHealthData.remove(workerId)
                                return@async false
                            }
                            health.addWorkerPerformanceData(workerData.performanceData)
                            health.addContainersPerformanceData(workerData.containersStats)
                            health.cleanRemovedContainersData(workerData.containersStats)
                            health.resetFailures()
                        }
                    } catch (e: Exception) {
                        log.warn("Health data for worker {} failed", it.worker.alias, e)
                        return@async health.increaseFailures()
                    }
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

    fun createWorker(worker: WorkerR) {
        log.debug("creating worker: {}", worker)
        workers[worker.id] = WorkerData(worker)
    }

    fun deleteWorker(id: UUID): Boolean {
        log.debug("removing worker {}", id)
        workers.remove(id)?.let {
            it.lock.withLock {
                workersHealthData.remove(id)
                it.removed = true
            }
            return true
        }
        return false
    }

    fun updateWorker(id: UUID, alias: String?, status: WorkerStatusR?): Boolean {
        log.debug("updating worker {}: ", id)
        workers[id]?.let {
            it.lock.withLock {
                if (it.removed) {
                    return false
                }
                if (alias != null) {
                    it.worker.alias = alias
                }
                if (status != null) {
                    it.worker.status = status
                    if (status == WorkerStatusR.DISABLED) {
                        workersHealthData.remove(id)
                    }
                }
            }
            return true
        }
        return false
    }

    fun getWorker(id: UUID): WorkerR? {
        return workers[id]?.let {
            if (it.worker.status == WorkerStatusR.DISABLED) {
                return null
            }
            return it.worker
        }
    }
}
