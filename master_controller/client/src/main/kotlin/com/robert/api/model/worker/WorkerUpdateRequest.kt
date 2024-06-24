package com.robert.api.model.worker

import com.robert.scaller.WorkerState

data class WorkerUpdateRequest(
    val state: WorkerState,
)
