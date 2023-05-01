package com.robert.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.FUNCTION])
annotation class PulsarConsumer()
