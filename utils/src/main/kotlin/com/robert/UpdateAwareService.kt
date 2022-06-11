package com.robert

import java.time.Instant

open class UpdateAwareService(val key: String) {
    protected fun markChange() {
        val now = Instant.now().epochSecond

        StorageUtils.executeInsert("INSERT INTO metadata(key, value) VALUES ('$key', '$now') ON CONFLICT (key) DO UPDATE SET value = '$now'")
    }
}
