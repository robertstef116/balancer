package com.robert.loadbalancer.model

import com.robert.scaling.client.model.WorkflowDeploymentData

abstract class BalancingAlgorithmData {
    abstract fun getWorkflowDeploymentData(): WorkflowDeploymentData

    fun getHostInfo(): HostPortPair {
        return getWorkflowDeploymentData().let {
            HostPortPair(it.workflowId, it.host, it.port)
        }
    }
}
