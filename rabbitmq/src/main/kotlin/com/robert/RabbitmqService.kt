package com.robert

import org.koin.core.component.KoinComponent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory
import java.util.Arrays

class RabbitmqService : KoinComponent {
    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
    }

    private val clients = mutableMapOf<String, RabbitmqClient>()

    fun createConsumers() {
        Reflections("com.robert", Scanners.TypesAnnotated, Scanners.SubTypes)
            .getTypesAnnotatedWith(RabbitmqProcessor::class.java)
            .map { clazz ->
                val consumer = get<Any>(clazz.kotlin)
                val consumeMethod = Arrays.stream(clazz.methods)
                    .filter { it.isAnnotationPresent(RabbitmqConsumer::class.java) }
                    .findAny()
                    .orElse(null)

                if (consumeMethod == null) {
                    LOG.warn("No consume method for RabbitMQ processor {}", clazz.canonicalName)
                    return
                }
                val consumerAnnotation = consumeMethod.getAnnotation(RabbitmqConsumer::class.java)
                val queueName = consumerAnnotation.queueName
                val exchangeName = Env.get(consumerAnnotation.exchangeNameKey)

                LOG.info("Initializing RabbitMQ listener for the exchange {} using the queue {}", exchangeName, queueName)
                val client = getClient(exchangeName)
                client.consume(queueName) { msg ->
                    LOG.debug("Consuming message received in the exchange {} for queue {}", exchangeName, queueName)
                    consumeMethod.invoke(consumer, msg)
                }
            }
    }

    @Synchronized
    private fun getClient(exchangeName: String): RabbitmqClient {
        return clients.getOrPut(exchangeName) { RabbitmqClient(exchangeName) }
    }

    fun produce(exchangeName: String, message: String) {
        getClient(exchangeName).produce(message)
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.CLASS])
annotation class RabbitmqProcessor()

@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.FUNCTION])
annotation class RabbitmqConsumer(val exchangeNameKey: String, val queueName: String)
