package com.robert.services

import com.robert.Constants
import com.robert.UpdateAwareService
import com.robert.WorkerNode
import com.robert.exceptions.ValidationException
import com.robert.persistance.WorkerNodeStorage
import org.apache.commons.validator.routines.InetAddressValidator

class WorkerService: UpdateAwareService(Constants.WORKER_SERVICE_KEY) {
    companion object {
        fun validateWorkerAlias(alias: String) = Regex("[a-zA-Z][a-z-A-Z0-9_]{2,}").matches(alias)
    }

    private val workerNodeStorage = WorkerNodeStorage()

    fun get(id: String): WorkerNode {
        return workerNodeStorage.get(id)
    }

    fun getAll(): List<WorkerNode> {
        return workerNodeStorage.getAll()
    }

    fun add(alias: String, host: String, port: Int, inUse: Boolean): WorkerNode {
        if (!validateWorkerAlias(alias)) {
            throw ValidationException("Invalid alias")
        }
        val res = workerNodeStorage.add(alias, host, port, inUse)
        markChange()
        return res
    }

    fun update(id: String, alias: String?, port: Int?, inUse: Boolean?) {
        if (alias != null && !validateWorkerAlias(alias)) {
            throw ValidationException("Invalid alias")
        }
        workerNodeStorage.update(id, alias, port, inUse)
        markChange()
    }

    fun delete(id: String) {
        workerNodeStorage.delete(id)
        markChange()
    }
}
