package com.robert.scaller

import java.util.UUID

data class WorkerR (
    val id: UUID,
    val alias: String,
    val host: String,
    val port: Int,
    val status: WorkerStatusR
)
