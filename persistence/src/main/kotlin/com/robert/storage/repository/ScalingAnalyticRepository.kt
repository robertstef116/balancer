package com.robert.storage.repository

import com.robert.scaller.ScalingAnalytic

interface ScalingAnalyticRepository {
    fun getAll(): Collection<ScalingAnalytic>
    fun create(analytics: ScalingAnalytic)
}