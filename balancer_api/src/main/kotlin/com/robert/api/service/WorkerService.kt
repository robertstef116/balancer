package com.robert.api.service

import com.robert.exceptions.ValidationException
import com.robert.scaling.client.ScalingClient
import com.robert.resources.Worker
import com.robert.enums.WorkerState
import com.robert.storage.repository.WorkerRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class WorkerService : KoinComponent {
    private val workerRepository by inject<WorkerRepository>()
    private val scalingClient by inject<ScalingClient>()

    fun get(id: UUID): Worker? {
        return workerRepository.find(id)
    }

    fun getAll(): Collection<Worker> {
        return workerRepository.getAll()
    }

    fun update(id: UUID, state: WorkerState) {
        if (!scalingClient.updateWorker(id, state)) {
            throw ValidationException("Unable to update worker")
        }
    }

    fun delete(id: UUID) {
        if (scalingClient.deleteWorker(id)) {
            throw ValidationException("Unable to delete worker")
        }
    }
}
