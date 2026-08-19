package dev.anilbeesetti.nextplayer.core.data.repository

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HttpLiveChannelRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var server: HttpServer

    /** Left to be created by the repository, as it is on a device the first time a source is read. */
    private val store: File get() = File(temporaryFolder.root, "playlists")

    private val playlist = """
        #EXTM3U
        #EXTINF:-1 tvg-logo="http://logo/1.png" group-title="央视",CCTV1
        http://stream/cctv1.m3u8
        #EXTINF:-1 group-title="卫视",湖南卫视
        http://stream/hunan.m3u8
    """.trimIndent()

    private val baseUrl: String get() = "http://127.0.0.1:${server.address.port}"

    /** A repository as a later run finds things: the same store, nothing kept in memory. */
    private fun repository() = HttpLiveChannelRepository(store)

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

        val channels = repository().getChannels("$baseUrl/tv.m3u").getOrThrow()

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

        val channels = repository().getChannels("$baseUrl/redirect").getOrThrow()

        assertEquals(2, channels.size)
    }

    @Test
    fun `returns failure on http error`() = runTest {
        server.respond("/missing") { exchange -> exchange.sendText(404, "not found") }

        val result = repository().getChannels("$baseUrl/missing")

        assertTrue(result.isFailure)
    }

    @Test
    fun `returns failure when playlist has no channels`() = runTest {
        server.respond("/empty") { exchange -> exchange.sendText(200, "#EXTM3U") }

        val result = repository().getChannels("$baseUrl/empty")

        assertTrue(result.isFailure)
    }

    @Test
    fun `returns failure for unreachable host`() = runTest {
        val result = repository().getChannels("http://127.0.0.1:1/tv.m3u")

        assertTrue(result.isFailure)
    }

    @Test
    fun `serves a playlist read in an earlier run without reaching its source`() = runTest {
        server.respond("/tv.m3u") { exchange -> exchange.sendText(200, playlist) }
        val url = "$baseUrl/tv.m3u"
        repository().getChannels(url).getOrThrow()
        server.stop(0)

        val stored = repository().getStoredChannels(url)

        assertEquals(listOf("CCTV1", "湖南卫视"), stored?.map { it.name })
    }

    @Test
    fun `has nothing stored for a source it has never read`() = runTest {
        assertNull(repository().getStoredChannels("$baseUrl/tv.m3u"))
    }

    @Test
    fun `stores nothing when a playlist could not be read`() = runTest {
        server.respond("/missing") { exchange -> exchange.sendText(404, "not found") }
        val url = "$baseUrl/missing"

        repository().getChannels(url)

        assertNull(repository().getStoredChannels(url))
    }

    @Test
    fun `stores nothing when a playlist carries no channels`() = runTest {
        server.respond("/empty") { exchange -> exchange.sendText(200, "#EXTM3U") }
        val url = "$baseUrl/empty"

        repository().getChannels(url)

        assertNull(repository().getStoredChannels(url))
    }

    @Test
    fun `reads the source even when it has a stored playlist`() = runTest {
        var served = 0
        server.respond("/tv.m3u") { exchange ->
            served++
            exchange.sendText(200, playlist)
        }
        val url = "$baseUrl/tv.m3u"
        repository().getChannels(url).getOrThrow()

        repository().getChannels(url).getOrThrow()

        assertEquals(2, served)
    }

    @Test
    fun `replaces the stored playlist with what the source now carries`() = runTest {
        val url = "$baseUrl/tv.m3u"
        var body = playlist
        server.respond("/tv.m3u") { exchange -> exchange.sendText(200, body) }
        repository().getChannels(url).getOrThrow()

        body = """
            #EXTM3U
            #EXTINF:-1,CCTV2
            http://stream/cctv2.m3u8
        """.trimIndent()
        repository().getChannels(url, refresh = true).getOrThrow()

        assertEquals(listOf("CCTV2"), repository().getStoredChannels(url)?.map { it.name })
    }

    @Test
    fun `keeps a playlist of one source apart from another`() = runTest {
        server.respond("/tv.m3u") { exchange -> exchange.sendText(200, playlist) }
        server.respond("/other.m3u") { exchange ->
            exchange.sendText(200, "#EXTM3U\n#EXTINF:-1,CCTV2\nhttp://stream/cctv2.m3u8")
        }
        val repository = repository()
        repository.getChannels("$baseUrl/tv.m3u").getOrThrow()
        repository.getChannels("$baseUrl/other.m3u").getOrThrow()

        val later = repository()

        assertEquals(2, later.getStoredChannels("$baseUrl/tv.m3u")?.size)
        assertEquals(listOf("CCTV2"), later.getStoredChannels("$baseUrl/other.m3u")?.map { it.name })
    }

    @Test
    fun `reading one address at once from several places leaves one whole playlist`() = runTest {
        server.respond("/tv.m3u") { exchange -> exchange.sendText(200, playlist) }
        val url = "$baseUrl/tv.m3u"

        List(4) { async { repository().getChannels(url) } }
            .awaitAll()
            .forEach { it.getOrThrow() }

        assertEquals(listOf("CCTV1", "湖南卫视"), repository().getStoredChannels(url)?.map { it.name })
        assertEquals(listOf("m3u"), store.listFiles()?.map { it.extension })
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
