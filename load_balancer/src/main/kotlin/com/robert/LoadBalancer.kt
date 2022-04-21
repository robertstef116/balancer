package com.robert

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket


class LoadBalancer {
    companion object {
        private val log = LoggerFactory.getLogger(LoadBalancer::class.java)

        private fun redirect(
            input: InputStream,
            output: OutputStream,
            buffer: ByteArray,
            trimRequestHeader: Boolean
        ): Boolean {
            var writtenToWorker = false
            var bytesRead: Int
            log.debug("=======")
            var spaces = 0
            var byte: Byte
            val route = StringBuilder()
            var startIdx: Int? = null
            var endIdx: Int? = null
            do {
                bytesRead = input.read(buffer)
                if (bytesRead >= 0) {
                    var i = 0
                    if (trimRequestHeader) {
                        while (i < bytesRead) {
                            byte = buffer[i]
                            if (byte == ' '.code.toByte()) {
                                i++
                                while (buffer[i] == ' '.code.toByte()) {
                                    i++
                                }
                                byte = buffer[i]
                                spaces++
                                if (spaces == 1) {
                                    startIdx = i
                                }
                                if (spaces >= 2) {
                                    endIdx = i
                                    break
                                }
                            }
                            if (byte == '\n'.code.toByte()) {
                                break
                            }
                            if (spaces == 1) {
                                route.append(byte.toInt().toChar())
                            }
                            i++
                        }
                        output.write(buffer, 0, startIdx!!)
                        output.write(buffer, startIdx + 5, bytesRead - 5)
                    } else {
                        output.write(buffer, 0, bytesRead);
                    }
//                    log.debug(buffer.decodeToString())
                    writtenToWorker = true
                }
            } while (input.available() > 0)
            log.debug(route.toString())
            log.debug(startIdx.toString())
            log.debug(endIdx.toString())
            output.flush()
            return writtenToWorker
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
                    launch(Dispatchers.IO) {
                        val buffer = ByteArray(2048)
                        val workerSocket = Socket("localhost", 8444)

                        var streamFromClient: InputStream? = null
                        var streamFromWorker: InputStream? = null
                        var streamToClient: OutputStream? = null
                        var streamToWorker: OutputStream? = null

                        try {
                            streamFromClient = clientSocket.getInputStream()
                            streamFromWorker = workerSocket.getInputStream()
                            streamToClient = clientSocket.getOutputStream()
                            streamToWorker = workerSocket.getOutputStream()


//                                val metrics = HttpTransportMetricsImpl()
//                                val buf = SessionInputBufferImpl(metrics, 2048)
//                                buf.bind(streamFromClient)
//                            log.debug(buf.readLine())
//                                val reqParser = DefaultHttpRequestParser(buf)
//                                reqParser.parse()

                            if (redirect(streamFromClient, streamToWorker, buffer, true)) {
                                redirect(streamFromWorker, streamToClient, buffer, false)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            streamToWorker?.close()
                            streamToClient?.close()
                            streamFromClient?.close()
                            streamFromWorker?.close()
                            clientSocket.close()
                            workerSocket.close()
                        }
                    }
                }
            }
        }
    }
}
