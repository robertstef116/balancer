package com.robert.loadbalancer.model

import com.robert.scaling.client.model.WorkflowDeploymentData

abstract class BalancingAlgorithmData {
    abstract fun getDeploymentData(): WorkflowDeploymentData

    fun getHostInfo(): HostPortPair {
        return getDeploymentData().let {
            HostPortPair(it.workflowId, it.host, it.port)
        }
    }
}
