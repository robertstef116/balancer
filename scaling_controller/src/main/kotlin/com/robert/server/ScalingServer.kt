package com.robert.server

import com.robert.Env
import com.robert.service.ScalingService
import com.robert.logger
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.Dispatchers

class ScalingServer {
    companion object {
        val LOG by logger()
    }

    private val port: Int = Env.getInt("PORT", 8000)

    private val server: Server =
        ServerBuilder
            .forPort(port)
            .addService(ScalingService(Dispatchers.IO))
            .build()

    fun start() {
        server.start()
        LOG.info("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                LOG.info("*** Shutting down gRPC server since JVM is shutting down")
                this@ScalingServer.stop()
                LOG.info("*** Server shut down")
            }
        )
    }

    private fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }
}