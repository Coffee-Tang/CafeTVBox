package dev.anilbeesetti.nextplayer.feature.videopicker.screens.history

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.common.Utils
import dev.anilbeesetti.nextplayer.core.model.RecentMedium
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.components.NextSegmentedListItem
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.rememberTvListFocusRequester
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.episodeLabel

@Composable
fun WatchHistoryScreenRoute(
    viewModel: WatchHistoryViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WatchHistoryScreen(
        uiState = uiState,
        onNavigateUp = viewModel::onNavigateUp,
        onItemClick = viewModel::resume,
        onRemoveItem = viewModel::remove,
        onClearAll = viewModel::clearAll,
    )
}

@Composable
internal fun WatchHistoryScreen(
    uiState: WatchHistoryUiState,
    onNavigateUp: () -> Unit,
    onItemClick: (RecentMedium) -> Unit,
    onRemoveItem: (RecentMedium) -> Unit,
    onClearAll: () -> Unit,
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    val showEmptyState = uiState.items.isEmpty() && !uiState.isLoading

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(R.string.watch_history),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp, modifier = Modifier.tvFocusRing()) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmation = true },
                            modifier = Modifier.tvFocusRing(),
                        ) {
                            Icon(
                                imageVector = NextIcons.Delete,
                                contentDescription = stringResource(R.string.clear_watch_history),
                            )
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        val containerModifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
            .padding(start = padding.calculateStartPadding(LocalLayoutDirection.current) + 2.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.background)

        Box(modifier = containerModifier) {
            if (showEmptyState) {
                WatchHistoryEmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .tvListFocus(rememberTvListFocusRequester()),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = padding.calculateBottomPadding() + 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(
                        items = uiState.items,
                        key = { _, item -> item.mediaKey },
                    ) { index, item ->
                        WatchHistoryItem(
                            medium = item,
                            isFirstItem = index == 0,
                            isLastItem = index == uiState.items.lastIndex,
                            onClick = { onItemClick(item) },
                            onRemove = { onRemoveItem(item) },
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        NextDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.clear_watch_history)) },
            content = { Text(stringResource(R.string.clear_watch_history_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearConfirmation = false
                    },
                ) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WatchHistoryItem(
    medium: RecentMedium,
    isFirstItem: Boolean,
    isLastItem: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    NextSegmentedListItem(
        contentPadding = PaddingValues(8.dp),
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        onClick = onClick,
        leadingContent = {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = medium.source.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        content = {
            Text(
                text = medium.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = medium.supportingText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove, modifier = Modifier.tvFocusRing()) {
                Icon(
                    imageVector = NextIcons.Delete,
                    contentDescription = stringResource(R.string.remove_from_watch_history),
                )
            }
        },
    )
}

/**
 * Which episode it reached, when it was last watched, and how far it got when that is worth saying.
 *
 * A stream has no length to be part of the way through, so only its source and time are shown.
 */
@Composable
private fun RecentMedium.supportingText(): String {
    val context = LocalContext.current
    val watched = remember(lastPlayedTime) {
        DateUtils.getRelativeTimeSpanString(
            lastPlayedTime,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }
    val sourceLabel = stringResource(id = source.labelRes)
    val duration = durationMs
    val progressLabel = when {
        source == RecentMedium.Source.STREAM -> null
        duration == null || positionMs <= 0L -> null
        else -> context.getString(
            R.string.watched_up_to,
            Utils.formatDurationMillis(positionMs),
            Utils.formatDurationMillis(duration),
        )
    }
    return listOfNotNull(episodeLabel(), watched, sourceLabel, progressLabel).joinToString(" · ")
}

private val RecentMedium.Source.icon
    get() = when (this) {
        RecentMedium.Source.LOCAL -> NextIcons.Movie
        RecentMedium.Source.SHARE -> NextIcons.Dns
        RecentMedium.Source.STREAM -> NextIcons.Live
    }

private val RecentMedium.Source.labelRes: Int
    get() = when (this) {
        RecentMedium.Source.LOCAL -> R.string.media_source_local
        RecentMedium.Source.SHARE -> R.string.media_source_share
        RecentMedium.Source.STREAM -> R.string.media_source_stream
    }

@Composable
private fun WatchHistoryEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = NextIcons.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.no_watch_history_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.no_watch_history_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewLightDark
@Composable
private fun WatchHistoryScreenPreview() {
    NextPlayerTheme {
        WatchHistoryScreen(
            uiState = WatchHistoryUiState(
                isLoading = false,
                items = listOf(
                    RecentMedium(
                        mediaKey = "cafeplayer-network://1/Shows/Episode.mkv",
                        title = "Episode.One.1080p.WEB-DL.mkv",
                        source = RecentMedium.Source.SHARE,
                        positionMs = 600_000,
                        durationMs = 1_800_000,
                        lastPlayedTime = System.currentTimeMillis(),
                    ),
                    RecentMedium(
                        mediaKey = "http://example.com/live.m3u8",
                        title = "CCTV-1",
                        source = RecentMedium.Source.STREAM,
                        positionMs = -1,
                        durationMs = null,
                        lastPlayedTime = System.currentTimeMillis(),
                    ),
                ),
            ),
            onNavigateUp = {},
            onItemClick = {},
            onRemoveItem = {},
            onClearAll = {},
        )
    }
}
