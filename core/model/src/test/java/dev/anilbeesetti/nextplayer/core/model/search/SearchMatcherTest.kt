package dev.anilbeesetti.nextplayer.core.model.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMatcherTest {

    @Test
    fun `a name that has nothing to do with the query scores nothing`() {
        assertEquals(0, SearchMatcher("stranger").score("Silicon Valley"))
    }

    @Test
    fun `an empty query answers nothing rather than everything`() {
        assertEquals(0, SearchMatcher("   ").score("Silicon Valley"))
    }

    @Test
    fun `the whole query written out beats its words scattered about`() {
        val matcher = SearchMatcher("stranger 2019")

        assertTrue(matcher.score("stranger 2019") > matcher.score("2019 stranger things"))
    }

    @Test
    fun `words in order beat the same words in any order`() {
        val matcher = SearchMatcher("stranger 2019")

        assertTrue(matcher.score("stranger things 2019") > matcher.score("2019 stranger things"))
    }

    @Test
    fun `a match at the start beats one buried in the middle`() {
        val matcher = SearchMatcher("valley")

        assertTrue(matcher.score("Valley of the Kings") > matcher.score("Death.in.the.Valley"))
    }

    @Test
    fun `the best of the names given is the one that counts`() {
        val matcher = SearchMatcher("valley")

        assertEquals(
            matcher.score("Silicon Valley"),
            matcher.score("硅谷", "Silicon Valley"),
        )
    }

    @Test
    fun `a name given as null is simply not a way of naming it`() {
        assertEquals(0, SearchMatcher("valley").score(null))
    }

    @Test
    fun `a chinese title is reached by typing its pinyin`() {
        assertTrue(SearchMatcher("guigu").score("硅谷") > 0)
    }

    @Test
    fun `a chinese title is reached by typing only the initials`() {
        assertTrue(SearchMatcher("gg").score("硅谷") > 0)
    }

    @Test
    fun `typing the title itself outranks anything reached by pinyin`() {
        val matcher = SearchMatcher("tiandao")

        assertTrue(SearchMatcher("天道").score("天道") > matcher.score("天道"))
    }

    @Test
    fun `a station is found by the initials of its name`() {
        assertTrue(SearchMatcher("jsws").score("江苏卫视") > 0)
    }

    @Test
    fun `initials of another name do not answer for this one`() {
        assertEquals(0, SearchMatcher("jsws").score("东方卫视"))
    }

    @Test
    fun `two readings running together are not read as a third thing`() {
        assertEquals(0, SearchMatcher("gg").score("中国气象"))
    }

    @Test
    fun `initials have to be typed from the beginning of the name`() {
        assertEquals(0, SearchMatcher("gg").score("江苏公共"))
    }

    @Test
    fun `a name can be reached by the beginning of its first reading`() {
        assertTrue(SearchMatcher("gui").score("硅谷") > 0)
    }

    @Test
    fun `a name spelled out in full outranks one that merely begins that way`() {
        val matcher = SearchMatcher("gg")

        assertTrue(matcher.score("硅谷") > matcher.score("硅谷之外"))
    }

    @Test
    fun `nothing reached by pinyin outranks the weakest match on the name itself`() {
        val onName = SearchMatcher("stranger 2019").score("2019 stranger things")
        val onPinyin = SearchMatcher("guigu").score("硅谷")

        assertTrue(onName > onPinyin)
    }
}
