package com.robert

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReadWriteLock {
    private val allowRead = Mutex()
    private val allowWrite = Mutex()
    private val stateLock = Mutex()
    private var readers = 0

    suspend fun <T> read(action: suspend () -> T): T {
        return try {
            allowRead.withLock {
                stateLock.withLock {
                    if (readers++ == 0) { // first reader is locking also the write mutex
                        allowWrite.lock(this)
                    }
                }
            }
            action()
        } finally {
            stateLock.withLock {
                if (--readers == 0) { // last reader is unlocking the write mutex
                    allowWrite.unlock(this)
                }
            }
        }
    }

    suspend fun <T> write(action: suspend () -> T): T {
        // prevent readers from starting
        return allowRead.withLock {
            allowWrite.withLock {
                action()
            }
        }
    }
}
