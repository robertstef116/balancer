package com.robert

import com.robert.annotations.Scheduler
import com.robert.annotations.SchedulerConsumer
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.util.*
import kotlin.time.Duration

class SchedulerService : KoinComponent {
    companion object {
        val LOG by logger()
    }

    private val jobs = arrayListOf<Job>()

    fun createSchedulers() {
        Reflections("com.robert", Scanners.TypesAnnotated, Scanners.SubTypes)
            .getTypesAnnotatedWith(Scheduler::class.java)
            .map { clazz ->
                Arrays.stream(clazz.methods)
                    .filter { it.isAnnotationPresent(SchedulerConsumer::class.java) }
                    .forEach { method ->
                        val schedulerConsumer = method.getAnnotation(SchedulerConsumer::class.java)
                        val scheduler = get<Any>(clazz.kotlin)
                        val schedulerName = schedulerConsumer.name
                        val interval = Env.get(schedulerConsumer.interval)
                        val executionInterval = Duration.parse(interval).inWholeMilliseconds
                        LOG.info("Initializing scheduler {} with the interval of {}", clazz.canonicalName, interval)
                        val job = CoroutineScope(Dispatchers.IO).launch {
                            while (true) {
                                try {
                                    method.invoke(scheduler)
                                    LOG.info("Scheduler '{}' done, next check in {}", schedulerName, interval)
                                } catch (e: Exception) {
                                    LOG.warn("Unable to execute scheduler '{}', retrying in {}: {}", schedulerName, interval, e.message ?: e.cause?.message ?: "")
                                }
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
