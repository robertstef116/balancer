package com.robert

import com.robert.annotations.PulsarListener
import com.robert.annotations.PulsarPersist
import org.apache.pulsar.client.api.*
import org.apache.pulsar.shade.com.fasterxml.jackson.databind.ObjectMapper
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory
import java.util.*

class PulsarService : KoinComponent {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
        private val OBJECT_MAPPER = ObjectMapper()
    }

    private val consumers = mutableSetOf<Consumer<*>>()
    private val producers = mutableMapOf<String, Producer<String>>()
    private val pulsarClient: PulsarClient

    init {
        val pulsarProtocol = Env.get("PULSAR_PROTOCOL")
        val pulsarHost = Env.get("PULSAR_HOST")
        val pulsarPort = Env.get("PULSAR_EXTERNAL_PORT")
        val pulsarUrl = "${pulsarProtocol}://${pulsarHost}:${pulsarPort}"

        pulsarClient = PulsarClient.builder()
            .serviceUrl(pulsarUrl)
            .build()
    }

    fun createConsumers() {
        Reflections("com.robert", Scanners.TypesAnnotated, Scanners.SubTypes)
            .getTypesAnnotatedWith(PulsarListener::class.java)
            .map { clazz ->
                val pulsarListener = clazz.getAnnotation(PulsarListener::class.java)
                val listenerInstance = get<Any>(clazz.kotlin)
                val topic = Env.get(pulsarListener.topic)
                val subscriptionName = pulsarListener.subscriptionName
                val subscriptionPosition = pulsarListener.subscriptionPosition
                val type = pulsarListener.type

                log.info("initializing pulsar listener for topic {} using subscription {}", topic, subscriptionName)
                val methodsStream = Arrays.stream(clazz.methods)
                val persistMethod = methodsStream
                    .filter { it.isAnnotationPresent(PulsarPersist::class.java) }
                    .findAny()
                    .orElse(null)
                val listenerMethod = methodsStream
                    .filter { it.isAnnotationPresent(PulsarPersist::class.java) }
                    .findAny()
                    .orElse(null)

                if (persistMethod == null && listenerMethod == null) {
                    log.warn("no persist or listener method for class {}", clazz.canonicalName)
                    return
                }

                pulsarClient.newConsumer()
                    .topic(topic)
                    .subscriptionName(subscriptionName)
                    .subscriptionInitialPosition(subscriptionPosition)
                    .messageListener { c, msg ->
                        try {
                            val value = OBJECT_MAPPER.readValue(String(msg.value), type.java)
                            log.debug("receive new message on topic {}: {}", topic, value)
                            persistMethod?.invoke(listenerInstance, value)
                            listenerMethod?.invoke(listenerInstance, value)
                            c.acknowledge(msg)
                        } catch (e: Exception) {
                            log.error("error processing message for topic {}", topic, e)
                            c.negativeAcknowledge(msg)
                        }
                    }
                    .subscribe()
            }
            .forEach { consumer ->
                consumers.add(consumer)
            }
        log.info("pulsar listeners initialized")
    }

    @Synchronized
    fun getProducerForTopic(topic: String): Producer<String> {
        return producers.getOrPut(topic) {
            pulsarClient.newProducer(Schema.STRING)
                .topic(topic)
                .compressionType(CompressionType.LZ4)
                .create();
        }
    }

    fun send(topic: String, msg: Any) {
        log.debug("sending message to topic {}: {}", topic, msg)
        val producer = getProducerForTopic(topic)
        producer.sendAsync(OBJECT_MAPPER.writeValueAsString(msg))
    }

    fun destroy() {
        consumers.forEach { consumer ->
            try {
                consumer.close()
            } catch (e: PulsarClientException) {
                log.error("something went wrong closing the consumer", e)
            }
        }
    }
}
