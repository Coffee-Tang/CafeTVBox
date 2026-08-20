package dev.anilbeesetti.nextplayer.core.model

/** One episode, or the single item a film is, as the work page lists it. */
data class LibraryEpisode(
    val id: Long,
    val season: Int,
    val episode: Int,
    val mediaKey: String?,
    val lastPlayedTime: Long? = null,
    val playbackPosition: Long = 0,
)

/** The episodes of one season, in episode order. */
data class LibrarySeason(
    val season: Int,
    val episodes: List<LibraryEpisode>,
)

/**
 * A work as the picker shows it: seasons on one page, and the episode focus should land on.
 *
 * [focusedEpisodeId] is the last-played episode when any has been, otherwise the first episode.
 */
data class WorkDetail(
    val work: LibraryWork,
    val seasons: List<LibrarySeason>,
    val focusedEpisodeId: Long?,
)

/**
 * The episode a work should carry on from: the one watched last, otherwise its first.
 *
 * One rule with two callers, which have to agree: the episode the picker lands on is the episode
 * playing the work starts.
 */
fun List<LibraryEpisode>.episodeToResume(): LibraryEpisode? =
    filter { it.lastPlayedTime != null }.maxByOrNull { it.lastPlayedTime!! }
        ?: minWithOrNull(compareBy(LibraryEpisode::season, LibraryEpisode::episode))

/** Folds [episodes] into seasons and picks the episode the work page should focus. */
fun workDetailOf(
    work: LibraryWork,
    episodes: List<LibraryEpisode>,
): WorkDetail {
    val seasons = episodes
        .groupBy { it.season }
        .toSortedMap()
        .map { (season, items) -> LibrarySeason(season, items.sortedBy { it.episode }) }
    return WorkDetail(work = work, seasons = seasons, focusedEpisodeId = episodes.episodeToResume()?.id)
}

/** The season that holds [focusedEpisodeId], or the first season when none has been played. */
fun focusedSeason(
    seasons: List<LibrarySeason>,
    focusedEpisodeId: Long?,
): LibrarySeason? =
    seasons.firstOrNull { season -> season.episodes.any { it.id == focusedEpisodeId } }
        ?: seasons.firstOrNull()

/**
 * The season the picker should open on: the one now playing, otherwise the last-played, otherwise
 * the first.
 */
fun seasonToOpen(
    seasons: List<LibrarySeason>,
    currentMediaKey: String?,
    focusedEpisodeId: Long?,
): LibrarySeason? =
    seasons.firstOrNull { season ->
        season.episodes.any { it.mediaKey != null && it.mediaKey == currentMediaKey }
    } ?: focusedSeason(seasons, focusedEpisodeId)

/** The episode the picker should land on within [season]. */
fun episodeToOpen(
    season: LibrarySeason?,
    currentMediaKey: String?,
    focusedEpisodeId: Long?,
): LibraryEpisode? {
    season ?: return null
    return season.episodes.firstOrNull { it.mediaKey != null && it.mediaKey == currentMediaKey }
        ?: season.episodes.firstOrNull { it.id == focusedEpisodeId }
        ?: season.episodes.firstOrNull()
}
