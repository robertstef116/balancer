package com.robert.storage.repository.exposed

import com.robert.analytics.LoadBalancerAnalytic
import com.robert.storage.entities.LoadBalancerAnalytics
import com.robert.storage.repository.LoadBalancerAnalyticRepository
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class LoadBalancerAnalyticRepositoryImpl : LoadBalancerAnalyticRepository {
    override fun getAll(): Collection<LoadBalancerAnalytic> = transaction {
        LoadBalancerAnalytics.selectAll().map {
            LoadBalancerAnalytic(
                workflowId = it[LoadBalancerAnalytics.workflowId],
                path = it[LoadBalancerAnalytics.path],
                responseTime = it[LoadBalancerAnalytics.responseTime],
                timestamp = it[LoadBalancerAnalytics.timestamp],
                responseType = it[LoadBalancerAnalytics.responseType],
            )
        }
    }

    override fun create(analytics: LoadBalancerAnalytic): Unit = transaction {
        LoadBalancerAnalytics.insert {
            it[workflowId] = analytics.workflowId
            it[path] = analytics.path
            it[responseTime] = analytics.responseTime
            it[timestamp] = analytics.timestamp
            it[responseType] = analytics.responseType
        }
    }
}