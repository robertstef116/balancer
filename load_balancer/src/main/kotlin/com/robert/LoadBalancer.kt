package com.robert

import com.robert.algorithms.LoadBalancingAlgorithm
import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ValidationException
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

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

class LoadBalancer(resourcesManager: ResourcesManager) {
    companion object {
        private val log = LoggerFactory.getLogger(LoadBalancer::class.java)
        private const val SPACE_CHAR = ' '.code.toByte()
        private const val LF_CHAR = '\n'.code.toByte()
        private const val CR_CHAR = '\r'.code.toByte()

        private fun redirectProcessedHeader(
            output: OutputStream,
            prefixLength: Int,
            headerProcessingResult: HeaderProcessingResult
        ) {
            val prefixEnd = headerProcessingResult.routeBegin + prefixLength
            output.write(headerProcessingResult.bufferRead, 0, headerProcessingResult.routeBegin);
            output.write(headerProcessingResult.bufferRead, prefixEnd, headerProcessingResult.bytesRead - prefixEnd)
        }

        private fun processHeader(input: InputStream, buffer: ByteArray): HeaderProcessingResult {
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
                bytesRead = input.read(buffer)
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
            } while (input.available() > 0)

            return HeaderProcessingResult(route.toString(), begin!!, totalBytesRead, protocol.toString(), tmpBuffer!!)
        }

        private fun redirect(input: InputStream, output: OutputStream, buffer: ByteArray) {
            var bytesRead: Int
            do {
                bytesRead = input.read(buffer)
                if (bytesRead >= 0) {
                    output.write(buffer, 0, bytesRead);
                }
            } while (input.available() > 0)
            output.flush()
        }

        private fun getWorkerSocket(route: String, pathsMapping: Set<WorkflowPath>): WorkerSocketInfo {
            val pathData = pathsMapping.find { route.startsWith(it.path) } ?: throw NotFoundException()
            val deployment = pathData.deploymentSelectionAlgorithm.selectTargetDeployment()
            val socket = Socket(deployment.host, deployment.port)
            return WorkerSocketInfo(
                pathData.path,
                socket,
                deployment,
                pathData.deploymentSelectionAlgorithm
            )
        }

        private fun sendNotFound(stream: OutputStream, protocol: String) {
            stream.write(
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

    init {
        log.debug("initializing load balancer")
        runBlocking {
            coroutineScope {
                val server = withContext(Dispatchers.IO) {
                    ServerSocket(9999)
                }
                while (true) {
                    val clientSocket = withContext(Dispatchers.IO) {
                        server.accept()
                    }

                    log.debug("received a new request")

                    launch(Dispatchers.IO) {
                        var headerProcessingResult: HeaderProcessingResult? = null
                        var workerSocket: Socket? = null
                        var streamFromClient: InputStream? = null
                        var streamFromWorker: InputStream? = null
                        var streamToClient: OutputStream? = null
                        var streamToWorker: OutputStream? = null
                        var workerSocketInfo: WorkerSocketInfo? = null

                        val buffer =
                            ByteArray(DynamicConfigProperties.getIntProperty(Constants.PROCESSING_SOCKET_BUFFER_LENGTH)!!)

                        try {
                            streamFromClient = clientSocket.getInputStream()

                            headerProcessingResult = processHeader(streamFromClient, buffer)
                            workerSocketInfo =
                                getWorkerSocket(headerProcessingResult.route, resourcesManager.pathsMapping.keys)
                            workerSocket = workerSocketInfo.socket
                            val routePrefixLength = workerSocketInfo.routePrefix.length

                            streamToWorker = workerSocket.getOutputStream()
                            redirectProcessedHeader(streamToWorker, routePrefixLength, headerProcessingResult)

                            streamFromWorker = workerSocket.getInputStream()
                            streamToClient = clientSocket.getOutputStream()

                            if (streamFromClient.available() > 0) {
                                redirect(streamFromClient, streamToWorker, buffer)
                            }
                            redirect(streamFromWorker, streamToClient, buffer)
                        } catch (e: ValidationException) {
                            log.debug("Received an invalid request")
                        } catch (e: NotFoundException) {
                            log.debug("No registered worker found for {}", headerProcessingResult?.route)
                            streamToClient = streamToClient ?: clientSocket.getOutputStream()
                            sendNotFound(streamToClient!!, headerProcessingResult?.protocol ?: "HTTP/1.1")
                        } catch (e: Exception) {
                            // TO DO: send Internal Server Error
                            e.printStackTrace()
                        } finally {
                            streamToWorker?.close()
                            streamToClient?.close()
                            streamFromClient?.close()
                            streamFromWorker?.close()
                            clientSocket.close()
                            workerSocket?.close()
                            workerSocketInfo?.algorithm?.registerProcessingFinished(workerSocketInfo.deploymentInfo)
                        }
                    }
                }
            }
        }
    }
}
