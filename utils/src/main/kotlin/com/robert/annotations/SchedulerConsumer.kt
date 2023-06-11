package com.robert.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.FUNCTION])
annotation class SchedulerConsumer(val name: String, val interval: String)
