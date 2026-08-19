package dev.anilbeesetti.nextplayer.core.data.live

import dev.anilbeesetti.nextplayer.core.model.LiveProgramme
import java.io.InputStream
import java.util.Calendar
import java.util.TimeZone
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/**
 * Reads an XMLTV programme guide.
 *
 * A single day of listings already runs to tens of thousands of entries, so the document is
 * streamed rather than held as a tree. Only `programme` elements and their title are of interest;
 * descriptions, ratings and credits are skipped.
 */
object XmltvParser {

    fun parse(input: InputStream): List<LiveProgramme> {
        val handler = GuideHandler()
        SAXParserFactory.newInstance().newSAXParser().parse(input, handler)
        return handler.programmes
    }

    private class GuideHandler : DefaultHandler() {

        val programmes = mutableListOf<LiveProgramme>()

        private var channelName: String? = null
        private var startMillis = 0L
        private var endMillis = 0L
        private var title: StringBuilder? = null
        private var hasTitle = false

        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
            when (qName) {
                "programme" -> {
                    channelName = attributes.getValue("channel")
                    startMillis = epochMillisOf(attributes.getValue("start"))
                    endMillis = epochMillisOf(attributes.getValue("stop"))
                    hasTitle = false
                }
                // Guides may repeat the title once per language; the first one is taken.
                "title" -> if (channelName != null && !hasTitle) title = StringBuilder()
            }
        }

        override fun characters(chars: CharArray, offset: Int, length: Int) {
            title?.append(chars, offset, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            when (qName) {
                "title" -> {
                    val text = title?.toString()?.trim()
                    title = null
                    if (text.isNullOrEmpty()) return
                    hasTitle = true
                    val channel = channelName ?: return
                    if (endMillis > startMillis) {
                        programmes += LiveProgramme(
                            channelName = channel,
                            title = text,
                            start = startMillis,
                            end = endMillis,
                        )
                    }
                }

                "programme" -> {
                    channelName = null
                    title = null
                }
            }
        }
    }

    /**
     * Reads an XMLTV timestamp: fourteen digits of `yyyyMMddHHmmss` followed by an optional
     * ` +0800` style offset, which XMLTV takes to be UTC when left out.
     */
    private fun epochMillisOf(value: String?): Long {
        val digits = value?.takeWhile(Char::isDigit) ?: return 0
        if (digits.length < 14) return 0
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.clear()
        calendar.set(
            digits.substring(0, 4).toInt(),
            digits.substring(4, 6).toInt() - 1,
            digits.substring(6, 8).toInt(),
            digits.substring(8, 10).toInt(),
            digits.substring(10, 12).toInt(),
            digits.substring(12, 14).toInt(),
        )
        return calendar.timeInMillis - offsetMillisOf(value)
    }

    private fun offsetMillisOf(value: String): Long {
        val signIndex = value.indexOfLast { it == '+' || it == '-' }
        if (signIndex < 0) return 0
        val offset = value.substring(signIndex + 1).trim()
        if (offset.length < 4) return 0
        val hours = offset.substring(0, 2).toIntOrNull() ?: return 0
        val minutes = offset.substring(2, 4).toIntOrNull() ?: return 0
        val sign = if (value[signIndex] == '+') 1 else -1
        return sign * (hours * 60L + minutes) * 60_000L
    }
}
