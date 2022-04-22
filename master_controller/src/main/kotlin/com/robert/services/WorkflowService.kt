package com.robert.services

import com.robert.Workflow
import com.robert.UpdateAwareService
import com.robert.exceptions.ValidationException
import com.robert.persistance.WorkflowStorage

class WorkflowService: UpdateAwareService() {
    companion object {
        fun validateImage(image: String) = Regex("[a-z][a-z0-9]+:[a-z0-9]+").matches(image)
        fun validatePath(path: String) = Regex("/[a-zA-Z0-9_-]+").matches(path)
    }

    private val workflowStorage = WorkflowStorage()

    fun get(id: String): Workflow {
        return workflowStorage.get(id)
    }

    fun getAll(): List<Workflow> {
        return workflowStorage.getAll()
    }

    fun add(path: String, image: String, memoryLimit: Long?, ports: List<Int>?): Workflow {
        if (!validatePath(path)) {
            throw ValidationException("Invalid workflow path")
        }
        if (!validateImage(image)) {
            throw ValidationException("Invalid workflow image")
        }

        val res = workflowStorage.add(path, image, memoryLimit, ports)
        markChange()
        return res
    }

    fun update(id: String, path: String?, image: String?, memoryLimit: Long?, ports: List<Int>?) {
        if (path != null && !validatePath(path)) {
            throw ValidationException("Invalid workflow path")
        }
        if (image != null && !validateImage(image)) {
            throw ValidationException("Invalid alias")
        }

        workflowStorage.update(id, path, image, memoryLimit, ports)
        markChange()
    }

    fun delete(id: String) {
        workflowStorage.delete(id)
        markChange()
    }
}
