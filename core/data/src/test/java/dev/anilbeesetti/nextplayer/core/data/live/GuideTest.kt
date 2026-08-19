package dev.anilbeesetti.nextplayer.core.data.live

import dev.anilbeesetti.nextplayer.core.model.LiveProgramme
import dev.anilbeesetti.nextplayer.core.model.channelKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideTest {

    @Test
    fun `arranges programmes by channel keeping their order`() {
        val guide = listOf(
            programme("CCTV1", "晚间新闻", 0),
            programme("东方卫视", "新闻夜线", 1),
            programme("CCTV1", "新闻联播", 2),
        ).toGuide()

        assertEquals(setOf("CCTV1", "东方卫视"), guide.keys)
        assertEquals(listOf("晚间新闻", "新闻联播"), guide.getValue("CCTV1").map { it.title })
    }

    @Test
    fun `leaves out the filler a guide pads unknown channels with`() {
        val guide = listOf(
            programme("厦门移动", "精彩节目", 0),
            programme("银川公共", "暂未提供节目预告信息", 1),
            programme("银川公共", "本地新闻", 2),
        ).toGuide()

        assertTrue("厦门移动" !in guide)
        assertEquals(listOf("本地新闻"), guide.getValue("银川公共").map { it.title })
    }

    @Test
    fun `files programmes under the key so a playlist naming a station its own way finds them`() {
        val guide = listOf(
            programme("CCTV-1综合", "新闻联播", 0),
            programme("东方卫视高清", "新闻夜线", 0),
        ).toGuide()

        assertEquals(listOf("新闻联播"), guide.getValue(channelKey("CCTV1 (1080p)")).map { it.title })
        assertEquals(listOf("新闻夜线"), guide.getValue(channelKey("东方卫视")).map { it.title })
    }

    @Test
    fun `programmes a guide splits across spellings of one station are gathered`() {
        val guide = listOf(
            programme("CCTV1", "晚间新闻", 0),
            programme("CCTV-1", "新闻联播", 1),
        ).toGuide()

        assertEquals(1, guide.size)
        assertEquals(listOf("晚间新闻", "新闻联播"), guide.getValue("CCTV1").map { it.title })
    }

    @Test
    fun `an empty guide yields no channels`() {
        assertTrue(emptyList<LiveProgramme>().toGuide().isEmpty())
    }

    private fun programme(channel: String, title: String, slot: Long) = LiveProgramme(
        channelName = channel,
        title = title,
        start = slot * 3_600_000,
        end = (slot + 1) * 3_600_000,
    )
}
