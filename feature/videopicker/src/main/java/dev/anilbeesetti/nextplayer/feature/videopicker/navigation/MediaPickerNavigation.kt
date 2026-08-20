package dev.anilbeesetti.nextplayer.feature.videopicker.navigation

import android.net.Uri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.anilbeesetti.nextplayer.core.data.playback.PlayableMedia
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.MediaPickerRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.MediaPickerViewModel
import kotlinx.serialization.Serializable

@Serializable
data class MediaPickerRoute(
    val folderId: String? = null,
) : NavKey

fun NavBackStack<NavKey>.navigateToMediaPickerScreen(folderId: String) {
    add(MediaPickerRoute(folderId))
}

fun EntryProviderScope<NavKey>.mediaPickerEntry(
    onNavigateUp: () -> Unit,
    onPlayVideo: (uri: Uri) -> Unit,
    onPlayVideos: (uris: List<Uri>) -> Unit,
    onResumeWatching: (media: PlayableMedia, mediaKey: String, title: String, workId: Long?) -> Unit,
    onWatchHistoryClick: () -> Unit,
    onFolderClick: (folderPath: String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onVaultClick: () -> Unit,
    onOpenNetwork: () -> Unit,
    onPlayWork: (media: PlayableMedia, mediaKey: String, title: String, workId: Long) -> Unit,
) {
    entry<MediaPickerRoute> { key ->
        MediaPickerRoute(
            viewModel = hiltViewModel<MediaPickerViewModel, MediaPickerViewModel.Factory>(
                creationCallback = { factory ->
                    factory.create(
                        input = MediaPickerViewModel.Input(
                            folderId = key.folderId,
                        ),
                        output = MediaPickerViewModel.Output(
                            navigateUp = onNavigateUp,
                            playVideo = onPlayVideo,
                            playVideos = onPlayVideos,
                            resumeWatching = onResumeWatching,
                            openWatchHistory = onWatchHistoryClick,
                            openFolder = onFolderClick,
                            openSettings = onSettingsClick,
                            openSearch = onSearchClick,
                            openVault = onVaultClick,
                            openNetwork = onOpenNetwork,
                            playWork = onPlayWork,
                        ),
                    )
                },
            ),
        )
    }
}
