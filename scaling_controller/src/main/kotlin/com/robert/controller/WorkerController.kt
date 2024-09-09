package com.robert.controller

import com.robert.Env
import com.robert.enums.WorkerState
import com.robert.logger
import com.robert.model.InternalWorkerStatus
import com.robert.resources.DockerContainer
import com.robert.resources.Worker
import com.robert.storage.repository.WorkerRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.postgresql.util.PSQLException
import java.time.Instant
import java.util.*

class WorkerController : KoinComponent {
    companion object {
        val LOG by logger()

        private val WORKER_STATUS_MAX_AGE_SECONDS = Env.getInt("WORKER_STATUS_MAX_AGE_SECONDS", 36000)
    }

    private val workerRepository: WorkerRepository by inject()
    private val workerStatus = mutableMapOf<UUID, InternalWorkerStatus>()

    init {
        workerRepository.getAll().forEach {
            initWorkerState(it.id, it.state)
        }
    }

    @Synchronized
    fun getOnlineWorkersStatus(): List<InternalWorkerStatus> {
        return workerStatus.values.filter { it.state == WorkerState.ONLINE }
    }

    @Synchronized
    fun updateWorkerStatus(id: UUID, alias: String, host: String, cpuLoad: Double, memoryLoad: Double, availableMemory: Long, activeDeployments: List<DockerContainer>) {
        when (workerStatus[id]?.state) {
            null -> {
                LOG.info("Discovered new worker {}", id)
                try {
                    workerRepository.create(Worker(id, alias.take(50), WorkerState.ONLINE))
                } catch (e: PSQLException) {
                    LOG.warn("Failed to insert worker details, trying to update instead: {}", e.message)
                    workerRepository.update(id, alias.take(50), WorkerState.ONLINE)
                }
            }

            WorkerState.OFFLINE -> {
                LOG.info("Updating status of worker {} to ONLINE", id)
                workerRepository.update(id, alias.take(50), WorkerState.ONLINE)
            }

            WorkerState.DISABLED -> {
                LOG.trace("Worker {} is disable, ignoring the status..", id)
                return
            }

            WorkerState.ONLINE -> {}
        }

        workerStatus[id] = InternalWorkerStatus(id, host, cpuLoad, memoryLoad, availableMemory, now(), WorkerState.ONLINE, activeDeployments)
    }

    @Synchronized
    fun updateWorkerState(id: UUID, state: WorkerState): Boolean {
        if (!workerStatus.containsKey(id)) {
            initWorkerState(id, state)
        } else {
            workerStatus[id]?.state = state
        }
        workerRepository.update(id, null, state)
        return true
    }

    @Synchronized
    fun removeWorker(id: UUID): Boolean {
        LOG.debug("Removing worker with id {}", id)
        workerStatus.remove(id)
        return workerRepository.delete(id)
    }

    @Synchronized
    fun pruneWorkersByAge(onPrune: (UUID) -> Unit) {
        val now = now()
        workerStatus.values.filter { it.state == WorkerState.ONLINE && now - it.lastUpdate > WORKER_STATUS_MAX_AGE_SECONDS }
            .forEach {
                onPrune(it.id)
                updateWorkerState(it.id, WorkerState.OFFLINE)
                LOG.info("Updated status of worker {} to OFFLINE since age threshold was exceeded", it.id)
            }
    }

    private fun initWorkerState(id: UUID, state: WorkerState) {
        var workerState = WorkerState.OFFLINE
        if (state == WorkerState.DISABLED) {
            workerState = WorkerState.DISABLED
        } else {
            LOG.debug("Initializing state of worker {}", id)
            workerRepository.update(id, null, workerState)
        }
        workerStatus[id] = InternalWorkerStatus(id, "", -1.0, -1.0, -1, now(), workerState, listOf())
    }

    private fun now(): Long {
        return Instant.now().epochSecond
    }
}