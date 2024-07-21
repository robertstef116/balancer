package com.robert.storage.entities

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object ScalingAnalytics : Table("scaling_analytics") {
    val workflowId = reference("workflow_id", Workflows.id, onDelete = ReferenceOption.CASCADE)
    val replicas = integer("replicas")
    val avgCpu = double("avg_cpu")
    val avgMemory = double("avg_memory")
    val timestamp = long("timestamp_ms")
}