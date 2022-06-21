package com.robert.api.response

data class WorkerCreateRequest(
    val alias: String,
    val host: String,
    val port: Int,
)
