package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.anilbeesetti.nextplayer.core.model.RecentMedium
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme

/**
 * The items the user watched most recently, newest first, for picking up where they left off.
 *
 * @param firstItemFocusRequester Set on the first card so a TV remote can reach the row from above.
 * @param downFocusRequester Where pressing down from a card leads, when there is a list below.
 */
@Composable
fun ContinueWatchingRow(
    items: List<RecentMedium>,
    onItemClick: (RecentMedium) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    firstItemFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListSectionTitle(
                text = stringResource(id = R.string.continue_watching),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, bottom = 8.dp),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSeeAllClick, modifier = Modifier.tvFocusRing()) {
                Text(text = stringResource(id = R.string.see_all))
            }
        }
        LazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items = items, key = { _, item -> item.mediaKey }) { index, item ->
                ContinueWatchingCard(
                    medium = item,
                    onClick = { onItemClick(item) },
                    modifier = Modifier
                        .thenIf(index == 0 && firstItemFocusRequester != null) {
                            focusRequester(firstItemFocusRequester!!)
                        }
                        .thenIf(downFocusRequester != null) {
                            focusProperties { down = downFocusRequester!! }
                        },
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    medium: RecentMedium,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(CARD_WIDTH)
            .tvFocusRing(shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f),
        ) {
            MediumArtwork(medium = medium)
            // A stream is watched as it arrives, so there is no whole for progress to be part of.
            medium.progress.takeIf { medium.source != RecentMedium.Source.STREAM }?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    trackColor = Color.Transparent,
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = medium.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(id = medium.source.labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A frame from the video, which only the device's own library can produce. Anything reached over the
 * network would have to be streamed for a frame, so those show what kind of source they came from.
 */
@Composable
private fun MediumArtwork(medium: RecentMedium) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        when (medium.source) {
            RecentMedium.Source.LOCAL -> AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(medium.mediaKey)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            RecentMedium.Source.SHARE -> Icon(
                imageVector = NextIcons.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )

            RecentMedium.Source.STREAM -> Icon(
                imageVector = NextIcons.Live,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

private val RecentMedium.Source.labelRes: Int
    get() = when (this) {
        RecentMedium.Source.LOCAL -> R.string.media_source_local
        RecentMedium.Source.SHARE -> R.string.media_source_share
        RecentMedium.Source.STREAM -> R.string.media_source_stream
    }

private val CARD_WIDTH = 168.dp

@Preview
@Composable
private fun ContinueWatchingRowPreview() {
    NextPlayerTheme {
        ContinueWatchingRow(
            onSeeAllClick = {},
            items = listOf(
                RecentMedium(
                    mediaKey = "cafeplayer-network://1/Shows/Episode.One.mkv",
                    title = "Episode.One.1080p.WEB-DL.mkv",
                    source = RecentMedium.Source.SHARE,
                    positionMs = 600_000,
                    durationMs = 1_800_000,
                    lastPlayedTime = 1,
                ),
                RecentMedium(
                    mediaKey = "http://example.com/live.m3u8",
                    title = "CCTV-1",
                    source = RecentMedium.Source.STREAM,
                    positionMs = -1,
                    durationMs = null,
                    lastPlayedTime = 2,
                ),
            ),
            onItemClick = {},
        )
    }
}
