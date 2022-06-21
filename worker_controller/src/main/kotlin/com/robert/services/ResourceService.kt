package com.robert.services

import com.robert.resources.performance.WorkerPerformanceData
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import oshi.hardware.CentralProcessor

class ResourceService {
    companion object {
        private val log = LoggerFactory.getLogger(ResourceService::class.java)
        private val si = SystemInfo()
        private val hardware = si.hardware
        private val processor = hardware.processor
        private val memory = hardware.memory
    }

    private var prevTicks = LongArray(CentralProcessor.TickType.values().size)

    fun getResources(): WorkerPerformanceData {
        log.debug("get container resources")
        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100
        prevTicks = processor.systemCpuLoadTicks

        return WorkerPerformanceData(
            processor.logicalProcessorCount,
            cpuLoad,
            memory.available,
            memory.total
        )
    }
}
