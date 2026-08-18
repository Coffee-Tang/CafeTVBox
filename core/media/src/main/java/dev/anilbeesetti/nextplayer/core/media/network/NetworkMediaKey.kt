package dev.anilbeesetti.nextplayer.core.media.network

/**
 * A durable identity for a file reached over a network connection.
 *
 * Network files are played through a local proxy whose URL carries a port that changes every
 * session, so the playable URL cannot identify a file. Keying on the connection and the path within
 * it keeps the resume position and track choices attached to the same file the next time the app
 * runs. The scheme keeps these keys from colliding with URIs of files the device can open directly.
 */
fun networkMediaKey(connectionId: Long, path: String): String =
    "cafeplayer-network://$connectionId/${path.trimStart('/')}"
