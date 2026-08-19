package dev.anilbeesetti.nextplayer.core.data.network

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches [url] and hands the response body to [read], returning what it produces.
 *
 * Redirects are followed by hand because HttpURLConnection drops the ones that switch between http
 * and https, which the hosts serving playlists and programme guides do routinely. The body is only
 * valid for the duration of [read]; the connection closes as soon as it returns.
 */
internal fun <T> httpFetch(url: String, read: (InputStream) -> T): T {
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
                in 200..299 -> return read(connection.inputStream)

                in 300..399 -> {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("Redirect without Location header")
                    currentUrl = URL(URL(currentUrl), location).toString()
                }

                else -> throw IOException("Request failed: HTTP $code")
            }
        } finally {
            connection.disconnect()
        }
    }
    throw IOException("Too many redirects")
}

/**
 * The body [url] serves, read as text, unpacked first if it turns out to have arrived gzipped.
 *
 * Named so that it can be handed to whatever wants a body without that caller having to say how a
 * body is fetched, which is what lets a test answer in its place.
 */
internal fun httpText(url: String): String = httpFetch(url) { body ->
    body.unpacked().bufferedReader().readText()
}

private const val CONNECT_TIMEOUT_MS = 15_000
private const val READ_TIMEOUT_MS = 20_000
private const val MAX_REDIRECTS = 5
private const val USER_AGENT = "CafeTVBox"
