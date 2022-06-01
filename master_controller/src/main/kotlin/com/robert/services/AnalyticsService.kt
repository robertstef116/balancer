package com.robert.services

import com.robert.AnalyticsEntry
import com.robert.persistance.AnalyticsStorage

class AnalyticsService {
    private val analyticsStorage = AnalyticsStorage()

    fun getAnalytics(from: Long): List<AnalyticsEntry> {
        return analyticsStorage.getAnalytics(from)
    }
}
