package com.robert.storage.entities

import com.robert.balancing.LoadBalancerResponseType
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object LoadBalancerAnalytics : Table() {
    val workflowId = reference("workflow_id", Workflows.id, onDelete = ReferenceOption.CASCADE)
    val path = varchar("path", 35)
    val responseTime = long("response_time_ms")
    val timestamp = long("timestamp_ms")
    val responseType = enumerationByName<LoadBalancerResponseType>("response_type", 20)
}
