package dev.anilbeesetti.nextplayer.feature.player

/**
 * Intent extra naming the media item independently of the URI used to reach it.
 *
 * Playback state is stored per media id, which defaults to the URI. That works for local files but
 * not for network streams, whose local proxy URL changes every session. Callers that know a durable
 * identity pass it here so resume position and track choices survive across sessions.
 */
const val EXTRA_MEDIA_KEY = "dev.anilbeesetti.nextplayer.extra.MEDIA_KEY"

/**
 * Intent extra giving a durable identity to every item of the playlist, in the same order.
 *
 * A folder on a network share is queued whole, so the items played after the requested one need
 * their own identities too — otherwise they would be remembered under a proxy URL that is already
 * gone by the next session.
 */
const val EXTRA_MEDIA_KEYS = "dev.anilbeesetti.nextplayer.extra.MEDIA_KEYS"

/**
 * Intent extra listing every line a broadcast can be reached by, most reliable first.
 *
 * A channel is one station carried by several servers, any of which may be down or unreachable from
 * where the viewer is. Handing the player all of them lets it move on to the next by itself instead
 * of asking the viewer to guess which of a list of identically named entries works today.
 */
const val EXTRA_LIVE_LINES = "dev.anilbeesetti.nextplayer.extra.LIVE_LINES"

/**
 * Intent extra naming the media in a way worth showing to the user.
 *
 * Without it a title is taken from the URI, which suits a file but not a live channel: its URL says
 * nothing, while the playlist it came from knows it as, say, CCTV-1.
 */
const val EXTRA_MEDIA_TITLE = "dev.anilbeesetti.nextplayer.extra.MEDIA_TITLE"
