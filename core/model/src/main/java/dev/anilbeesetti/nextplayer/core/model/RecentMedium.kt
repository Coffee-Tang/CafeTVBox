package dev.anilbeesetti.nextplayer.core.model

/**
 * Something the user has watched, as playback history presents it.
 */
data class RecentMedium(
    /** Identifies the item, and is what playback resumes from. */
    val mediaKey: String,
    val title: String,
    val source: Source,
    val positionMs: Long,
    val durationMs: Long?,
    val lastPlayedTime: Long,
) {
    /**
     * How far playback reached, as a fraction of the whole, or null when there is nothing to
     * resume: a live channel has no end, and an item just started has no progress to show.
     */
    val progress: Float?
        get() {
            if (durationMs == null || durationMs <= 0L || positionMs <= 0L) return null
            return (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        }

    /** Where the item comes from, which decides how it is labelled and how it is reopened. */
    enum class Source {
        /** A file in the device's own library. */
        LOCAL,

        /** A file on a network connection, reachable only while that server is up. */
        SHARE,

        /** A URL played as it arrives, such as a live channel. */
        STREAM,
    }
}
