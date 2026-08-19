package dev.anilbeesetti.nextplayer.core.media.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveMediaKeyTest {

    @Test
    fun `a channel is written under a scheme of its own`() {
        assertEquals("cafeplayer-live://CCTV1", LiveMediaKey("CCTV1").toString())
    }

    @Test
    fun `a written key reads back as the channel it names`() {
        val key = LiveMediaKey("东方卫视")

        assertEquals(key, LiveMediaKey.of(key.toString()))
    }

    @Test
    fun `a key of another kind is not a channel`() {
        assertNull(LiveMediaKey.of("cafeplayer-network://1/movies/a.mkv"))
        assertNull(LiveMediaKey.of("content://media/external/video/media/42"))
        assertNull(LiveMediaKey.of("http://example.com/cctv1.m3u8"))
    }

    @Test
    fun `a key naming no channel at all is not a channel`() {
        assertNull(LiveMediaKey.of("cafeplayer-live://"))
        assertNull(LiveMediaKey.of(""))
    }
}
