package dev.anilbeesetti.nextplayer.core.model.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinyinTest {

    @Test
    fun `a chinese title is spelled out and reduced to its initials`() {
        val forms = pinyinOf("硅谷")

        assertEquals(listOf("gui", "gu"), forms?.syllables)
        assertEquals("gg", forms?.initials)
    }

    @Test
    fun `the readings are kept apart, so that two of them do not read as a third thing`() {
        assertEquals(listOf("zhong", "guo"), pinyinOf("中国")?.syllables)
    }

    @Test
    fun `a title with no chinese in it has no pinyin to offer`() {
        assertNull(pinyinOf("Silicon Valley"))
    }

    @Test
    fun `what is not chinese is carried through as a piece of its own`() {
        val forms = pinyinOf("硅谷2016")

        assertEquals(listOf("gui", "gu", "2016"), forms?.syllables)
        assertEquals("gg2016", forms?.initials)
    }

    @Test
    fun `a name in both scripts keeps the latin half in both forms`() {
        val forms = pinyinOf("CCTV-5 体育")

        assertEquals(listOf("cctv-5 ", "ti", "yu"), forms?.syllables)
        assertEquals("cctv-5 ty", forms?.initials)
    }

    @Test
    fun `spelling the same name twice gives the same answer`() {
        assertEquals(pinyinOf("天道"), pinyinOf("天道"))
    }

    @Test
    fun `a station's name spells out the way it would be typed`() {
        assertEquals("jsws", pinyinOf("江苏卫视")?.initials)
        assertEquals(listOf("jiang", "su", "wei", "shi"), pinyinOf("江苏卫视")?.syllables)
    }
}
