package com.robert.services

import com.robert.Deployment
import com.robert.UpdateAwareService
import com.robert.exceptions.ValidationException
import com.robert.persistance.DeploymentStorage

class DeploymentService: UpdateAwareService() {
    companion object {
        fun validateImage(image: String) = Regex("[a-z][a-z0-9]+:[a-z0-9]+").matches(image)
        fun validatePath(path: String) = Regex("/[a-zA-Z0-9_-]+").matches(path)
    }

    private val deploymentStorage = DeploymentStorage()

    fun get(id: String): Deployment {
        return deploymentStorage.get(id)
    }

    fun getAll(): List<Deployment> {
        return deploymentStorage.getAll()
    }

    fun add(path: String, image: String, memoryLimit: Long?, ports: List<Int>?): Deployment {
        if (!validatePath(path)) {
            throw ValidationException("Invalid deployment path")
        }
        if (!validateImage(image)) {
            throw ValidationException("Invalid deployment image")
        }

        val res = deploymentStorage.add(path, image, memoryLimit, ports)
        markChange()
        return res
    }

    fun update(id: String, path: String?, image: String?, memoryLimit: Long?, ports: List<Int>?) {
        if (path != null && !validatePath(path)) {
            throw ValidationException("Invalid deployment path")
        }
        if (image != null && !validateImage(image)) {
            throw ValidationException("Invalid alias")
        }

        deploymentStorage.update(id, path, image, memoryLimit, ports)
        markChange()
    }

    fun delete(id: String) {
        deploymentStorage.delete(id)
        markChange()
    }
}
