package dev.anilbeesetti.nextplayer.core.data.network

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class GzipTest {

    @Test
    fun `a gzipped body is unpacked`() {
        val xml = "<tv><programme>新闻联播</programme></tv>"

        val read = gzipped(xml).inputStream().unpacked().reader().readText()

        assertEquals(xml, read)
    }

    @Test
    fun `a plain body is passed through`() {
        val xml = "<tv><programme>新闻联播</programme></tv>"

        val read = xml.toByteArray().inputStream().unpacked().reader().readText()

        assertEquals(xml, read)
    }

    @Test
    fun `a body too short to judge is passed through`() {
        assertEquals("<", "<".toByteArray().inputStream().unpacked().reader().readText())
        assertEquals("", ByteArray(0).inputStream().unpacked().reader().readText())
    }

    @Test
    fun `a body larger than one buffer survives unpacking`() {
        val xml = "<tv>" + "<programme>新闻联播</programme>".repeat(20_000) + "</tv>"

        val read = gzipped(xml).inputStream().unpacked().reader().readText()

        assertEquals(xml, read)
    }

    private fun gzipped(text: String): ByteArray {
        val packed = ByteArrayOutputStream()
        GZIPOutputStream(packed).use { it.write(text.toByteArray()) }
        return packed.toByteArray()
    }
}
