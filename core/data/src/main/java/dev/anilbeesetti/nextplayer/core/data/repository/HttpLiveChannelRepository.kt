package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.live.M3uParser
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class HttpLiveChannelRepository @Inject constructor() : LiveChannelRepository {

    override suspend fun getChannels(url: String): Result<List<LiveChannel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val channels = M3uParser.parse(download(url))
                if (channels.isEmpty()) throw IOException("Playlist contains no channels")
                channels
            }
        }

    /**
     * Downloads [url] as text, following redirects manually so that http/https switches are
     * handled too (HttpURLConnection drops cross-protocol redirects).
     */
    private fun download(url: String): String {
        var currentUrl = url
        repeat(MAX_REDIRECTS) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try {
                when (val code = connection.responseCode) {
                    in 200..299 -> return connection.inputStream.bufferedReader().use { it.readText() }

                    in 300..399 -> {
                        val location = connection.getHeaderField("Location")
                            ?: throw IOException("Redirect without Location header")
                        currentUrl = URL(URL(currentUrl), location).toString()
                    }

                    else -> throw IOException("Failed to fetch playlist: HTTP $code")
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 20_000
        const val MAX_REDIRECTS = 5
        const val USER_AGENT = "CafeTVBox"
    }
}
