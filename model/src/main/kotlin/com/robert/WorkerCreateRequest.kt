package com.robert

data class WorkerCreateRequest(
    val alias: String,
    val host: String,
    val port: Int,
)
