package com.robert.storage.repository

import com.robert.balancing.LoadBalancerAnalytic

interface LoadBalancerAnalyticRepository {
    fun getAll(): Collection<LoadBalancerAnalytic>
    fun create(analytics: LoadBalancerAnalytic)
}