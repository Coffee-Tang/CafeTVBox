package dev.anilbeesetti.nextplayer.feature.live.screens.channels

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextSegmentedListItem
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.rememberTvListFocusRequester
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

@Composable
fun LiveChannelsScreenRoute(
    onNavigateUp: () -> Unit,
    onPlayChannel: (Uri) -> Unit,
    viewModel: LiveChannelsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LiveChannelsScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onPlayChannel = { channel -> onPlayChannel(channel.url.toUri()) },
        onSelectGroup = viewModel::selectGroup,
        onRetry = viewModel::refresh,
    )
}

@Composable
internal fun LiveChannelsScreen(
    uiState: LiveChannelsUiState,
    onNavigateUp: () -> Unit,
    onPlayChannel: (LiveChannel) -> Unit,
    onSelectGroup: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = uiState.sourceName.ifBlank { stringResource(R.string.live_tv) },
                fontWeight = FontWeight.Bold,
                navigationIcon = {
                    IconButton(onClick = onNavigateUp, modifier = Modifier.tvFocusRing()) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRetry, modifier = Modifier.tvFocusRing()) {
                        Icon(
                            imageVector = NextIcons.Update,
                            contentDescription = stringResource(R.string.refresh),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                )
                else -> ChannelBrowser(
                    uiState = uiState,
                    onPlayChannel = onPlayChannel,
                    onSelectGroup = onSelectGroup,
                )
            }
        }
    }
}

@Composable
private fun ChannelBrowser(
    uiState: LiveChannelsUiState,
    onPlayChannel: (LiveChannel) -> Unit,
    onSelectGroup: (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        GroupList(
            groups = uiState.groups,
            selectedIndex = uiState.selectedGroupIndex,
            onSelectGroup = onSelectGroup,
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainer),
        )
        ChannelList(
            channels = uiState.selectedChannels,
            onPlayChannel = onPlayChannel,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp))
                .background(MaterialTheme.colorScheme.background),
        )
    }
}

@Composable
private fun GroupList(
    groups: List<LiveChannelGroup>,
    selectedIndex: Int,
    onSelectGroup: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        itemsIndexed(items = groups, key = { index, group -> "${index}_${group.name}" }) { index, group ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .tvFocusRing(shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(
                        selected = selected,
                        onClick = { onSelectGroup(index) },
                        role = Role.Tab,
                    )
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Text(
                    text = group.name.ifBlank { stringResource(R.string.ungrouped_channels) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.channel_count, group.channels.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChannelList(
    channels: List<LiveChannel>,
    onPlayChannel: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.tvListFocus(rememberTvListFocusRequester()),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(items = channels, key = { channel -> channel.url }) { channel ->
            ChannelItem(
                channel = channel,
                isFirstItem = channel == channels.firstOrNull(),
                isLastItem = channel == channels.lastOrNull(),
                onClick = { onPlayChannel(channel) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChannelItem(
    channel: LiveChannel,
    isFirstItem: Boolean,
    isLastItem: Boolean,
    onClick: () -> Unit,
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
                if (channel.logoUrl != null) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(36.dp),
                    )
                } else {
                    Icon(
                        imageVector = NextIcons.Live,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        content = {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Icon(
                imageVector = NextIcons.Play,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = NextIcons.Priority,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.playlist_source_error),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.tvFocusRing(shape = RoundedCornerShape(20.dp)),
        ) {
            Text(stringResource(R.string.retry))
        }
        HorizontalDivider(modifier = Modifier.padding(top = 24.dp), color = Color.Transparent)
    }
}
