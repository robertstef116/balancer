package com.robert.scaller

import java.util.UUID

data class WorkerR (
    val id: UUID,
    var alias: String,
    val host: String,
    val port: Int,
    var status: WorkerStatusR
)
