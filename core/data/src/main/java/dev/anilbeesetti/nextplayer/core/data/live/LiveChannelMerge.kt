package dev.anilbeesetti.nextplayer.core.data.live

import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.model.channelKey

/**
 * How many lines a channel keeps. Playback waits up to fifteen seconds on a line that never
 * arrives, so a station carried by a dozen sources could otherwise show black for minutes.
 */
const val MAX_LINES_PER_CHANNEL = 5

/**
 * Gathers the channels of several playlists into one list, a station at a time.
 *
 * A station named by more than one playlist becomes a single channel holding every line all of
 * them offer, so playback can fall back from one source onto another. [playlists] are given
 * most-trusted first: the playlist that names a station first decides its name, group, artwork and
 * position, and a later one only fills in artwork or a tvg-id that is still missing. This is the
 * rule [M3uParser] already applies to repeated entries within a playlist, one level up.
 *
 * Lines follow the same priority, each playlist keeping its own order. Identical lines are dropped
 * before the list is cut to [maxLines] so that a source added twice cannot spend the whole budget.
 * Lines are compared as written, since a port, a query or an IPv6 literal really does tell two
 * lines apart.
 */
fun mergeChannels(
    playlists: List<List<LiveChannel>>,
    maxLines: Int = MAX_LINES_PER_CHANNEL,
): List<LiveChannel> {
    val channelsByKey = LinkedHashMap<String, LiveChannel>()
    for (playlist in playlists) {
        for (channel in playlist) {
            val key = channelKey(channel.name)
            val existing = channelsByKey[key]
            channelsByKey[key] = existing?.copy(
                urls = existing.urls + channel.urls,
                logoUrl = existing.logoUrl ?: channel.logoUrl,
                tvgId = existing.tvgId ?: channel.tvgId,
            ) ?: channel
        }
    }
    return channelsByKey.values.map { channel ->
        channel.copy(urls = channel.urls.map(String::trim).distinct().take(maxLines))
    }
}
