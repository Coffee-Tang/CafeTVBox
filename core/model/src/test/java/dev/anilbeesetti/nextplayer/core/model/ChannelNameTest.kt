package dev.anilbeesetti.nextplayer.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChannelNameTest {

    @Test
    fun `the ways playlists and guides write a cctv channel share one key`() {
        val written = listOf(
            "CCTV1",
            "CCTV-1",
            "CCTV-1综合",
            "CCTV-1 (1080p)",
            "CCTV-1 (720p)",
            "CCTV1高清",
            "cctv 1",
        )

        assertEquals(setOf("CCTV1"), written.map(::channelKey).toSet())
    }

    @Test
    fun `a channel numbered with a plus stays apart from the plain one`() {
        assertEquals("CCTV5+", channelKey("CCTV-5+体育赛事"))
        assertEquals("CCTV5", channelKey("CCTV5"))
        assertNotEquals(channelKey("CCTV5"), channelKey("CCTV5+"))
    }

    @Test
    fun `the cctv plus family is not folded into a numbered channel`() {
        assertNotEquals(channelKey("CCTV+ 1"), channelKey("CCTV+ 2"))
        assertNotEquals(channelKey("CCTV+ 1"), channelKey("CCTV1"))
    }

    @Test
    fun `notes about the stream are dropped`() {
        assertEquals(channelKey("宁夏卫视"), channelKey("宁夏卫视 (576p)"))
        assertEquals(channelKey("江苏综艺"), channelKey("江苏综艺 (576p) [Not 24/7]"))
        assertEquals(channelKey("东方卫视"), channelKey("东方卫视【高清】"))
    }

    @Test
    fun `quality wording is dropped from the end`() {
        assertEquals(channelKey("东方卫视"), channelKey("东方卫视4K"))
        assertEquals(channelKey("湖南卫视"), channelKey("湖南卫视高清"))
        assertEquals(channelKey("湖南卫视"), channelKey("湖南卫视 HD"))
    }

    @Test
    fun `a name made only of quality wording is left alone`() {
        assertEquals("4K", channelKey("4K"))
        assertEquals("HD", channelKey("HD"))
    }

    @Test
    fun `the cctv channels named after a resolution are stations of their own`() {
        val fourK = channelKey("CCTV-4K (1080p)")
        val eightK = channelKey("CCTV-8K (1080p)")

        assertNotEquals(fourK, eightK)
        assertNotEquals(channelKey("CCTV4"), fourK)
        assertNotEquals(channelKey("CCTV8"), eightK)
    }

    @Test
    fun `stations that merely look alike keep separate keys`() {
        assertNotEquals(channelKey("CCTV1"), channelKey("CCTV11"))
        assertNotEquals(channelKey("东方卫视"), channelKey("东南卫视"))
        assertNotEquals(channelKey("北京卫视"), channelKey("北京影视"))
    }

    @Test
    fun `an empty name yields an empty key`() {
        assertEquals("", channelKey(""))
        assertEquals("", channelKey("  "))
    }
}
