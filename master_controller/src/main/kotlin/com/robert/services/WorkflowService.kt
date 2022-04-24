package com.robert.services

import com.robert.Constants
import com.robert.LBAlgorithms
import com.robert.Workflow
import com.robert.UpdateAwareService
import com.robert.exceptions.ValidationException
import com.robert.persistance.WorkflowStorage

class WorkflowService: UpdateAwareService(Constants.WORKFLOW_SERVICE_KEY) {
    companion object {
        fun validateImage(image: String) = Regex("[a-z][a-z0-9]+:[a-z0-9]+").matches(image)
        fun validatePath(path: String) = Regex("/[a-zA-Z0-9_-]+").matches(path)
        fun validatePort(port: Int) = port in 1..65535
        fun validateMemory(memory: Long) = memory > 0
        fun validatePathMapping(pathMapping: Map<String, Int>) = pathMapping.isNotEmpty()
    }

    private val workflowStorage = WorkflowStorage()

    fun get(id: String): Workflow {
        return workflowStorage.get(id)
    }

    fun getAll(): List<Workflow> {
        return workflowStorage.getAll()
    }

    fun add(image: String, memoryLimit: Long?, algorithm: LBAlgorithms, pathMapping: Map<String, Int>): Workflow {
        if (!validateImage(image)) {
            throw ValidationException("Invalid workflow image")
        }

        if (memoryLimit != null && !validateMemory(memoryLimit)) {
            throw ValidationException("Invalid workflow memory limit")
        }

        if (!validatePathMapping(pathMapping)) {
            throw ValidationException("At least one port should be defined")
        }

        for (mapping in pathMapping.entries) {
            if (!validatePath(mapping.key)) {
                throw ValidationException("Invalid workflow path")
            }
            if (!validatePort(mapping.value)) {
                throw ValidationException("Invalid workflow port")
            }
        }

        val res = workflowStorage.add(image, memoryLimit, algorithm, pathMapping)
        markChange()
        return res
    }

    fun update(id: String, memoryLimit: Long?, algorithm: LBAlgorithms?) {
        if (memoryLimit != null && !validateMemory(memoryLimit)) {
            throw ValidationException("Invalid workflow memory limit")
        }

        workflowStorage.update(id, memoryLimit, algorithm)
        markChange()
    }

    fun delete(id: String) {
        workflowStorage.delete(id)
        markChange()
    }
}
