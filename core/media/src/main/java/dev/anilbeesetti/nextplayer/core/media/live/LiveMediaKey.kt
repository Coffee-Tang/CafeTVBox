package dev.anilbeesetti.nextplayer.core.media.live

/**
 * A durable identity for a live channel, the station rather than the line it was watched on.
 *
 * A channel is one station carried by several servers, and which of its lines comes first depends on
 * which playlists are configured and in what order. Keying on the station therefore keeps a channel
 * to one entry in playback history however many lines had to be tried, and stops a newly added
 * playlist from starting a second entry for a channel already watched. The scheme keeps these keys
 * apart from the URIs of streams the player can open on its own.
 */
data class LiveMediaKey(val channelKey: String) {

    override fun toString(): String = "$SCHEME://$channelKey"

    companion object {
        private const val SCHEME = "cafeplayer-live"

        /** Reads back a key written by [toString], or null for a key of any other kind. */
        fun of(stored: String): LiveMediaKey? {
            val channelKey = stored.substringAfter("$SCHEME://", missingDelimiterValue = "")
            return channelKey.takeIf { it.isNotEmpty() }?.let(::LiveMediaKey)
        }
    }
}
