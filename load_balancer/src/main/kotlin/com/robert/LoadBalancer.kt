package com.robert

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
    val begin: Int,
    val bytesRead: Int,
    @Suppress("ArrayInDataClass")
    val bufferRead: ByteArray
)

private data class WorkerSocketInfo(
    val routePrefix: String,
    val socket: Socket
)

class LoadBalancer {
    companion object {
        private val log = LoggerFactory.getLogger(LoadBalancer::class.java)
        private const val SPACE_CHAR = ' '.code.toByte()
        private const val NEW_LINE_CHAR = '\n'.code.toByte()

        private val prefixes = listOf("/test")

        private fun redirectProcessedHeader(
            output: OutputStream,
            prefixLength: Int,
            headerProcessingResult: HeaderProcessingResult
        ) {
            val prefixEnd = headerProcessingResult.begin + prefixLength
            output.write(headerProcessingResult.bufferRead, 0, headerProcessingResult.begin);
            output.write(headerProcessingResult.bufferRead, prefixEnd, headerProcessingResult.bytesRead - prefixEnd)
        }

        private fun processHeader(input: InputStream, buffer: ByteArray): HeaderProcessingResult {
            val route = StringBuilder()
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
                    when (byte) {
                        NEW_LINE_CHAR -> break@processingLoop
                        SPACE_CHAR -> {
                            spacesRead++ // skip space separator
                            i++
                            if (spacesRead == 1) {
                                begin = globalIdx + i + 1
                                continue
                            } else {
                                break@processingLoop
                            }
                        }
                    }

                    if (spacesRead == 1) {
                        route.append(byte.toInt().toChar())
                    }
                    i++
                }
                globalIdx += i
            } while (input.available() > 0)

            return HeaderProcessingResult(route.toString(), begin!!, totalBytesRead, tmpBuffer!!)
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

        private fun getWorkerSocket(route: String): WorkerSocketInfo {
            val routePrefix = prefixes.find { route.startsWith(it) } ?: throw NotFoundException()
            val socket = Socket("localhost", 8444)
            return WorkerSocketInfo(routePrefix, socket)
        }
    }

    fun init() {
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

                        val buffer = ByteArray(2048)

                        try {
                            streamFromClient = clientSocket.getInputStream()

                            headerProcessingResult = processHeader(streamFromClient, buffer)
                            val workerSocketInfo = getWorkerSocket(headerProcessingResult.route)
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
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            streamToWorker?.close()
                            streamToClient?.close()
                            streamFromClient?.close()
                            streamFromWorker?.close()
                            clientSocket.close()
                            workerSocket?.close()
                        }
                    }
                }
            }
        }
    }
}
