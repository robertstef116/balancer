package com.robert

import java.util.concurrent.atomic.AtomicInteger

open class UpdateAwareService(val key: String) {
    private val version = AtomicInteger(0)

    fun getVersion() = version.get()

    protected fun markChange() = version.incrementAndGet()
}
