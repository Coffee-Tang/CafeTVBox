package dev.anilbeesetti.nextplayer.core.media.network.proxy

import android.net.Uri
import dev.anilbeesetti.nextplayer.core.media.network.NetworkClient
import dev.anilbeesetti.nextplayer.core.media.network.NetworkClientFactory
import dev.anilbeesetti.nextplayer.core.media.network.networkVideoMimeType
import dev.anilbeesetti.nextplayer.core.model.NetworkConnection
import fi.iki.elonen.NanoHTTPD
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** A remote file to stream: [path] locates it on the connection, [name] gives it a type and title. */
data class NetworkStreamRequest(val path: String, val name: String)

/**
 * A local HTTP server on `127.0.0.1` that bridges network protocols (SMB/FTP/WebDAV) to plain HTTP
 * so the Media3 player can stream and seek remote files without a custom data source.
 *
 * A caller [registerStreams] the files of one playback session and receives a
 * `http://127.0.0.1:<port>/<id>/<name>` URL for each. Incoming HTTP range requests are translated
 * into offset reads on the session's [NetworkClient].
 *
 * A session's files share one connection, and registering a new session releases the previous one:
 * its URLs stop working, which is what leaving those files behind means.
 *
 * The NanoHTTPD server is composed (not inherited) so the dependency does not leak to callers.
 */
@Singleton
class NetworkStreamingProxy @Inject constructor(
    private val clientFactory: NetworkClientFactory,
) {

    private data class StreamInfo(val path: String, val mimeType: String)

    /**
     * The files of one playback session. The player plays one at a time, but may reach for the next
     * before letting go of the current one, so they all stay registered until the session ends.
     */
    private class Session(val client: NetworkClient) {
        val initializer = NetworkStreamInitializer(client)
        val streams = ConcurrentHashMap<String, StreamInfo>()
    }

    @Volatile
    private var session: Session? = null
    private val idCounter = AtomicLong(0)
    private var server: ProxyServer? = null

    @Synchronized
    private fun ensureStarted(): Int {
        val running = server ?: ProxyServer().also {
            it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            server = it
        }
        return running.listeningPort
    }

    /** Registers [files] on [connection] as one playback session and returns their URLs, in order. */
    @Synchronized
    fun registerStreams(connection: NetworkConnection, files: List<NetworkStreamRequest>): List<String> {
        val port = ensureStarted()
        releaseSession()
        val newSession = Session(clientFactory.create(connection))
        session = newSession
        return files.map { file ->
            val id = idCounter.incrementAndGet().toString()
            newSession.streams[id] = StreamInfo(path = file.path, mimeType = networkVideoMimeType(file.name))
            // The stream id is the first path segment; the (encoded) file name is appended only so the
            // player can derive a proper title from the URL's last segment instead of the id.
            "http://127.0.0.1:$port/$id/${Uri.encode(file.name)}"
        }
    }

    /** Registers a lone [filePath] for streaming and returns its local playback URL. */
    fun registerStream(connection: NetworkConnection, filePath: String, fileName: String): String =
        registerStreams(connection, listOf(NetworkStreamRequest(path = filePath, name = fileName))).single()

    /**
     * Stops the local server and releases the current session and its connection. Call when the app
     * is being destroyed. The proxy stays reusable — a later [registerStreams] starts a new server.
     */
    @Synchronized
    fun release() {
        releaseSession()
        server?.stop()
        server = null
    }

    /** Drops the registered session and closes its connection off the caller's thread. */
    private fun releaseSession() {
        val previous = session ?: return
        session = null
        CoroutineScope(Dispatchers.IO).launch { runCatching { previous.client.disconnect() } }
    }

    private inner class ProxyServer : NanoHTTPD("127.0.0.1", 0) {

        override fun serve(httpSession: IHTTPSession): Response {
            val session = this@NetworkStreamingProxy.session
            val id = httpSession.uri.trim('/').substringBefore('/')
            val info = session?.streams?.get(id)
                ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Stream not found")

            return try {
                runBlocking {
                    val fileSize = session.initializer.initialize(info.path)

                    val range = httpSession.headers["range"]
                    if (range != null && range.startsWith("bytes=") && fileSize > 0) {
                        partialResponse(session.client, info, fileSize, range)
                    } else {
                        fullResponse(session.client, info, fileSize)
                    }
                }
            } catch (e: Exception) {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
            }
        }

        private suspend fun partialResponse(
            client: NetworkClient,
            info: StreamInfo,
            fileSize: Long,
            rangeHeader: String,
        ): Response {
            val parts = rangeHeader.removePrefix("bytes=").split("-")
            val start = parts.getOrNull(0)?.toLongOrNull() ?: 0L
            val end = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }?.toLongOrNull() ?: (fileSize - 1)
            val contentLength = (end - start + 1).coerceAtLeast(0)

            val stream = client.openStream(info.path, start)
            return newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, info.mimeType, stream, contentLength).apply {
                addHeader("Accept-Ranges", "bytes")
                addHeader("Content-Range", "bytes $start-$end/$fileSize")
            }
        }

        private suspend fun fullResponse(client: NetworkClient, info: StreamInfo, fileSize: Long): Response {
            val stream = client.openStream(info.path, 0L)
            return if (fileSize >= 0) {
                newFixedLengthResponse(Response.Status.OK, info.mimeType, stream, fileSize).apply {
                    addHeader("Accept-Ranges", "bytes")
                }
            } else {
                newChunkedResponse(Response.Status.OK, info.mimeType, stream)
            }
        }
    }
}
