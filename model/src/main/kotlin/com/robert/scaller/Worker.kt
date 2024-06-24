package com.robert.scaller

import java.util.UUID

data class Worker (
    val id: UUID,
    var alias: String,
    var state: WorkerState
)
