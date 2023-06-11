package com.robert

import com.robert.annotations.Scheduler
import com.robert.annotations.SchedulerConsumer
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Duration

class SchedulerService : KoinComponent {
    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
    }
    private val jobs = arrayListOf<Job>()

    fun createSchedulers() {
        Reflections("com.robert", Scanners.TypesAnnotated, Scanners.SubTypes)
            .getTypesAnnotatedWith(Scheduler::class.java)
            .map { clazz ->
                Arrays.stream(clazz.methods)
                    .filter { it.isAnnotationPresent(SchedulerConsumer::class.java) }
                    .forEach{method ->
                        val schedulerConsumer = method.getAnnotation(SchedulerConsumer::class.java)
                        val scheduler = get<Any>(clazz.kotlin)
                        val schedulerName = schedulerConsumer.name
                        val interval = Env.get(schedulerConsumer.interval)
                        val executionInterval = Duration.parse(interval).inWholeMilliseconds
                        LOG.info("Initializing scheduler {} with interval {}", clazz.canonicalName, schedulerConsumer.interval)
                        val job = CoroutineScope(Dispatchers.IO).launch {
                            while (true) {
                                method.invoke(scheduler)
                                LOG.info("Scheduler '{}' done, next check in {} ms", schedulerName, executionInterval)
                                delay(executionInterval)
                            }
                        }
                        jobs.add(job)
                    }
            }
        LOG.info("Schedules initialized")
    }

    suspend fun wait() {
        jobs.forEach {
            it.join()
        }
    }
}
