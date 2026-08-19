package dev.anilbeesetti.nextplayer.core.data.live

import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveChannelMergeTest {

    @Test
    fun `sources naming one station differently offer it as a single channel`() {
        val merged = mergeChannels(
            listOf(
                listOf(channel("CCTV-1 (1080p)", "http://a/cctv1")),
                listOf(channel("CCTV1综合", "http://b/cctv1")),
            ),
        )

        assertEquals(1, merged.size)
        assertEquals("CCTV-1 (1080p)", merged[0].name)
        assertEquals(listOf("http://a/cctv1", "http://b/cctv1"), merged[0].urls)
    }

    @Test
    fun `stations that only look alike stay apart`() {
        val merged = mergeChannels(
            listOf(
                listOf(channel("CCTV1", "http://a/cctv1"), channel("CCTV5", "http://a/cctv5")),
                listOf(channel("CCTV11", "http://b/cctv11"), channel("CCTV5+", "http://b/cctv5plus")),
            ),
        )

        assertEquals(listOf("CCTV1", "CCTV5", "CCTV11", "CCTV5+"), merged.map { it.name })
    }

    @Test
    fun `the first source to carry a station decides how it reads and where it sits`() {
        val merged = mergeChannels(
            listOf(
                listOf(
                    channel("Hunan", "http://a/hunan", group = "卫视"),
                    channel("CCTV1", "http://a/cctv1", group = "央视", logo = "http://a/logo.png", tvgId = "a"),
                ),
                listOf(
                    channel("CCTV-1", "http://b/cctv1", group = "备用", logo = "http://b/logo.png", tvgId = "b"),
                ),
            ),
        )

        assertEquals(listOf("Hunan", "CCTV1"), merged.map { it.name })
        assertEquals("央视", merged[1].group)
        assertEquals("http://a/logo.png", merged[1].logoUrl)
        assertEquals("a", merged[1].tvgId)
    }

    @Test
    fun `a later source fills in artwork the first one left out`() {
        val merged = mergeChannels(
            listOf(
                listOf(channel("CCTV1", "http://a/cctv1")),
                listOf(channel("CCTV1", "http://b/cctv1", logo = "http://b/logo.png", tvgId = "cctv1")),
            ),
        )

        assertEquals("http://b/logo.png", merged[0].logoUrl)
        assertEquals("cctv1", merged[0].tvgId)
    }

    @Test
    fun `artwork the first source carries is not replaced by a later one`() {
        val merged = mergeChannels(
            listOf(
                listOf(channel("CCTV1", "http://a/cctv1", logo = "http://a/logo.png")),
                listOf(channel("CCTV1", "http://b/cctv1", logo = "http://b/logo.png", tvgId = "cctv1")),
            ),
        )

        assertEquals("http://a/logo.png", merged[0].logoUrl)
        assertEquals("cctv1", merged[0].tvgId)
    }

    @Test
    fun `lines follow source order with each source keeping its own`() {
        val merged = mergeChannels(
            listOf(
                listOf(channel("CCTV1", "http://a/one", "http://a/two")),
                listOf(channel("CCTV1", "http://b/one", "http://b/two")),
            ),
        )

        assertEquals(
            listOf("http://a/one", "http://a/two", "http://b/one", "http://b/two"),
            merged[0].urls,
        )
    }

    @Test
    fun `a source added twice contributes its lines once`() {
        val playlist = listOf(channel("CCTV1", "http://a/one", "http://a/two"))

        val merged = mergeChannels(listOf(playlist, playlist))

        assertEquals(listOf("http://a/one", "http://a/two"), merged[0].urls)
    }

    @Test
    fun `lines that only differ by port or query stay apart`() {
        val merged = mergeChannels(
            listOf(
                listOf(channel("CCTV1", "http://[2409:8087:8::18]:6610/1.m3u8?")),
                listOf(
                    channel(
                        "CCTV1",
                        "http://[2409:8087:8::18]:6610/1.m3u8",
                        "http://[2409:8087:8::18]:8080/1.m3u8?",
                    ),
                ),
            ),
        )

        assertEquals(3, merged[0].urls.size)
    }

    @Test
    fun `repeated lines are dropped before the cap so they cannot spend it`() {
        val merged = mergeChannels(
            listOf(
                listOf(channel("CCTV1", "http://a/one", "http://a/two")),
                listOf(channel("CCTV1", "http://a/one", "http://a/two", "http://b/three")),
            ),
            maxLines = 3,
        )

        assertEquals(listOf("http://a/one", "http://a/two", "http://b/three"), merged[0].urls)
    }

    @Test
    fun `a station carried by many sources keeps only its first few lines`() {
        val playlists = (1..8).map { listOf(channel("CCTV1", "http://source$it/cctv1")) }

        val merged = mergeChannels(playlists)

        assertEquals(MAX_LINES_PER_CHANNEL, merged[0].urls.size)
        assertEquals("http://source1/cctv1", merged[0].url)
        assertEquals("http://source5/cctv1", merged[0].urls.last())
    }

    @Test
    fun `a single source is left as it was`() {
        val playlist = listOf(
            channel("CCTV1", "http://a/cctv1", group = "央视", logo = "http://a/logo.png"),
            channel("Hunan", "http://a/hunan"),
        )

        assertEquals(playlist, mergeChannels(listOf(playlist)))
    }

    @Test
    fun `no sources yield no channels`() {
        assertTrue(mergeChannels(emptyList()).isEmpty())
    }

    @Test
    fun `a source holding no channels adds nothing`() {
        val merged = mergeChannels(
            listOf(emptyList(), listOf(channel("CCTV1", "http://b/cctv1")), emptyList()),
        )

        assertEquals(listOf("CCTV1"), merged.map { it.name })
    }

    private fun channel(
        name: String,
        vararg urls: String,
        group: String = "",
        logo: String? = null,
        tvgId: String? = null,
    ) = LiveChannel(
        name = name,
        urls = urls.toList(),
        group = group,
        logoUrl = logo,
        tvgId = tvgId,
    )
}
