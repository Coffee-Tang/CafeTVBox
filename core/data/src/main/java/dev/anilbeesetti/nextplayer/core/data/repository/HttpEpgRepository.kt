package dev.anilbeesetti.nextplayer.core.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.data.live.XmltvParser
import dev.anilbeesetti.nextplayer.core.data.live.toGuide
import dev.anilbeesetti.nextplayer.core.data.network.httpFetch
import dev.anilbeesetti.nextplayer.core.data.network.unpacked
import dev.anilbeesetti.nextplayer.core.model.LiveProgramme
import java.io.File
import java.io.IOException
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Serves the programme guide from an XMLTV feed, keeping today's copy on disk.
 *
 * A guide covers a single day, so it is downloaded once a day and parsed once per run. Switching
 * channels must not pay for either.
 */
@Singleton
class HttpEpgRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : EpgRepository {

    private val mutex = Mutex()
    private var cachedDay: String? = null
    private var cachedGuide: Map<String, List<LiveProgramme>> = emptyMap()

    override suspend fun getGuide(): Result<Map<String, List<LiveProgramme>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                mutex.withLock {
                    val day = today()
                    if (day != cachedDay) {
                        cachedGuide = readGuide(day)
                        cachedDay = day
                    }
                    cachedGuide
                }
            }.onFailure { Logger.logError(TAG, "Could not read the guide at $GUIDE_URL: $it") }
        }

    private fun readGuide(day: String): Map<String, List<LiveProgramme>> {
        val directory = File(context.cacheDir, GUIDE_DIRECTORY).apply { mkdirs() }
        val guide = File(directory, "$day.xml")
        if (!guide.isFile || guide.length() == 0L) download(GUIDE_URL, guide)
        directory.listFiles()?.forEach { if (it != guide) it.delete() }
        return guide.inputStream().buffered().use { XmltvParser.parse(it) }.toGuide()
    }

    /** Downloads to a sibling file first so that an interrupted run cannot leave a partial guide. */
    private fun download(url: String, target: File) {
        val partial = File(target.parentFile, "${target.name}.part")
        try {
            httpFetch(url) { body ->
                body.unpacked().use { xml -> partial.outputStream().use { xml.copyTo(it) } }
            }
            if (!partial.renameTo(target)) throw IOException("Could not store the downloaded guide")
        } finally {
            partial.delete()
        }
    }

    private fun today(): String {
        val calendar = Calendar.getInstance()
        return "%04d%02d%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
        )
    }

    private companion object {
        const val TAG = "Epg"
        const val GUIDE_URL = "https://epg.112114.xyz/pp.xml.gz"
        const val GUIDE_DIRECTORY = "epg"
    }
}
