package com.robert.loadbalancer.algorithm

import com.robert.balancing.LoadBalancerResponseType
import com.robert.enums.LBAlgorithms
import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData

interface BalancingAlgorithm {
    fun getAlgorithmType(): LBAlgorithms
    fun updateData(data: List<WorkflowDeploymentData>)
    fun getTarget(): HostPortPair
    fun addResponseTimeData(target: HostPortPair, responseTime: Long, responseType: LoadBalancerResponseType) {}
}