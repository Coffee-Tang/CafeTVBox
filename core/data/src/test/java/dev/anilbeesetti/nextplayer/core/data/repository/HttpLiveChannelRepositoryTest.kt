package dev.anilbeesetti.nextplayer.core.data.repository

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpLiveChannelRepositoryTest {

    private lateinit var server: HttpServer
    private val repository = HttpLiveChannelRepository()

    private val playlist = """
        #EXTM3U
        #EXTINF:-1 tvg-logo="http://logo/1.png" group-title="央视",CCTV1
        http://stream/cctv1.m3u8
        #EXTINF:-1 group-title="卫视",湖南卫视
        http://stream/hunan.m3u8
    """.trimIndent()

    private val baseUrl: String get() = "http://127.0.0.1:${server.address.port}"

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun `fetches and parses playlist channels`() = runTest {
        server.respond("/tv.m3u") { exchange -> exchange.sendText(200, playlist) }

        val channels = repository.getChannels("$baseUrl/tv.m3u").getOrThrow()

        assertEquals(2, channels.size)
        assertEquals("CCTV1", channels[0].name)
        assertEquals("央视", channels[0].group)
        assertEquals("http://stream/cctv1.m3u8", channels[0].url)
        assertEquals("湖南卫视", channels[1].name)
    }

    @Test
    fun `follows redirects`() = runTest {
        server.respond("/redirect") { exchange ->
            exchange.responseHeaders.add("Location", "$baseUrl/tv.m3u")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.respond("/tv.m3u") { exchange -> exchange.sendText(200, playlist) }

        val channels = repository.getChannels("$baseUrl/redirect").getOrThrow()

        assertEquals(2, channels.size)
    }

    @Test
    fun `returns failure on http error`() = runTest {
        server.respond("/missing") { exchange -> exchange.sendText(404, "not found") }

        val result = repository.getChannels("$baseUrl/missing")

        assertTrue(result.isFailure)
    }

    @Test
    fun `returns failure when playlist has no channels`() = runTest {
        server.respond("/empty") { exchange -> exchange.sendText(200, "#EXTM3U") }

        val result = repository.getChannels("$baseUrl/empty")

        assertTrue(result.isFailure)
    }

    @Test
    fun `returns failure for unreachable host`() = runTest {
        val result = repository.getChannels("http://127.0.0.1:1/tv.m3u")

        assertTrue(result.isFailure)
    }

    private fun HttpServer.respond(path: String, handler: (HttpExchange) -> Unit) {
        createContext(path) { exchange -> handler(exchange) }
    }

    private fun HttpExchange.sendText(status: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
}
