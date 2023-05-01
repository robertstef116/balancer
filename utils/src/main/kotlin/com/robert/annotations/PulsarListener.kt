package com.robert.annotations

import org.apache.pulsar.client.api.SubscriptionInitialPosition
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.CLASS])
annotation class PulsarListener(val topic: String, val subscriptionName: String, val subscriptionPosition: SubscriptionInitialPosition, val type: KClass<*>)
