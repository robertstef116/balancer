package com.robert.storage.repository

import com.robert.scaller.Worker
import com.robert.scaller.WorkerState
import java.util.*

interface WorkerRepository {
    fun getAll(): Collection<Worker>
    fun getAllOnline(): Collection<Worker>
    fun create(worker: Worker)
    fun find(id: UUID): Worker?
    fun update(id: UUID, alias: String?, status: WorkerState?): Boolean
    fun delete(id: UUID): Boolean
}