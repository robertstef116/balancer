package com.robert

data class WorkerNode(
    val id: String,
    val alias: String,
    val host: String,
    val port: Int,
    val status: WorkerNodeStatus
)
