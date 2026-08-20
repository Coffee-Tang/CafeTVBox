package dev.anilbeesetti.nextplayer.core.model

enum class WorkPickerBand {
    SEASONS,
    EPISODES,
}

enum class WorkPickerDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

data class WorkPickerCursor(
    val band: WorkPickerBand,
    val seasonIndex: Int,
    val episodeIndex: Int,
)

const val WORK_PICKER_COLUMNS = 6

fun hasSeasonTabs(seasons: List<LibrarySeason>): Boolean {
    val only = seasons.singleOrNull()
    return seasons.size > 1 || (only != null && only.season != GroupedItem.FILM_SEASON)
}

fun shouldOpenWorkPickerOnDown(controlsVisible: Boolean, hasWork: Boolean): Boolean =
    !controlsVisible && hasWork

fun workPickerCursorToOpen(
    seasons: List<LibrarySeason>,
    currentMediaKey: String?,
    focusedEpisodeId: Long?,
): WorkPickerCursor? {
    if (seasons.isEmpty()) return null
    val season = seasonToOpen(seasons, currentMediaKey, focusedEpisodeId) ?: seasons.first()
    val seasonIndex = seasons.indexOf(season).coerceAtLeast(0)
    val episode = episodeToOpen(seasons[seasonIndex], currentMediaKey, focusedEpisodeId)
    val episodeIndex = seasons[seasonIndex].episodes.indexOfFirst { it.id == episode?.id }
        .coerceAtLeast(0)
    return WorkPickerCursor(
        band = WorkPickerBand.EPISODES,
        seasonIndex = seasonIndex,
        episodeIndex = episodeIndex,
    )
}

fun moveWorkPickerCursor(
    cursor: WorkPickerCursor,
    seasons: List<LibrarySeason>,
    direction: WorkPickerDirection,
    columns: Int = WORK_PICKER_COLUMNS,
    currentMediaKey: String? = null,
    focusedEpisodeId: Long? = null,
): WorkPickerCursor {
    if (seasons.isEmpty()) return cursor
    val seasonIndex = cursor.seasonIndex.coerceIn(0, seasons.lastIndex)
    val episodes = seasons[seasonIndex].episodes
    val episodeIndex = cursor.episodeIndex.coerceIn(0, episodes.lastIndex.coerceAtLeast(0))
    val tabs = hasSeasonTabs(seasons)
    return when (cursor.band) {
        WorkPickerBand.SEASONS -> moveOnSeasons(
            seasonIndex = seasonIndex,
            seasons = seasons,
            direction = direction,
            currentMediaKey = currentMediaKey,
            focusedEpisodeId = focusedEpisodeId,
        )
        WorkPickerBand.EPISODES -> moveOnEpisodes(
            seasonIndex = seasonIndex,
            episodeIndex = episodeIndex,
            episodeCount = episodes.size,
            columns = columns,
            hasSeasonTabs = tabs,
            direction = direction,
        )
    }
}

private fun moveOnSeasons(
    seasonIndex: Int,
    seasons: List<LibrarySeason>,
    direction: WorkPickerDirection,
    currentMediaKey: String?,
    focusedEpisodeId: Long?,
): WorkPickerCursor {
    val nextSeason = when (direction) {
        WorkPickerDirection.LEFT -> (seasonIndex - 1).coerceAtLeast(0)
        WorkPickerDirection.RIGHT -> (seasonIndex + 1).coerceAtMost(seasons.lastIndex)
        WorkPickerDirection.UP -> seasonIndex
        WorkPickerDirection.DOWN -> seasonIndex
    }
    val band = if (direction == WorkPickerDirection.DOWN) {
        WorkPickerBand.EPISODES
    } else {
        WorkPickerBand.SEASONS
    }
    val episode = episodeToOpen(seasons[nextSeason], currentMediaKey, focusedEpisodeId)
    val episodeIndex = seasons[nextSeason].episodes.indexOfFirst { it.id == episode?.id }
        .coerceAtLeast(0)
    return WorkPickerCursor(band = band, seasonIndex = nextSeason, episodeIndex = episodeIndex)
}

private fun moveOnEpisodes(
    seasonIndex: Int,
    episodeIndex: Int,
    episodeCount: Int,
    columns: Int,
    hasSeasonTabs: Boolean,
    direction: WorkPickerDirection,
): WorkPickerCursor {
    if (episodeCount == 0) {
        return WorkPickerCursor(WorkPickerBand.SEASONS, seasonIndex, 0)
    }
    val lastIndex = episodeCount - 1
    val lastRowStart = (lastIndex / columns) * columns
    val nextIndex = when (direction) {
        WorkPickerDirection.LEFT ->
            if (episodeIndex % columns == 0) episodeIndex else episodeIndex - 1
        WorkPickerDirection.RIGHT ->
            if (episodeIndex == lastIndex || episodeIndex % columns == columns - 1) {
                episodeIndex
            } else {
                episodeIndex + 1
            }
        WorkPickerDirection.UP ->
            if (episodeIndex < columns) episodeIndex else episodeIndex - columns
        WorkPickerDirection.DOWN ->
            if (episodeIndex >= lastRowStart) {
                episodeIndex
            } else {
                (episodeIndex + columns).coerceAtMost(lastIndex)
            }
    }
    val band = if (direction == WorkPickerDirection.UP && episodeIndex < columns && hasSeasonTabs) {
        WorkPickerBand.SEASONS
    } else {
        WorkPickerBand.EPISODES
    }
    return WorkPickerCursor(band = band, seasonIndex = seasonIndex, episodeIndex = nextIndex)
}
