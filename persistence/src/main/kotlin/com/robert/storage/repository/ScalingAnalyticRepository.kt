package com.robert.storage.repository

import com.robert.analytics.ScalingAnalytic

interface ScalingAnalyticRepository {
    fun getAll(): Collection<ScalingAnalytic>
    fun create(analytics: ScalingAnalytic)
}