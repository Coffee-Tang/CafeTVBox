package dev.anilbeesetti.nextplayer.core.media.network

/**
 * A durable identity for a file reached over a network connection.
 *
 * Network files are played through a local proxy whose URL carries a port that changes every
 * session, so the playable URL cannot identify a file. Keying on the connection and the path within
 * it keeps the resume position, track choices and playback history attached to the same file the
 * next time the app runs. The scheme keeps these keys apart from the URIs of files the device can
 * open on its own.
 */
data class NetworkMediaKey(val connectionId: Long, val path: String) {

    val fileName: String get() = path.substringAfterLast('/')

    override fun toString(): String = "$SCHEME://$connectionId/${path.trimStart('/')}"

    companion object {
        private const val SCHEME = "cafeplayer-network"

        /** Reads back a key written by [toString], or null for a key of any other kind. */
        fun of(stored: String): NetworkMediaKey? {
            val body = stored.substringAfter("$SCHEME://", missingDelimiterValue = "")
            val connectionId = body.substringBefore('/').toLongOrNull() ?: return null
            return NetworkMediaKey(
                connectionId = connectionId,
                path = body.substringAfter('/', missingDelimiterValue = ""),
            )
        }
    }
}
