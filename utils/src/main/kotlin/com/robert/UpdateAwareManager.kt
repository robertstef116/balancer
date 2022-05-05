package com.robert

abstract class UpdateAwareManager(val key: String) {
    abstract fun refresh()
}
