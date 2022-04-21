package com.robert

open class UpdateAwareService {
    private var version = 0

    fun getCurrentVersion() = Update(Constants.HASH, version)

    protected fun markChange() = version++
}
