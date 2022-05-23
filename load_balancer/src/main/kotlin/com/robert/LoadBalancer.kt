package com.robert

import com.robert.algorithms.LoadBalancingAlgorithm
import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ValidationException
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.properties.Delegates

private data class HeaderProcessingResult(
    val route: String,
    val routeBegin: Int,
    val bytesRead: Int,
    val protocol: String,
    @Suppress("ArrayInDataClass")
    val bufferRead: ByteArray
)

private data class WorkerSocketInfo(
    val routePrefix: String,
    val socket: Socket,
    val deploymentInfo: SelectedDeploymentInfo,
    val algorithm: LoadBalancingAlgorithm
)

class LoadBalancer(private val resourcesManager: ResourcesManager) : BackgroundService {
    companion object {
        private val log = LoggerFactory.getLogger(LoadBalancer::class.java)
        private const val SPACE_CHAR = ' '.code.toByte()
        private const val LF_CHAR = '\n'.code.toByte()
        private const val CR_CHAR = '\r'.code.toByte()
        private val selectorManager = ActorSelectorManager(Dispatchers.IO)

        private suspend fun redirectProcessedHeader(output: ByteWriteChannel, prefixLength: Int, headerProcessingResult: HeaderProcessingResult) {
            val prefixEnd = headerProcessingResult.routeBegin + prefixLength
            output.writeAvailable(headerProcessingResult.bufferRead, 0, headerProcessingResult.routeBegin);
            output.writeAvailable(headerProcessingResult.bufferRead, prefixEnd, headerProcessingResult.bytesRead - prefixEnd)
        }

        private suspend fun processHeader(input: ByteReadChannel, buffer: ByteArray): HeaderProcessingResult {
            val route = StringBuilder()
            val protocol = StringBuilder()
            var totalBytesRead = 0
            var spacesRead = 0
            var globalIdx = 0
            var tmpBuffer: ByteArray? = null
            var begin: Int? = null
            var bytesRead: Int
            var i: Int
            var byte: Byte

            processingLoop@ do {
                bytesRead = input.readAvailable(buffer)
                tmpBuffer = tmpBuffer?.plus(buffer) ?: buffer.clone()
                totalBytesRead += (if (bytesRead > 0) bytesRead else 0)

                if (totalBytesRead <= 0) {
                    throw ValidationException("Bad request")
                }

                i = 0
                while (i < bytesRead) {
                    byte = buffer[i]

                    if (byte == LF_CHAR || byte == CR_CHAR) {
                        break@processingLoop
                    } else if (byte == SPACE_CHAR) {
                        spacesRead++ // skip space separator
                        if (spacesRead == 1) {
                            begin = globalIdx + i + 1
                        }
                        i++
                        continue
                    }

                    if (spacesRead == 1) {
                        route.append(byte.toInt().toChar())
                    } else if (spacesRead == 2) {
                        protocol.append(byte.toInt().toChar())
                    }

                    i++
                }
                globalIdx += i
            } while (input.availableForRead > 0)

            return HeaderProcessingResult(route.toString(), begin!!, totalBytesRead, protocol.toString(), tmpBuffer!!)
        }

        private suspend fun redirect(input: ByteReadChannel, output: ByteWriteChannel, buffer: ByteArray) {
            var bytesRead: Int
            do {
                bytesRead = input.readAvailable(buffer)
                if (bytesRead >= 0) {
                    output.writeAvailable(buffer, 0, bytesRead);
                }
            } while (input.availableForRead > 0)
            output.flush()
        }

        private suspend fun getWorkerSocket(route: String, pathsMapping: Set<WorkflowPath>): WorkerSocketInfo {
            val pathData = pathsMapping.find { route.startsWith(it.path) } ?: throw NotFoundException()
            val deployment = pathData.deploymentSelectionAlgorithm.selectTargetDeployment()
            log.debug("selected deployment running at host {} on port {}", deployment.host, deployment.port)
            val socket = aSocket(selectorManager).tcp().connect(deployment.host, deployment.port)

            return WorkerSocketInfo(
                pathData.path,
                socket,
                deployment,
                pathData.deploymentSelectionAlgorithm
            )
        }
    }

    private var bufferSize by Delegates.notNull<Int>()

    override fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            suspendCoroutine<Unit> {
                resourcesManager.registerInitializationWaiter { it.resume(Unit) }
            }
            run()
        }
    }

    private suspend fun run() {
        log.debug("starting load balancer")

        val server = aSocket(selectorManager).tcp().bind("127.0.0.1", 9999)

        while (true) {
            val clientSocket = server.accept()

            log.debug("received a new request")

            CoroutineScope(Dispatchers.IO).launch {
                var headerProcessingResult: HeaderProcessingResult? = null
                var workerSocket: Socket? = null
                val streamFromClient: ByteReadChannel?
                val streamFromWorker: ByteReadChannel?
                var streamToClient: ByteWriteChannel? = null
                var streamToWorker: ByteWriteChannel? = null
                var workerSocketInfo: WorkerSocketInfo? = null

                val buffer = ByteArray(bufferSize)

                try {
                    streamFromClient = clientSocket.openReadChannel()

                    headerProcessingResult = processHeader(streamFromClient, buffer)
                    workerSocketInfo = getWorkerSocket(headerProcessingResult.route, resourcesManager.pathsMapping.keys)
                    workerSocket = workerSocketInfo.socket
                    val routePrefixLength = workerSocketInfo.routePrefix.length

                    streamToWorker = workerSocket.openWriteChannel(true)
                    redirectProcessedHeader(streamToWorker, routePrefixLength, headerProcessingResult)

                    streamFromWorker = workerSocket.openReadChannel()
                    streamToClient = clientSocket.openWriteChannel(true)

                    if (streamFromClient.availableForRead > 0) {
                        redirect(streamFromClient, streamToWorker, buffer)
                    }

                    redirect(streamFromWorker, streamToClient, buffer)
                } catch (e: ValidationException) {
                    log.debug("Received an invalid request")
                } catch (e: NotFoundException) {
                    log.debug("No registered worker found for {}", headerProcessingResult?.route)
                    streamToClient = streamToClient ?: clientSocket.openWriteChannel(true)
                    LoadBalancerUtils.sendNotFound(streamToClient, headerProcessingResult?.protocol ?: "HTTP/1.1")
                } catch (e: Exception) {
                    // TO DO: send Internal Server Error
                    e.printStackTrace()
                } finally {
                    streamToWorker?.close()
                    streamToClient?.close()
                    withContext(Dispatchers.IO) {
                        clientSocket.close()
                        workerSocket?.close()
                    }
                    workerSocketInfo?.algorithm?.registerProcessingFinished(workerSocketInfo.deploymentInfo)
                }
            }
        }
    }

    fun reloadDynamicConfigs() {
        log.debug("load configs")
        bufferSize = DynamicConfigProperties.getIntPropertyOrDefault(Constants.PROCESSING_SOCKET_BUFFER_LENGTH, 1024)
    }
}

private object LoadBalancerUtils {
    private val log = LoggerFactory.getLogger(LoadBalancerUtils::class.java)

    suspend fun sendNotFound(stream: ByteWriteChannel, protocol: String) {
        stream.writeFully(
            """
                $protocol 404 Not Found
                Server: load-balancer/1.0.0
                Content-Type: text/html
                Content-Length: 166
                
                <html>
                <head><title>Not found</title></head>
                <body>
                <center><h1>Oops, request not found</h1></center>
                <hr><center>load-balancer/1.0.0</center>
                </body>
                </html>
            """.trimIndent().toByteArray()
        )
        log.debug("not found sent")
    }
}
