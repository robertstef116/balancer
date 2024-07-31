package com.robert.resources

import com.robert.enums.WorkerState
import java.util.*

data class Worker(
    val id: UUID,
    var alias: String,
    var state: WorkerState
)
