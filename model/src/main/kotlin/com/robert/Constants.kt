package com.robert

import java.util.UUID

object Constants {
    @JvmField
    val HASH = UUID.randomUUID().toString()
    const val MANAGED_CONTAINER_LABEL = "MANAGED_BY_BALANCER"
}
