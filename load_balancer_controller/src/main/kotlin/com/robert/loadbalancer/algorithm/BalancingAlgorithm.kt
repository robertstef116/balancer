package com.robert.loadbalancer.algorithm

import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData

interface BalancingAlgorithm {
    fun updateData(data: List<WorkflowDeploymentData>)
    fun getTarget(): HostPortPair
    fun addResponseTimeData(responseTime: Long)
}