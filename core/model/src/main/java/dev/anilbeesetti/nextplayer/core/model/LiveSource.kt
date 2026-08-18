package dev.anilbeesetti.nextplayer.core.model

import java.io.Serializable

/**
 * A saved IPTV subscription: a named pointer to an m3u playlist URL. Channels are parsed from
 * [url] on demand rather than persisted, so the list always reflects the latest source.
 */
data class LiveSource(
    val id: Long = 0,
    val name: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
) : Serializable {

    companion object {
        val sample = LiveSource(
            id = 1,
            name = "IPTV",
            url = "https://live.fanmingming.com/tv/m3u/ipv6.m3u",
        )
    }
}
