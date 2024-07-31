package com.robert.service

import com.robert.logger
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory

class ResourceService {
    companion object {
        val LOG by logger()
    }

    private val processor: CentralProcessor
    private val memory: GlobalMemory

    init {
        val si = SystemInfo()
        val hardware = si.hardware
        processor = hardware.processor
        memory = hardware.memory
    }

    private var prevTicks = LongArray(CentralProcessor.TickType.entries.size)

    fun getResources(): SystemResources {
        LOG.debug("Getting worker resources")
        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks)
        prevTicks = processor.systemCpuLoadTicks

        return SystemResources(
            cpuLoad,
            memory.available,
            memory.total,
        )
    }
}

data class SystemResources(
    val cpuLoad: Double,
    val availableMemory: Long,
    val totalMemory: Long
)
