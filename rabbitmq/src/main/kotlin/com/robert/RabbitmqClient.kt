package com.robert

import com.rabbitmq.client.*
import org.slf4j.LoggerFactory


class RabbitmqClient(private val exchangeName: String) {
    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)

        private val connection: Connection by lazy {
            val factory = ConnectionFactory()
            factory.host = Env.get("RABBITMQ_HOST")
            factory.port = Env.getInt("RABBITMQ_PORT")
            factory.username = Env.get("RABBITMQ_USER")
            factory.password = Env.get("RABBITMQ_PASSWORD")
            factory.isAutomaticRecoveryEnabled = true
            factory.newConnection()
        }

        private val channel: Channel by lazy {
            connection.createChannel()
        }
    }

    init {
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT, true, false, null)
    }

    fun consume(queueName: String, autoDelete: Boolean = false, callback: (String) -> Unit) {
        channel.queueDeclare(queueName, true, false, autoDelete, null)
        channel.queueBind(queueName, exchangeName, "")
        val deliverCallback = DeliverCallback { _: String, delivery: Delivery ->
            val message = String(delivery.body, Charsets.UTF_8)
            LOG.trace("Received '{}' in the exchange {} for queue {}", message, exchangeName, queueName)
            callback(message)
        }
        channel.basicConsume(queueName, true, deliverCallback) { _ -> }
    }

    fun produce(message: String) {
        channel.basicPublish(exchangeName, "", null, message.toByteArray(Charsets.UTF_8))
    }
}
