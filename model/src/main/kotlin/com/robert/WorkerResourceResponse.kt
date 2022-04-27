package com.robert

data class WorkerResourceResponse(
    val resourcesInfo: ResourcesInfo,
    val containersStats: List<DockerContainerStats>
)
