package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.model.GroupedItem
import dev.anilbeesetti.nextplayer.core.model.LibraryEpisode
import dev.anilbeesetti.nextplayer.core.model.LibrarySeason
import dev.anilbeesetti.nextplayer.core.model.WORK_PICKER_COLUMNS
import dev.anilbeesetti.nextplayer.core.model.WorkPickerBand
import dev.anilbeesetti.nextplayer.core.model.WorkPickerCursor
import dev.anilbeesetti.nextplayer.core.model.moveWorkPickerCursor
import dev.anilbeesetti.nextplayer.core.model.workPickerCursorToOpen
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.feature.player.PlayerViewModel
import dev.anilbeesetti.nextplayer.feature.player.state.rememberPlaylistState

class WorkPickerKeySink {
    var onKeyCode: (keyCode: Int, isDown: Boolean) -> Boolean = { _, _ -> false }
}

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.WorkPickerView(
    show: Boolean,
    workId: Long,
    player: Player,
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    keySink: WorkPickerKeySink? = null,
) {
    if (!show) return
    val detail by remember(workId) { viewModel.workDetail(workId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val playlistState = rememberPlaylistState(player)
    val currentMediaId = playlistState.playlist.getOrNull(playlistState.currentMediaItemIndex)?.mediaId
    val seasons = detail?.seasons.orEmpty()
    var cursor by remember(detail?.work?.id, currentMediaId) {
        mutableStateOf(
            workPickerCursorToOpen(seasons, currentMediaId, detail?.focusedEpisodeId),
        )
    }
    LaunchedEffect(seasons, currentMediaId, detail?.focusedEpisodeId) {
        if (cursor == null) {
            cursor = workPickerCursorToOpen(seasons, currentMediaId, detail?.focusedEpisodeId)
        }
    }
    val season = seasons.getOrNull(cursor?.seasonIndex ?: -1) ?: seasons.firstOrNull()
    val gridState = rememberLazyGridState()

    SideEffect {
        if (keySink != null) {
            keySink.onKeyCode = { keyCode, isDown ->
                handlePickerKey(
                    keyCode = keyCode,
                    isDown = isDown,
                    seasons = seasons,
                    cursor = cursor,
                    // Asked as the key arrives, so the step matches the row the grid last laid out
                    // rather than the row someone expected it to lay out.
                    columns = gridState.layoutInfo.maxSpan,
                    currentMediaId = currentMediaId,
                    focusedEpisodeId = detail?.focusedEpisodeId,
                    onCursor = { cursor = it },
                    onPlay = { episode ->
                        viewModel.playEpisode(player, episode, detail?.work?.title.orEmpty())
                        onDismiss()
                    },
                )
            }
        }
    }
    DisposableEffect(keySink) {
        onDispose {
            keySink?.onKeyCode = { _, _ -> false }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        if (seasons.size > 1 || season?.season != GroupedItem.FILM_SEASON) {
            SeasonTabs(
                seasons = seasons,
                selectedSeason = season?.season,
                highlighted = cursor?.band == WorkPickerBand.SEASONS,
                onSelect = { picked ->
                    val index = seasons.indexOfFirst { it.season == picked.season }
                    if (index >= 0) {
                        cursor = WorkPickerCursor(
                            band = WorkPickerBand.EPISODES,
                            seasonIndex = index,
                            episodeIndex = 0,
                        )
                    }
                },
            )
        }
        if (season != null) {
            EpisodeGrid(
                season = season,
                state = gridState,
                currentMediaId = currentMediaId,
                cursorEpisodeIndex = cursor
                    ?.takeIf { it.band == WorkPickerBand.EPISODES }
                    ?.episodeIndex,
                onEpisodeClick = { episode ->
                    viewModel.playEpisode(player, episode, detail?.work?.title.orEmpty())
                    onDismiss()
                },
            )
        }
    }
}

private fun handlePickerKey(
    keyCode: Int,
    isDown: Boolean,
    seasons: List<LibrarySeason>,
    cursor: WorkPickerCursor?,
    columns: Int,
    currentMediaId: String?,
    focusedEpisodeId: Long?,
    onCursor: (WorkPickerCursor) -> Unit,
    onPlay: (LibraryEpisode) -> Unit,
): Boolean {
    if (!isWorkPickerHandledKey(keyCode)) return false
    if (!isDown) return true
    val current = cursor
        ?: workPickerCursorToOpen(seasons, currentMediaId, focusedEpisodeId)?.also(onCursor)
        ?: return true
    if (isWorkPickerConfirmKey(keyCode)) {
        if (current.band == WorkPickerBand.SEASONS) {
            onCursor(current.copy(band = WorkPickerBand.EPISODES))
        } else {
            seasons.getOrNull(current.seasonIndex)
                ?.episodes
                ?.getOrNull(current.episodeIndex)
                ?.let(onPlay)
        }
        return true
    }
    val direction = workPickerDirectionOf(keyCode) ?: return true
    onCursor(
        moveWorkPickerCursor(
            cursor = current,
            seasons = seasons,
            direction = direction,
            // The grid has nothing to report until it has been laid out once.
            columns = columns.takeIf { it > 0 } ?: WORK_PICKER_COLUMNS,
            currentMediaKey = currentMediaId,
            focusedEpisodeId = focusedEpisodeId,
        ),
    )
    return true
}

@Composable
private fun SeasonTabs(
    seasons: List<LibrarySeason>,
    selectedSeason: Int?,
    highlighted: Boolean,
    onSelect: (LibrarySeason) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedSeason) {
        val index = seasons.indexOfFirst { it.season == selectedSeason }
        if (index >= 0) listState.scrollToItem(index)
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        itemsIndexed(seasons, key = { _, item -> item.season }) { _, season ->
            val selected = season.season == selectedSeason
            val shape = RoundedCornerShape(10.dp)
            Column(
                modifier = Modifier
                    .widthIn(min = 96.dp)
                    .thenIf(selected && highlighted) {
                        border(3.dp, Color.White, shape)
                    }
                    .background(
                        color = if (selected) {
                            Color.White.copy(alpha = 0.28f)
                        } else {
                            Color.White.copy(alpha = 0.12f)
                        },
                        shape = shape,
                    )
                    .clickable { onSelect(season) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = if (season.season == GroupedItem.FILM_SEASON) {
                        stringResource(R.string.play)
                    } else {
                        stringResource(R.string.season_number, season.season)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.episode_count, season.episodes.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun EpisodeGrid(
    season: LibrarySeason,
    state: LazyGridState,
    currentMediaId: String?,
    cursorEpisodeIndex: Int?,
    onEpisodeClick: (LibraryEpisode) -> Unit,
) {
    LaunchedEffect(season.season, cursorEpisodeIndex) {
        val index = cursorEpisodeIndex ?: return@LaunchedEffect
        if (index in season.episodes.indices) state.scrollToItem(index)
    }
    LazyVerticalGrid(
        // How wide the screen is decides the row, so picking up the remote does not re-lay it out.
        // The cursor steps by what the grid reports laying out, so any row size navigates correctly.
        columns = GridCells.Adaptive(minSize = 88.dp),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(season.episodes, key = { _, item -> item.id }) { index, episode ->
            val current = episode.mediaKey != null && episode.mediaKey == currentMediaId
            val highlighted = cursorEpisodeIndex == index
            val shape = RoundedCornerShape(8.dp)
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .thenIf(highlighted) { border(3.dp, Color.White, shape) }
                    .background(
                        color = if (current) {
                            Color.White.copy(alpha = 0.32f)
                        } else {
                            Color.White.copy(alpha = 0.14f)
                        },
                        shape = shape,
                    )
                    .clickable { onEpisodeClick(episode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (episode.season == GroupedItem.FILM_SEASON) {
                        stringResource(R.string.play)
                    } else {
                        episode.episode.toString()
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            }
        }
    }
}
