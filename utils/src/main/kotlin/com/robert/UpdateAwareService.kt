package com.robert

open class UpdateAwareService(key: String) {
    private var version = 0

    fun getCurrentVersion() = Update(Constants.HASH, version)

    protected fun markChange() = version++
}
