package com.robert.balancing

data class ProcessedHeader(
    val route: String,
    val routeBegin: Int,
    val bytesRead: Int,
    val protocol: String,
    val method: String,
    @Suppress("ArrayInDataClass")
    val bufferRead: ByteArray
)
