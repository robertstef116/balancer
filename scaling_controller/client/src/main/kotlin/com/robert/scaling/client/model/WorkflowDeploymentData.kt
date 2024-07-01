package com.robert.scaling.client.model

import com.robert.enums.LBAlgorithms
import java.util.*

data class WorkflowDeploymentData(
    val workflowId: UUID,
    val path: String,
    val host: String,
    val port: Int,
    val algorithm: LBAlgorithms,
    val score: Double,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorkflowDeploymentData

        if (workflowId != other.workflowId) return false
        if (path != other.path) return false
        if (host != other.host) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = workflowId.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + port
        return result
    }
}
