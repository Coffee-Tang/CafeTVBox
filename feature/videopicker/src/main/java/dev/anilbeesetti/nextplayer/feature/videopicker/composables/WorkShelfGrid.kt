package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.anilbeesetti.nextplayer.core.model.LibraryWork
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

@Composable
fun WorkShelfGrid(
    works: List<LibraryWork>,
    onWorkClick: (LibraryWork) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
) {
    LazyVerticalGrid(
        // Fixed rather than adaptive, so a poster is exactly as wide as the cards in the rows above.
        columns = GridCells.FixedSize(HOME_CARD_WIDTH),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = HEADER_KEY, span = { GridItemSpan(maxLineSpan) }) {
            // The grid's own padding already sets the left edge; the row spacing below sets the gap.
            ListSectionTitle(
                text = stringResource(id = R.string.media_library),
                contentPadding = PaddingValues(top = 12.dp),
            )
        }
        itemsIndexed(works, key = { _, work -> work.id }) { index, work ->
            WorkPoster(
                work = work,
                onClick = { onWorkClick(work) },
                modifier = Modifier.thenIf(index == 0 && firstItemFocusRequester != null) {
                    focusRequester(firstItemFocusRequester!!)
                },
            )
        }
    }
}

@Composable
private fun WorkPoster(
    work: LibraryWork,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .tvFocusRing(shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        // Underneath the poster rather than instead of it: a poster arriving over the network is
        // missing while it loads too, and a card that is blank for ten seconds reads as broken.
        Icon(
            imageVector = NextIcons.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp),
        )
        if (work.posterUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(work.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = work.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Over the artwork rather than below it, so a row of posters is only as tall as the posters.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(48.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, TITLE_SCRIM))),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                text = work.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

/** Dark enough for white text to stay legible over a bright poster. */
private val TITLE_SCRIM = Color.Black.copy(alpha = 0.75f)

private const val HEADER_KEY = "media-library-header"
