package dev.anilbeesetti.nextplayer.feature.player

/**
 * Intent extra naming the media item independently of the URI used to reach it.
 *
 * Playback state is stored per media id, which defaults to the URI. That works for local files but
 * not for network streams, whose local proxy URL changes every session. Callers that know a durable
 * identity pass it here so resume position and track choices survive across sessions.
 */
const val EXTRA_MEDIA_KEY = "dev.anilbeesetti.nextplayer.extra.MEDIA_KEY"
