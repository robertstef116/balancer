package com.robert.loadbalancer.algorithm

import com.robert.balancing.LoadBalancerResponseType
import com.robert.enums.LBAlgorithms
import com.robert.loadbalancer.model.BalancingAlgorithmData
import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData

interface BalancingAlgorithm {
    companion object {
        fun getAvailableTargets(allTargets: List<WorkflowDeploymentData>, blacklistedTargets: Set<HostPortPair>): List<WorkflowDeploymentData> {
            if (blacklistedTargets.isEmpty()) {
                return allTargets
            }
            return allTargets
        }

        inline fun <reified T : BalancingAlgorithmData> getAvailableTargetsData(allTargets: List<T>, blacklistedTargets: Set<HostPortPair>): List<T> {
            if (blacklistedTargets.isEmpty()) {
                return allTargets
            }
            return allTargets
        }
    }

    fun getAlgorithmType(): LBAlgorithms
    fun updateData(data: List<WorkflowDeploymentData>)
    fun getTarget(blacklistedTargets: Set<HostPortPair>): HostPortPair

    fun addResponseTimeData(target: HostPortPair, responseTime: Long, responseType: LoadBalancerResponseType) {}
}