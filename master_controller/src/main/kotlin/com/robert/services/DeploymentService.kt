package com.robert.services

import com.robert.resources.Deployment
import com.robert.persistance.DeploymentStorage

class DeploymentService {
    private val deploymentStorage = DeploymentStorage()

    fun getAll(): List<Deployment> {
        return deploymentStorage.getDeployments()
    }
}
