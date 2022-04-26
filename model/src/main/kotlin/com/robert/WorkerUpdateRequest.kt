package com.robert

data class WorkerUpdateRequest(
    val alias: String?,
    val port: Int?,
    val inUse: Boolean?
)
