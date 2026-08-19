package dev.anilbeesetti.nextplayer.core.model

import java.io.Serializable

/**
 * A single live TV channel parsed from an m3u playlist.
 *
 * [group] comes from the `group-title` attribute and is used to categorise channels. [logoUrl] is
 * the `tvg-logo` attribute when present.
 *
 * [urls] holds every line the playlist offers for this channel, in the order the playlist lists
 * them. Public IPTV playlists routinely give the same channel several lines because any one of
 * them may be dead, so playback falls back down the list rather than failing outright.
 */
data class LiveChannel(
    val name: String,
    val urls: List<String>,
    val group: String = "",
    val logoUrl: String? = null,
    val tvgId: String? = null,
) : Serializable {

    init {
        require(urls.isNotEmpty()) { "A channel needs at least one line" }
    }

    /** The line to start on: playlists put their most reliable line first. */
    val url: String get() = urls.first()

    companion object {
        const val UNGROUPED = ""
    }
}
