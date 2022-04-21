package com.robert.services

import com.robert.UpdateAwareService
import com.robert.WorkerNode
import com.robert.exceptions.ValidationException
import com.robert.persistance.WorkerNodeStorage
import org.apache.commons.validator.routines.InetAddressValidator

class WorkerService: UpdateAwareService() {
    companion object {
        fun validateWorkerAlias(alias: String) = Regex("[a-zA-Z][a-z-A-Z0-9_]{2,}").matches(alias)
        fun validateWorkerIpAddress(ip: String) = InetAddressValidator.getInstance().isValidInet4Address(ip)
    }

    private val workerNodeStorage = WorkerNodeStorage()

    fun get(id: String): WorkerNode {
        return workerNodeStorage.get(id)
    }

    fun getAll(): List<WorkerNode> {
        return workerNodeStorage.getAll()
    }

    fun add(alias: String, ip: String, inUse: Boolean): WorkerNode {
        if (!validateWorkerAlias(alias)) {
            throw ValidationException("Invalid alias")
        }
        if (!validateWorkerIpAddress(ip)) {
            throw ValidationException("Invalid ip address")
        }
        val res = workerNodeStorage.add(alias, ip, inUse)
        markChange()
        return res
    }

    fun update(id: String, alias: String?, inUse: Boolean?) {
        if (alias != null && !validateWorkerAlias(alias)) {
            throw ValidationException("Invalid alias")
        }
        workerNodeStorage.update(id, alias, inUse)
        markChange()
    }

    fun delete(id: String) {
        workerNodeStorage.delete(id)
        markChange()
    }
}
