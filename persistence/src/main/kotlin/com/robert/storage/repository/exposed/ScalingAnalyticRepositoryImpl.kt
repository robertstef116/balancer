package com.robert.storage.repository.exposed

import com.robert.analytics.ScalingAnalytic
import com.robert.storage.entities.ScalingAnalytics
import com.robert.storage.repository.ScalingAnalyticRepository
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ScalingAnalyticRepositoryImpl : ScalingAnalyticRepository {
    override fun getAll(): Collection<ScalingAnalytic> = transaction {
        ScalingAnalytics.selectAll().map {
            ScalingAnalytic(
                workflowId = it[ScalingAnalytics.workflowId],
                replicas = it[ScalingAnalytics.replicas],
                avgCpu = it[ScalingAnalytics.avgCpu],
                avgMemory = it[ScalingAnalytics.avgMemory],
                timestamp = it[ScalingAnalytics.timestamp],
            )
        }
    }

    override fun create(analytics: ScalingAnalytic): Unit = transaction {
        ScalingAnalytics.insert {
            it[workflowId] = analytics.workflowId
            it[replicas] = analytics.replicas
            it[avgCpu] = analytics.avgCpu
            it[avgMemory] = analytics.avgMemory
            it[timestamp] = analytics.timestamp
        }
    }
}