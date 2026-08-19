package dev.anilbeesetti.nextplayer.core.data.live

import dev.anilbeesetti.nextplayer.core.data.repository.LiveChannelRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveSourceRepository
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.model.LiveSource
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveChannelGathererTest {

    @Test
    fun `a station both sources carry is offered once with the older source first`() = runTest {
        val gather = gatherer(
            "https://a/tv.m3u" to listOf(channel("CCTV1", "http://a/cctv1")),
            "https://b/tv.m3u" to listOf(
                channel("CCTV-1综合", "http://b/cctv1"),
                channel("Hunan", "http://b/hunan"),
            ),
        )

        val result = gather()

        assertEquals(listOf("CCTV1", "Hunan"), result.channels.map { it.name })
        assertEquals(listOf("http://a/cctv1", "http://b/cctv1"), result.channels[0].urls)
        assertEquals(emptyList<LiveSource>(), result.failedSources)
        assertEquals(2, result.sourceCount)
    }

    @Test
    fun `a source that cannot be read costs only its own channels`() = runTest {
        val gather = gatherer(
            "https://a/tv.m3u" to listOf(channel("CCTV1", "http://a/cctv1")),
            "https://broken/tv.m3u" to null,
            "https://c/tv.m3u" to listOf(channel("Hunan", "http://c/hunan")),
        )

        val result = gather()

        assertEquals(listOf("CCTV1", "Hunan"), result.channels.map { it.name })
        assertEquals(listOf("https://broken/tv.m3u"), result.failedSources.map { it.url })
        assertEquals(3, result.sourceCount)
    }

    @Test
    fun `nothing is offered when every source fails`() = runTest {
        val gather = gatherer(
            "https://a/tv.m3u" to null,
            "https://b/tv.m3u" to null,
        )

        val result = gather()

        assertTrue(result.channels.isEmpty())
        assertEquals(2, result.failedSources.size)
        assertEquals(2, result.sourceCount)
    }

    @Test
    fun `no configured sources leave nothing to read`() = runTest {
        val result = gatherer()()

        assertTrue(result.channels.isEmpty())
        assertTrue(result.failedSources.isEmpty())
        assertEquals(0, result.sourceCount)
    }

    @Test
    fun `asking for a refresh asks every source for a fresh copy`() = runTest {
        val channelRepository = FakeLiveChannelRepository(
            mapOf(
                "https://a/tv.m3u" to listOf(channel("CCTV1", "http://a/cctv1")),
                "https://b/tv.m3u" to listOf(channel("Hunan", "http://b/hunan")),
            ),
        )
        val gather = LiveChannelGatherer(
            FakeLiveSourceRepository(listOf("https://a/tv.m3u", "https://b/tv.m3u")),
            channelRepository,
        )

        gather()
        gather(refresh = true)

        assertEquals(
            listOf(
                FakeLiveChannelRepository.Request("https://a/tv.m3u", refresh = false),
                FakeLiveChannelRepository.Request("https://b/tv.m3u", refresh = false),
                FakeLiveChannelRepository.Request("https://a/tv.m3u", refresh = true),
                FakeLiveChannelRepository.Request("https://b/tv.m3u", refresh = true),
            ),
            channelRepository.requests,
        )
    }

    @Test
    fun `stored channels are merged as read ones are, without reading a source`() = runTest {
        val channelRepository = FakeLiveChannelRepository(
            playlists = emptyMap(),
            stored = mapOf(
                "https://a/tv.m3u" to listOf(channel("CCTV1", "http://a/cctv1")),
                "https://b/tv.m3u" to listOf(channel("CCTV-1综合", "http://b/cctv1")),
            ),
        )
        val gather = LiveChannelGatherer(
            FakeLiveSourceRepository(listOf("https://a/tv.m3u", "https://b/tv.m3u")),
            channelRepository,
        )

        val result = gather.stored()

        assertEquals(listOf("CCTV1"), result?.channels?.map { it.name })
        assertEquals(listOf("http://a/cctv1", "http://b/cctv1"), result?.channels?.get(0)?.urls)
        assertEquals(2, result?.sourceCount)
        assertTrue(channelRepository.requests.isEmpty())
    }

    @Test
    fun `a source that has stored nothing is left out rather than counted as failed`() = runTest {
        val gather = LiveChannelGatherer(
            FakeLiveSourceRepository(listOf("https://a/tv.m3u", "https://b/tv.m3u")),
            FakeLiveChannelRepository(
                playlists = emptyMap(),
                stored = mapOf("https://b/tv.m3u" to listOf(channel("Hunan", "http://b/hunan"))),
            ),
        )

        val result = gather.stored()

        assertEquals(listOf("Hunan"), result?.channels?.map { it.name })
        assertEquals(emptyList<LiveSource>(), result?.failedSources)
        assertEquals(2, result?.sourceCount)
    }

    @Test
    fun `nothing stored anywhere leaves nothing to show early`() = runTest {
        val gather = gatherer("https://a/tv.m3u" to listOf(channel("CCTV1", "http://a/cctv1")))

        assertNull(gather.stored())
    }

    @Test
    fun `a station is found among stored channels without reading a source`() = runTest {
        val channelRepository = FakeLiveChannelRepository(
            playlists = mapOf("https://a/tv.m3u" to listOf(channel("CCTV1", "http://a/cctv1"))),
            stored = mapOf("https://a/tv.m3u" to listOf(channel("CCTV-1综合", "http://kept/cctv1"))),
        )
        val gather = LiveChannelGatherer(
            FakeLiveSourceRepository(listOf("https://a/tv.m3u")),
            channelRepository,
        )

        val found = gather.channelFor("CCTV1")

        assertEquals(listOf("http://kept/cctv1"), found?.urls)
        assertTrue(channelRepository.requests.isEmpty())
    }

    @Test
    fun `a station the stored channels lack is looked for in the sources`() = runTest {
        val gather = LiveChannelGatherer(
            FakeLiveSourceRepository(listOf("https://a/tv.m3u")),
            FakeLiveChannelRepository(
                playlists = mapOf(
                    "https://a/tv.m3u" to listOf(
                        channel("CCTV1", "http://a/cctv1"),
                        channel("CCTV5", "http://a/cctv5"),
                    ),
                ),
                stored = mapOf("https://a/tv.m3u" to listOf(channel("CCTV1", "http://kept/cctv1"))),
            ),
        )

        val found = gather.channelFor("CCTV5")

        assertEquals(listOf("http://a/cctv5"), found?.urls)
    }

    @Test
    fun `a station no source carries at all is not found`() = runTest {
        val gather = gatherer("https://a/tv.m3u" to listOf(channel("CCTV1", "http://a/cctv1")))

        assertNull(gather.channelFor("CCTV5"))
    }

    private fun gatherer(vararg playlists: Pair<String, List<LiveChannel>?>) =
        LiveChannelGatherer(
            FakeLiveSourceRepository(playlists.map { it.first }),
            FakeLiveChannelRepository(playlists.toMap()),
        )

    private fun channel(name: String, vararg urls: String) =
        LiveChannel(name = name, urls = urls.toList())
}

private class FakeLiveSourceRepository(urls: List<String>) : LiveSourceRepository {
    private val sources = MutableStateFlow(
        urls.mapIndexed { index, url ->
            LiveSource(id = index + 1L, name = "Source ${index + 1}", url = url)
        },
    )

    override fun getSources(): Flow<List<LiveSource>> = sources
    override suspend fun getSource(id: Long): LiveSource? = error("Not used")
    override suspend fun upsert(source: LiveSource): Long = error("Not used")
    override suspend fun delete(id: Long) = error("Not used")
}

private class FakeLiveChannelRepository(
    private val playlists: Map<String, List<LiveChannel>?>,
    private val stored: Map<String, List<LiveChannel>> = emptyMap(),
) : LiveChannelRepository {

    data class Request(val url: String, val refresh: Boolean)

    val requests = mutableListOf<Request>()

    override suspend fun getChannels(url: String, refresh: Boolean): Result<List<LiveChannel>> {
        requests += Request(url, refresh)
        return playlists[url]?.let { Result.success(it) }
            ?: Result.failure(IOException("Could not read $url"))
    }

    override suspend fun getStoredChannels(url: String): List<LiveChannel>? = stored[url]
}
