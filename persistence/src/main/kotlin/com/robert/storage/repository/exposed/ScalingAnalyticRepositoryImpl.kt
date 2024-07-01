package com.robert.storage.repository.exposed

import com.robert.scaller.ScalingAnalytic
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
                timestamp = it[ScalingAnalytics.timestamp],
            )
        }
    }

    override fun create(analytics: ScalingAnalytic): Unit = transaction {
        ScalingAnalytics.insert {
            it[workflowId] = analytics.workflowId
            it[replicas] = analytics.replicas
            it[timestamp] = analytics.timestamp
        }
    }
}