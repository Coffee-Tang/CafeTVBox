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
    /**
     * The work this belongs to, so that reopening it can still offer the rest of the work.
     *
     * Null for anything no library has catalogued: a live channel, a URL played once, a file
     * outside every library.
     */
    val workId: Long? = null,
    /**
     * Which episode was reached, for a work watched an episode at a time.
     *
     * Both are null for anything with nothing to count through: a film, a live channel, a file no
     * library has catalogued.
     */
    val season: Int? = null,
    val episode: Int? = null,
) {
    /**
     * Whether this was watched as it was broadcast, rather than played from end to end.
     *
     * A broadcast has no length worth recording: what a live stream reports is the rolling window it
     * keeps, which says nothing about what is on. So a stream that was left without a length was
     * live, whether it is named by its station or, as older entries are, by the line it was on. One
     * whose length was never learnt because playback failed at once reads as live too, which costs
     * nothing: it has no position to resume from either way.
     */
    val isLive: Boolean
        get() = source == Source.STREAM && durationMs == null

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
