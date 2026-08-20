package dev.anilbeesetti.nextplayer.core.data.history

import dev.anilbeesetti.nextplayer.core.data.mappers.toRecentMedium
import dev.anilbeesetti.nextplayer.core.database.entities.RecentPlayback
import dev.anilbeesetti.nextplayer.core.model.RecentMedium
import dev.anilbeesetti.nextplayer.core.model.WorkKind

/**
 * History as it is read: one entry per work, however many episodes of it were played.
 *
 * Watching a series leaves a row per episode, which is right for resuming but wrong for a list:
 * a season of it would push everything else off the screen. So episodes of one work collapse into
 * the episode watched last, which is also the one worth returning to. Anything the catalogue does
 * not know — a live channel, a URL played once, a file outside every library — stands on its own.
 *
 * Expects [this] newest first, as the query returns it, and keeps that order.
 */
internal fun List<RecentPlayback>.foldByWork(limit: Int): List<RecentMedium> {
    val seen = mutableSetOf<Long>()
    return asSequence()
        .filter { row -> row.workId?.let(seen::add) ?: true }
        .map { it.toHistoryEntry() }
        .take(limit)
        .toList()
}

/**
 * The work's name is preferred over the file's, so that entries left by earlier versions — named
 * after the file they were played from — read like the rest once the work is catalogued.
 */
private fun RecentPlayback.toHistoryEntry(): RecentMedium {
    val entry = state.toRecentMedium()
    return entry.copy(
        title = workTitle?.takeIf { it.isNotBlank() } ?: entry.title,
        workId = workId,
        season = season.takeIf { countsEpisodes },
        episode = episode.takeIf { countsEpisodes },
    )
}

/** A film is one item long, so numbering it as an episode would say nothing. */
private val RecentPlayback.countsEpisodes: Boolean
    get() = workKind == WorkKind.SERIES.name
