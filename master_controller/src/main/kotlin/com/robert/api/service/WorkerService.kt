package com.robert.api.service

import com.robert.scaling.client.ScalingClient
import com.robert.scaller.Worker
import com.robert.scaller.WorkerState
import com.robert.storage.repository.WorkerRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class WorkerService: KoinComponent {
    private val workerRepository by inject<WorkerRepository>()
    private val scalingClient by inject<ScalingClient>()

    fun get(id: UUID): Worker? {
        return workerRepository.find(id)
    }

    fun getAll(): Collection<Worker> {
        return workerRepository.getAll()
    }

    fun update(id: UUID, state: WorkerState) {
        scalingClient.updateWorker(id, state)
    }

    fun delete(id: UUID) {
        scalingClient.deleteWorker(id)
    }
}
