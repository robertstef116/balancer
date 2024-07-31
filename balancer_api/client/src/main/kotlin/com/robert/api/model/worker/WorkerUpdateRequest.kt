package com.robert.api.model.worker

import com.robert.enums.WorkerState

data class WorkerUpdateRequest(
    val state: WorkerState,
)
