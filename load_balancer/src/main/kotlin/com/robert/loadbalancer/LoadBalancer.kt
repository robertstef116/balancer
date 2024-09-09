package com.robert.loadbalancer

import com.robert.Env
import com.robert.analytics.LoadBalancerAnalytic
import com.robert.balancing.LoadBalancerResponseType
import com.robert.exceptions.ConnectionClosedException
import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ValidationException
import com.robert.loadbalancer.algorithm.BalancingAlgorithm
import com.robert.loadbalancer.model.HostPortPair
import com.robert.logger
import com.robert.storage.repository.LoadBalancerAnalyticRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.ServerSocket
import java.net.Socket
import java.time.Instant
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class LoadBalancer : KoinComponent {
    companion object {
        private val LOG by logger()

        private const val LF: Byte = 0xA
        private const val CR: Byte = 0xD
        private const val DEFAULT_HTTP_VERSION = "HTTP/1.1"

        private val LF_CHAR: Char = Char(LF.toInt())
        private val CR_CHAR: Char = Char(CR.toInt())
        private val LINE_END = "$CR_CHAR$LF_CHAR".toByteArray()

        private val CONTENT_TEMPL = "%s %s %s$CR_CHAR$LF_CHAR" +
                "Server: load-balancer/1.0.0$CR_CHAR$LF_CHAR" +
                "Content-Type: text/html$CR_CHAR$LF_CHAR" +
                "$CR_CHAR$LF_CHAR" +
                "<html>" +
                "<head><title>%s</title></head>" +
                "<body>" +
                "<center><h1>%s</h1></center>" +
                "<hr><center>load-balancer/1.0.0</center>" +
                "</body>" +
                "</html>"

        private val NOT_FOUND_CONTENT_TEMPL = CONTENT_TEMPL.format("%s", 404, "Not Found", "Not Found", "Oops, target not found")
        private val INTERNAL_SERVER_ERROR_CONTENT_TEMPL = CONTENT_TEMPL.format("%s", 500, "Internal Server Error", "Internal Server Error", "Oops, something went wrong")
        private val BAD_REQUEST_ERROR_CONTENT_TEMPL = CONTENT_TEMPL.format("%s", 400, "Bad Request", "Bad Request", "Oops, something was wrong with the request")
    }

    private val requestHandlerProvider by inject<RequestHandlerProvider>()
    private val loadBalancerAnalyticRepository by inject<LoadBalancerAnalyticRepository>()

    private val processingSocketBufferLength = Env.getInt("PROCESSING_SOCKET_BUFFER_LENGTH", 2000)

    //    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val scope = CoroutineScope(Executors.newFixedThreadPool(250).asCoroutineDispatcher())

    fun start() {
        val port = Env.getInt("PORT", 8001)
        val backlog = Env.getInt("INCOMING_QUEUE_BACKLOG", 1000) // default 50
        LOG.info("Starting load balancer on port {}", port.toString())

        val serverSocket = ServerSocket(port, backlog)

        while (true) {
            LOG.trace("Waiting new connection")
            handleRequest(serverSocket.accept())
        }
    }

    private fun handleRequest(socket: Socket) {
        scope.launch {
            var lbUri = ""
            var target: HostPortPair? = null
            var assigner: BalancingAlgorithm? = null
            var responseType = LoadBalancerResponseType.OK

            var retryTime = 0L
            val responseTime = measureTimeMillis {
                try {
                    socket.use {
                        socket.getInputStream().use { input ->
                            socket.getOutputStream().use { output ->
                                var version = DEFAULT_HTTP_VERSION
                                try {
                                    val buffer = ByteArray(processingSocketBufferLength)
                                    val (requestHeaders, requestBody) = readFromStream(input, buffer)
                                    updateRequestLineHeader(requestHeaders).also {
                                        lbUri = it.first
                                        version = it.second
                                    }

                                    assigner = requestHandlerProvider.getAssigner(lbUri).also { assigner ->
                                        var connected = false
                                        val unresponsiveTargets = mutableSetOf<HostPortPair>()
                                        while (!connected) {
                                            target = assigner.getTarget(unresponsiveTargets).also {
                                                LOG.debug("Selected target {}", it)
                                                val connectionTime = measureTimeMillis {
                                                    try {
                                                        Socket(it.host, it.port).use { targetSocket ->
                                                            targetSocket.getOutputStream().use { targetOutputStream ->
                                                                writeToStream(targetOutputStream, requestHeaders, requestBody)

                                                                targetSocket.getInputStream().use { targetInputStream ->
                                                                    val (responseHeaders, responseBody) = readFromStream(targetInputStream, buffer)
                                                                    writeToStream(output, responseHeaders, responseBody)
                                                                }
                                                            }
                                                        }
                                                        connected = true
                                                    } catch (e: ConnectException) {
                                                        unresponsiveTargets.add(it)
                                                        LOG.warn("Unable to connect to target {}, selecting different target, retry count: {}", it, unresponsiveTargets.size)
                                                        assigner.addResponseTimeData(it, -1, LoadBalancerResponseType.SERVER_ERROR)
                                                    }
                                                }
                                                if (!connected) {
                                                    retryTime += connectionTime
                                                }
                                            }
                                        }
                                    }
                                    LOG.trace("Done on path {}", lbUri)
                                } catch (e: ConnectionClosedException) {
                                    responseType = LoadBalancerResponseType.CONNECTION_CLOSED
                                    LOG.debug("Client closed the connection on path: {}", lbUri)
                                } catch (e: NotFoundException) {
                                    responseType = LoadBalancerResponseType.NOT_FOUND
                                    LOG.warn("Received request to unknown path: {}", lbUri)
                                    writeContentToStream(NOT_FOUND_CONTENT_TEMPL.format(version), output)
                                } catch (e: ValidationException) {
                                    responseType = LoadBalancerResponseType.INVALID
                                    LOG.warn("Received an invalid request: {}", e.message)
                                    writeContentToStream(BAD_REQUEST_ERROR_CONTENT_TEMPL.format(version), output)
                                } catch (e: Exception) {
                                    responseType = LoadBalancerResponseType.SERVER_ERROR
                                    LOG.warn("Error", e)
                                    writeContentToStream(INTERNAL_SERVER_ERROR_CONTENT_TEMPL.format(version), output)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    responseType = LoadBalancerResponseType.ERROR
                    LOG.error("Unexpected error occurred", e)
                }
            }

            target?.also { targetData ->
                assigner?.also { it.addResponseTimeData(targetData, responseTime - retryTime, responseType) }
                if (lbUri.isNotEmpty()) {
                    targetData.also { loadBalancerAnalyticRepository.create(LoadBalancerAnalytic(it.workflowId, lbUri, responseTime, Instant.now().toEpochMilli(), responseType)) }
                }
            }
        }
    }

    private fun readFromStream(input: InputStream, buffer: ByteArray): Pair<MutableList<ByteArray>, ByteArray> {
        var from = 0
        var currentReadSize = readFromStreamRaw(input, buffer)
        var line = ByteArray(0)
        var bodySize = 0
        val headers = mutableListOf<ByteArray>()
        var chunked = false

        while (true) {
            val c = buffer[from]
            from++
            if (from >= currentReadSize) {
                currentReadSize = readFromStreamRaw(input, buffer)
                from = 0
            }

            if (c == CR && buffer[from] == LF) {
                from++
                if (line.isEmpty()) {
                    break
                }
                if (from >= currentReadSize) {
                    currentReadSize = readFromStreamRaw(input, buffer)
                    from = 0
                }

                headers.add(line)
                if (bodySize == 0) {
                    line.toString(Charsets.UTF_8).also {
                        if (it.startsWith("Content-Length:")) {
                            bodySize = it.split(':')[1].trim().toInt()
                        } else if (it.startsWith("Transfer-Encoding: chunked")) {
                            chunked = true
                        }
                    }
                }
                line = ByteArray(0)
            } else {
                line += c
            }
        }

        var body = ByteArray(0)
        if (chunked) {
            var charsInChunk = 0
            while (true) {
                val c = buffer[from]
                from++
                charsInChunk++
                if (from >= currentReadSize) {
                    currentReadSize = readFromStreamRaw(input, buffer)
                    if (currentReadSize == -1) {
                        body += c
                        break
                    }
                    from = 0
                }
                if (c == CR && buffer[from] == LF) {
                    if (charsInChunk == 1) {
                        body += CR
                        body += LF
                        break
                    }
                    charsInChunk = 0
                }
                body += c
            }
        } else {
            body += buffer.copyOfRange(from, currentReadSize)
            var remaining = bodySize - (currentReadSize - from)
            while (remaining > 0) {
                remaining -= readFromStreamRaw(input, buffer)
                body += buffer
            }
        }
        return Pair(headers, body)
    }

    private fun writeToStream(stream: OutputStream, headers: List<ByteArray>, body: ByteArray) {
        var out = ByteArray(0)
        headers.forEach { out += it + LINE_END }
        out += LINE_END // End of headers
        out += body
        stream.write(out)
    }

    private fun updateRequestLineHeader(headers: MutableList<ByteArray>): Pair<String, String> {
        if (headers.isEmpty()) {
            throw ValidationException("No headers found")
        }

        val requestLineSplits = headers[0].toString(Charsets.UTF_8).split(" ")
        if (requestLineSplits.size != 3) {
            throw ValidationException("Invalid request line length")
        }

        val uri = requestLineSplits[1]
        val version = requestLineSplits[2]

        val uriSplitIdx = uri.indexOf('/', 1)
        return if (uriSplitIdx == -1) {
            Pair(uri, version)
        } else {
            Pair(uri.substring(0, uriSplitIdx), version)
        }
    }

    private fun readFromStreamRaw(input: InputStream, buffer: ByteArray): Int {
        return input.read(buffer)
    }

    private fun writeContentToStream(content: String, out: OutputStream) {
        out.write(content.toByteArray())
    }
}

