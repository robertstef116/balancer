package com.robert.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SchedulerConsumer(val name: String, val interval: String)
