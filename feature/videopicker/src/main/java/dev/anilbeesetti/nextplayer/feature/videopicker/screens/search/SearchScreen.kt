package dev.anilbeesetti.nextplayer.feature.videopicker.screens.search

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.anilbeesetti.nextplayer.core.domain.SearchResults
import dev.anilbeesetti.nextplayer.core.model.Folder
import dev.anilbeesetti.nextplayer.core.model.LibraryWork
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.model.MediaLayoutMode
import dev.anilbeesetti.nextplayer.core.model.RecentMedium
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.model.WorkKind
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextSegmentedListItem
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.core.ui.extensions.plus
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.FolderItem
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.VideoItem
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.episodeLabel

@Composable
fun SearchRoute(
    viewModel: SearchViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)

    SearchScreen(
        uiState = uiState,
        onNavigateUp = viewModel::onNavigateUp,
        onFolderClick = viewModel::onOpenFolder,
        onVideoClick = viewModel::onPlayVideo,
        onWorkClick = viewModel::onPlayWork,
        onChannelClick = viewModel::onPlayChannel,
        onRecentClick = viewModel::onResume,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    uiState: SearchUiState,
    onNavigateUp: () -> Unit = {},
    onFolderClick: (String) -> Unit = {},
    onVideoClick: (Uri) -> Unit = {},
    onWorkClick: (LibraryWork) -> Unit = {},
    onChannelClick: (LiveChannel) -> Unit = {},
    onRecentClick: (RecentMedium) -> Unit = {},
    onEvent: (SearchUiEvent) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { onEvent(SearchUiEvent.OnQueryChange(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_everything),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { onEvent(SearchUiEvent.OnQueryChange("")) }) {
                                    Icon(
                                        imageVector = NextIcons.Close,
                                        contentDescription = stringResource(R.string.clear_history),
                                    )
                                }
                            } else if (uiState.isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                onEvent(SearchUiEvent.OnSearch(uiState.query))
                                keyboardController?.hide()
                            },
                        ),
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            errorBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                        ),
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = scaffoldPadding.calculateTopPadding())
                .padding(start = scaffoldPadding.calculateStartPadding(LocalLayoutDirection.current)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.background),
            ) {
                val updatedScaffoldPadding = scaffoldPadding.copy(top = 0.dp, start = 0.dp)
                if (uiState.query.isBlank()) {
                    SuggestionsContent(
                        searchHistory = uiState.searchHistory,
                        recentlyPlayed = uiState.recentlyPlayed,
                        contentPadding = updatedScaffoldPadding,
                        onHistoryItemClick = { onEvent(SearchUiEvent.OnHistoryItemClick(it)) },
                        onRemoveHistoryItem = { onEvent(SearchUiEvent.OnRemoveHistoryItem(it)) },
                        onClearHistory = { onEvent(SearchUiEvent.OnClearHistory) },
                        onRecentClick = onRecentClick,
                    )
                } else {
                    SearchResultsContent(
                        uiState = uiState,
                        contentPadding = updatedScaffoldPadding,
                        onFolderClick = onFolderClick,
                        onVideoClick = onVideoClick,
                        onWorkClick = onWorkClick,
                        onChannelClick = onChannelClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionsContent(
    searchHistory: List<String>,
    recentlyPlayed: List<RecentMedium>,
    contentPadding: PaddingValues = PaddingValues(),
    onHistoryItemClick: (String) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRecentClick: (RecentMedium) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp) + contentPadding,
    ) {
        if (searchHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ListSectionTitle(
                        text = stringResource(R.string.recent_searches),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(text = stringResource(R.string.clear_history))
                    }
                }
            }

            items(
                items = searchHistory,
                key = { "history_$it" },
            ) { query ->
                SearchHistoryItem(
                    query = query,
                    onClick = { onHistoryItemClick(query) },
                    onRemove = { onRemoveHistoryItem(query) },
                )
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item {
                ListSectionTitle(
                    text = stringResource(R.string.recently_played),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = if (searchHistory.isNotEmpty()) 20.dp else 12.dp,
                        bottom = 8.dp,
                    ),
                )
            }

            itemsIndexed(
                items = recentlyPlayed,
                key = { _, medium -> "recent_${medium.mediaKey}" },
            ) { index, medium ->
                RecentItem(
                    medium = medium,
                    isFirstItem = index == 0,
                    isLastItem = index == recentlyPlayed.lastIndex,
                    onClick = { onRecentClick(medium) },
                )
            }
        }

        if (searchHistory.isEmpty() && recentlyPlayed.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = NextIcons.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.search_everything),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchHistoryItem(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    NextSegmentedListItem(
        modifier = Modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        onClick = onClick,
        leadingContent = {
            Icon(
                imageVector = NextIcons.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = NextIcons.Close,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        content = {
            Text(
                text = query,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

/**
 * What was found, gathered under where it was found: works in a library, live channels, and the
 * files on the device itself.
 *
 * Each group shows a few answers and offers the rest, because reaching the group below by holding
 * the arrow key through every channel named `CCTV` is worse than one press on "see all".
 */
@Composable
private fun SearchResultsContent(
    uiState: SearchUiState,
    contentPadding: PaddingValues = PaddingValues(),
    onFolderClick: (String) -> Unit,
    onVideoClick: (Uri) -> Unit,
    onWorkClick: (LibraryWork) -> Unit,
    onChannelClick: (LiveChannel) -> Unit,
) {
    AnimatedVisibility(
        visible = uiState.isSearching,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 100.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            CircularProgressIndicator()
        }
    }

    AnimatedVisibility(
        visible = !uiState.isSearching,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        if (!uiState.hasResults) {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 100.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = NextIcons.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.no_results_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val shownInFull = remember { mutableStateListOf<Int>() }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp) + contentPadding,
            ) {
                resultGroup(
                    titleRes = R.string.media_library,
                    items = uiState.workResults,
                    shownInFull = shownInFull,
                    key = { "work_${it.id}" },
                ) { work, isFirst, isLast ->
                    WorkResultItem(
                        work = work,
                        isFirstItem = isFirst,
                        isLastItem = isLast,
                        onClick = { onWorkClick(work) },
                    )
                }

                resultGroup(
                    titleRes = R.string.live_tv,
                    items = uiState.channelResults,
                    shownInFull = shownInFull,
                    key = { "channel_${it.name}" },
                ) { channel, isFirst, isLast ->
                    ChannelResultItem(
                        channel = channel,
                        isFirstItem = isFirst,
                        isLastItem = isLast,
                        onClick = { onChannelClick(channel) },
                    )
                }

                resultGroup(
                    titleRes = R.string.folders,
                    items = uiState.searchResults.folders,
                    shownInFull = shownInFull,
                    key = { "folder_${it.path}" },
                ) { folder, isFirst, isLast ->
                    FolderItem(
                        folder = folder,
                        isRecentlyPlayedFolder = false,
                        preferences = uiState.preferences.copy(mediaLayoutMode = MediaLayoutMode.LIST),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        isFirstItem = isFirst,
                        isLastItem = isLast,
                        onClick = { onFolderClick(folder.path) },
                    )
                }

                resultGroup(
                    titleRes = R.string.videos,
                    items = uiState.searchResults.videos,
                    shownInFull = shownInFull,
                    key = { "video_${it.uriString}" },
                ) { video, isFirst, isLast ->
                    VideoItem(
                        video = video,
                        isRecentlyPlayedVideo = false,
                        preferences = uiState.preferences.copy(mediaLayoutMode = MediaLayoutMode.LIST),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        isFirstItem = isFirst,
                        isLastItem = isLast,
                        onClick = { onVideoClick(video.uriString.toUri()) },
                    )
                }
            }
        }
    }
}

/** How many answers a group shows before the rest have to be asked for. */
private const val GROUP_PREVIEW = 6

private fun <T> LazyListScope.resultGroup(
    @StringRes titleRes: Int,
    items: List<T>,
    shownInFull: MutableList<Int>,
    key: (T) -> Any,
    row: @Composable (T, Boolean, Boolean) -> Unit,
) {
    if (items.isEmpty()) return
    val inFull = titleRes in shownInFull
    val shown = if (inFull) items else items.take(GROUP_PREVIEW)

    item(key = "title_$titleRes") {
        ListSectionTitle(
            text = stringResource(titleRes),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, bottom = 8.dp),
        )
    }

    itemsIndexed(items = shown, key = { _, item -> key(item) }) { index, item ->
        row(item, index == 0, index == shown.lastIndex)
    }

    if (!inFull && items.size > shown.size) {
        item(key = "more_$titleRes") {
            TextButton(
                onClick = { shownInFull.add(titleRes) },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(text = stringResource(R.string.see_all))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WorkResultItem(
    work: LibraryWork,
    isFirstItem: Boolean,
    isLastItem: Boolean,
    onClick: () -> Unit,
) {
    NextSegmentedListItem(
        modifier = Modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        onClick = onClick,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 60.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = NextIcons.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AsyncImage(
                    model = work.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
        supportingContent = work.year?.let {
            {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        content = {
            Text(
                text = work.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChannelResultItem(
    channel: LiveChannel,
    isFirstItem: Boolean,
    isLastItem: Boolean,
    onClick: () -> Unit,
) {
    NextSegmentedListItem(
        modifier = Modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        onClick = onClick,
        leadingContent = {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = NextIcons.Live,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
        supportingContent = channel.group.takeIf { it.isNotBlank() }?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        content = {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecentItem(
    medium: RecentMedium,
    isFirstItem: Boolean,
    isLastItem: Boolean,
    onClick: () -> Unit,
) {
    val episode = medium.episodeLabel()
    NextSegmentedListItem(
        modifier = Modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        onClick = onClick,
        leadingContent = {
            Icon(
                imageVector = if (medium.isLive) NextIcons.Live else NextIcons.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        supportingContent = episode?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        content = {
            Text(
                text = medium.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@PreviewLightDark
@Composable
private fun SearchScreenEmptyPreview() {
    NextPlayerTheme {
        SearchScreen(
            uiState = SearchUiState(),
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchScreenWithHistoryPreview() {
    NextPlayerTheme {
        SearchScreen(
            uiState = SearchUiState(
                searchHistory = listOf("avengers", "movie", "trailer"),
                recentlyPlayed = listOf(
                    RecentMedium(
                        mediaKey = "content://sample/silicon_valley_s03e05.mkv",
                        title = "硅谷",
                        source = RecentMedium.Source.LOCAL,
                        positionMs = 45_000,
                        durationMs = 1_800_000,
                        lastPlayedTime = System.currentTimeMillis(),
                        season = 3,
                        episode = 5,
                    ),
                ),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchScreenWithResultsPreview() {
    NextPlayerTheme {
        SearchScreen(
            uiState = SearchUiState(
                query = "valley",
                workResults = listOf(
                    LibraryWork(
                        id = 1,
                        libraryId = 1,
                        workKey = "silicon valley",
                        kind = WorkKind.SERIES,
                        title = "硅谷",
                        otherTitle = "Silicon Valley",
                        year = 2014,
                        posterUrl = null,
                    ),
                ),
                channelResults = listOf(
                    LiveChannel(name = "CCTV-5 体育", urls = listOf("http://example/cctv5.m3u8"), group = "央视"),
                ),
                searchResults = SearchResults(
                    folders = listOf(
                        Folder(
                            name = "Movies",
                            path = "/storage/Movies",
                            dateModified = System.currentTimeMillis(),
                        ),
                    ),
                    videos = listOf(
                        Video.sample.copy(nameWithExtension = "Movie_Clip.mp4", uriString = "content://sample/movie_clip.mp4"),
                    ),
                ),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchScreenNoResultsPreview() {
    NextPlayerTheme {
        SearchScreen(
            uiState = SearchUiState(
                query = "xyz123",
                searchResults = SearchResults(),
            ),
        )
    }
}
