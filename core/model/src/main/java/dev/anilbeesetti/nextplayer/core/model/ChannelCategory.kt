package dev.anilbeesetti.nextplayer.core.model

/** The rough kind of station a channel is, as far as its name says. */
enum class ChannelCategory { CCTV, SATELLITE, OTHER }

/**
 * Sorts a channel by name into the few kinds a viewer looks for first.
 *
 * Playlists disagree about their own `group-title`s, so gathering several of them leaves dozens of
 * overlapping groups in two languages. These categories are instead read from the name, which the
 * playlists do agree about: the national broadcaster numbers its channels `CCTV`, and a provincial
 * broadcaster's national channel is a 卫视. Everything else is [ChannelCategory.OTHER] rather than a
 * guess, and remains reachable through the list of all channels.
 *
 * The station key rather than the name settles a CCTV channel, since it is the key that already
 * knows `CCTV-1综合`, `cctv 1` and `CCTV1` for one station; a name merely mentioning CCTV elsewhere
 * is not one of its channels.
 */
fun channelCategory(name: String): ChannelCategory = when {
    channelKey(name).startsWith("CCTV") -> ChannelCategory.CCTV
    name.contains("卫视") -> ChannelCategory.SATELLITE
    else -> ChannelCategory.OTHER
}
