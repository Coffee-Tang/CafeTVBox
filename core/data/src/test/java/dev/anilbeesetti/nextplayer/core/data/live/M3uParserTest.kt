package dev.anilbeesetti.nextplayer.core.data.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun `parses channel with all attributes`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="cctv1" tvg-name="CCTV1" tvg-logo="http://logo/cctv1.png" group-title="央视",CCTV1 综合
            http://stream/cctv1.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals(1, channels.size)
        val channel = channels.first()
        assertEquals("CCTV1 综合", channel.name)
        assertEquals("http://stream/cctv1.m3u8", channel.url)
        assertEquals("央视", channel.group)
        assertEquals("http://logo/cctv1.png", channel.logoUrl)
        assertEquals("cctv1", channel.tvgId)
    }

    @Test
    fun `keeps bracketed ipv6 url with trailing query marker intact`() {
        val content = """
            #EXTM3U x-tvg-url="https://live.fanmingming.cn/e.xml" catchup="append"
            #EXTINF:-1 tvg-name="CCTV1" tvg-logo="https://live.fanmingming.cn/tv/CCTV1.png" group-title="央视频道",CCTV-1综合
            http://[2409:8087:8:21::18]:6610/otttv.bj.chinamobile.com/PLTV/88888888/224/3221226895/1.m3u8?
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals(1, channels.size)
        val channel = channels.first()
        assertEquals("CCTV-1综合", channel.name)
        assertEquals(
            "http://[2409:8087:8:21::18]:6610/otttv.bj.chinamobile.com/PLTV/88888888/224/3221226895/1.m3u8?",
            channel.url,
        )
        assertEquals("央视频道", channel.group)
    }

    @Test
    fun `parses multiple channels`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 group-title="A",Channel 1
            http://stream/1
            #EXTINF:-1 group-title="B",Channel 2
            http://stream/2
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals(2, channels.size)
        assertEquals("Channel 1", channels[0].name)
        assertEquals("A", channels[0].group)
        assertEquals("Channel 2", channels[1].name)
        assertEquals("B", channels[1].group)
    }

    @Test
    fun `title after comma is kept when attribute value contains a comma`() {
        val content = """
            #EXTINF:-1 group-title="News, Sports",Big, Channel
            http://stream/x
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals(1, channels.size)
        assertEquals("Big, Channel", channels[0].name)
        assertEquals("News, Sports", channels[0].group)
    }

    @Test
    fun `EXTGRP is used as fallback group`() {
        val content = """
            #EXTINF:-1,Channel Without Group
            #EXTGRP:Movies
            http://stream/x
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals("Movies", channels[0].group)
    }

    @Test
    fun `channel without group has empty group`() {
        val content = """
            #EXTINF:-1,Solo
            http://stream/x
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals("", channels[0].group)
        assertNull(channels[0].logoUrl)
    }

    @Test
    fun `falls back to tvg-name when title is empty`() {
        val content = """
            #EXTINF:-1 tvg-name="Fallback",
            http://stream/x
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals("Fallback", channels[0].name)
    }

    @Test
    fun `ignores blank lines comments and leading BOM`() {
        val content = "\uFEFF#EXTM3U\n\n#EXTVLCOPT:network-caching=1000\n#EXTINF:-1,Chan\nhttp://stream/x\n"

        val channels = M3uParser.parse(content)

        assertEquals(1, channels.size)
        assertEquals("Chan", channels[0].name)
        assertEquals("http://stream/x", channels[0].url)
    }

    @Test
    fun `empty content yields no channels`() {
        assertTrue(M3uParser.parse("").isEmpty())
    }

    @Test
    fun `url without extinf falls back to file name`() {
        val content = """
            http://stream/my_channel
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals(1, channels.size)
        assertEquals("my_channel", channels[0].name)
    }

    @Test
    fun `pipe separated urls become alternative lines of one channel`() {
        val content = """
            #EXTINF:-1,CCTV1
            http://one/x.m3u8|http://two/x.m3u8|http://three/x.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals(1, channels.size)
        assertEquals(
            listOf("http://one/x.m3u8", "http://two/x.m3u8", "http://three/x.m3u8"),
            channels[0].urls,
        )
        assertEquals("http://one/x.m3u8", channels[0].url)
    }

    @Test
    fun `pipe carrying request headers is kept as part of the url`() {
        val content = """
            #EXTINF:-1,CCTV1
            http://host/x.m3u8|User-Agent=Mozilla&Referer=http://host/
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals(
            listOf("http://host/x.m3u8|User-Agent=Mozilla&Referer=http://host/"),
            channels[0].urls,
        )
    }

    @Test
    fun `repeated channels are folded into one keeping playlist order`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 group-title="央视",CCTV1
            http://a/cctv1
            #EXTINF:-1 group-title="卫视",Hunan
            http://a/hunan
            #EXTINF:-1 group-title="高清",CCTV1
            http://b/cctv1
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals(2, channels.size)
        assertEquals("CCTV1", channels[0].name)
        assertEquals(listOf("http://a/cctv1", "http://b/cctv1"), channels[0].urls)
        assertEquals("央视", channels[0].group)
        assertEquals("Hunan", channels[1].name)
    }

    @Test
    fun `folding fills in metadata the first entry left out and drops duplicate urls`() {
        val content = """
            #EXTINF:-1,CCTV1
            http://a/cctv1
            #EXTINF:-1 tvg-id="cctv1" tvg-logo="http://logo/cctv1.png",CCTV1
            http://a/cctv1
            #EXTINF:-1,cctv1
            http://c/cctv1
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals(1, channels.size)
        assertEquals(listOf("http://a/cctv1", "http://c/cctv1"), channels[0].urls)
        assertEquals("http://logo/cctv1.png", channels[0].logoUrl)
        assertEquals("cctv1", channels[0].tvgId)
        assertEquals("CCTV1", channels[0].name)
    }
}
