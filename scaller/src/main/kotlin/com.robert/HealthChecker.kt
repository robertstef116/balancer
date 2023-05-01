package com.robert

import com.robert.annotations.Scheduler
import com.robert.annotations.SchedulerConsumer
import com.robert.api.response.WorkerResourceResponse
import com.robert.resources.Worker
import io.ktor.client.call.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

@Scheduler
class HealthChecker : KoinComponent {
    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
        private val checkTimeout = Env[Constants.HEALTH_CHECK_TIMEOUT, 10000L]

        private suspend fun doHealthCheck(worker: Worker): WorkerResourceResponse {
            LOG.debug("health check on worker {}", worker.alias)
            val url = "http://${worker.host}:${worker.port}/resource"
            return HttpClient.get<WorkerResourceResponse>(url, checkTimeout).body()
        }
    }

    private val storage: Storage by inject()

    var workers: List<Worker> = storage.getWorkers()

    @SchedulerConsumer(interval = "\${${Constants.HEALTH_CHECK_INTERVAL}:30s}")
    fun checkWorkers() {
        for (worker in workers) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val res = doHealthCheck(worker)
                } catch (e : Exception) {

                }
            }
        }
        println("test")
    }
}
