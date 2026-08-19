package dev.anilbeesetti.nextplayer.core.data.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XmltvParserTest {

    @Test
    fun `reads a programme with an offset timestamp`() {
        val guide = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="CCTV1"><display-name>CCTV1</display-name></channel>
              <programme channel="CCTV1" start="20260819004800 +0800" stop="20260819013200 +0800">
                <title lang="zh">晚间新闻</title>
                <desc lang="zh">忽略我</desc>
              </programme>
            </tv>
        """.trimIndent()

        val programmes = XmltvParser.parse(guide.byteInputStream())

        assertEquals(1, programmes.size)
        val programme = programmes.first()
        assertEquals("CCTV1", programme.channelName)
        assertEquals("晚间新闻", programme.title)
        assertEquals(1787071680000L, programme.start)
        assertEquals(1787074320000L, programme.end)
    }

    @Test
    fun `a timestamp without an offset is read as utc`() {
        val programmes = XmltvParser.parse(
            guideOf("""<programme channel="A" start="20260819004800" stop="20260819013200"><title>X</title></programme>"""),
        )

        assertEquals(1787100480000L, programmes.first().start)
    }

    @Test
    fun `reads a negative offset`() {
        val programmes = XmltvParser.parse(
            guideOf("""<programme channel="A" start="20260819004800 -0500" stop="20260819013200 -0500"><title>X</title></programme>"""),
        )

        assertEquals(1787118480000L, programmes.first().start)
    }

    @Test
    fun `keeps the first title when a programme is listed in several languages`() {
        val programmes = XmltvParser.parse(
            guideOf(
                """
                <programme channel="A" start="20260819004800 +0800" stop="20260819013200 +0800">
                  <title lang="zh">新闻联播</title>
                  <title lang="en">News</title>
                </programme>
                """.trimIndent(),
            ),
        )

        assertEquals(1, programmes.size)
        assertEquals("新闻联播", programmes.first().title)
    }

    @Test
    fun `skips a programme that has no end or no title`() {
        val programmes = XmltvParser.parse(
            guideOf(
                """
                <programme channel="A" start="20260819004800 +0800"><title>No end</title></programme>
                <programme channel="A" start="20260819013200 +0800" stop="20260819020000 +0800"></programme>
                <programme channel="A" start="20260819020000 +0800" stop="20260819022400 +0800"><title>Kept</title></programme>
                """.trimIndent(),
            ),
        )

        assertEquals(1, programmes.size)
        assertEquals("Kept", programmes.first().title)
    }

    @Test
    fun `keeps the order the guide lists programmes in`() {
        val programmes = XmltvParser.parse(
            guideOf(
                """
                <programme channel="A" start="20260819004800 +0800" stop="20260819013200 +0800"><title>First</title></programme>
                <programme channel="B" start="20260819013200 +0800" stop="20260819020000 +0800"><title>Second</title></programme>
                <programme channel="A" start="20260819020000 +0800" stop="20260819022400 +0800"><title>Third</title></programme>
                """.trimIndent(),
            ),
        )

        assertEquals(listOf("First", "Second", "Third"), programmes.map { it.title })
        assertEquals(listOf("A", "B", "A"), programmes.map { it.channelName })
    }

    @Test
    fun `a programme is on air between its start and end`() {
        val programme = XmltvParser.parse(
            guideOf("""<programme channel="A" start="20260819004800 +0800" stop="20260819013200 +0800"><title>X</title></programme>"""),
        ).first()

        assertTrue(programme.isOnAt(programme.start))
        assertTrue(programme.isOnAt(programme.end - 1))
        assertTrue(!programme.isOnAt(programme.end))
        assertTrue(!programme.isOnAt(programme.start - 1))
    }

    @Test
    fun `a guide without programmes yields nothing`() {
        assertTrue(XmltvParser.parse(guideOf("")).isEmpty())
    }

    private fun guideOf(body: String) =
        """<?xml version="1.0" encoding="UTF-8"?><tv>$body</tv>""".byteInputStream()
}
