package dev.anilbeesetti.nextplayer.feature.videopicker.navigation

import android.net.Uri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.anilbeesetti.nextplayer.core.data.playback.PlayableMedia
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.search.SearchRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.search.SearchViewModel
import kotlinx.serialization.Serializable

@Serializable
object SearchRoute : NavKey

fun NavBackStack<NavKey>.navigateToSearch() {
    add(SearchRoute)
}

fun EntryProviderScope<NavKey>.searchEntry(
    onNavigateUp: () -> Unit,
    onPlayVideo: (uri: Uri) -> Unit,
    onFolderClick: (folderPath: String) -> Unit,
    onPlayWork: (media: PlayableMedia, mediaKey: String, title: String, workId: Long) -> Unit,
    onResumeWatching: (media: PlayableMedia, mediaKey: String, title: String, workId: Long?) -> Unit,
    onPlayChannel: (channel: LiveChannel) -> Unit,
) {
    entry<SearchRoute> {
        SearchRoute(
            viewModel = hiltViewModel<SearchViewModel, SearchViewModel.Factory>(
                creationCallback = { factory ->
                    factory.create(
                        output = SearchViewModel.Output(
                            navigateUp = onNavigateUp,
                            playVideo = onPlayVideo,
                            openFolder = onFolderClick,
                            playWork = onPlayWork,
                            resumeWatching = onResumeWatching,
                            playChannel = onPlayChannel,
                        ),
                    )
                },
            ),
        )
    }
}
