package dev.anilbeesetti.nextplayer.feature.videopicker.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.anilbeesetti.nextplayer.core.data.playback.PlayableMedia
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.history.WatchHistoryScreenRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.history.WatchHistoryViewModel
import kotlinx.serialization.Serializable

@Serializable
data object WatchHistoryRoute : NavKey

fun NavBackStack<NavKey>.navigateToWatchHistory() {
    add(WatchHistoryRoute)
}

fun EntryProviderScope<NavKey>.watchHistoryEntry(
    onNavigateUp: () -> Unit,
    onResumeWatching: (media: PlayableMedia, mediaKey: String, title: String, workId: Long?) -> Unit,
) {
    entry<WatchHistoryRoute> {
        WatchHistoryScreenRoute(
            viewModel = hiltViewModel<WatchHistoryViewModel, WatchHistoryViewModel.Factory>(
                creationCallback = { factory ->
                    factory.create(
                        output = WatchHistoryViewModel.Output(
                            navigateUp = onNavigateUp,
                            resumeWatching = onResumeWatching,
                        ),
                    )
                },
            ),
        )
    }
}
