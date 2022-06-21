package com.robert.resources

import com.robert.enums.WorkerStatus

data class Worker(
    val id: String,
    val alias: String,
    val host: String,
    val port: Int,
    val status: WorkerStatus
)
