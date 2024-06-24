package com.robert.resources

import com.robert.enums.WorkerStatusDepr

data class Worker(
    val id: String,
    val alias: String,
    val host: String,
    val port: Int,
    val status: WorkerStatusDepr
)
