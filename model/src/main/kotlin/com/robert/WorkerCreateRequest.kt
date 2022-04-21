package com.robert

data class WorkerCreateRequest(
    val alias: String,
    val ip: String,
    val inUse: Boolean
)
