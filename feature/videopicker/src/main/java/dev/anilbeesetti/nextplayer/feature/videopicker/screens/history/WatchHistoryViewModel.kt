package dev.anilbeesetti.nextplayer.feature.videopicker.screens.history

import android.widget.Toast
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.common.service.system.SystemService
import dev.anilbeesetti.nextplayer.core.data.playback.PlayableMedia
import dev.anilbeesetti.nextplayer.core.data.playback.PlayableMediaResolver
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.model.RecentMedium
import dev.anilbeesetti.nextplayer.core.ui.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = WatchHistoryViewModel.Factory::class)
class WatchHistoryViewModel @AssistedInject constructor(
    private val mediaRepository: MediaRepository,
    private val playableMediaResolver: PlayableMediaResolver,
    private val systemService: SystemService,
    @Assisted private val output: Output,
) : ViewModel() {

    data class Output(
        val navigateUp: () -> Unit,
        val resumeWatching: (media: PlayableMedia, mediaKey: String, title: String, workId: Long?) -> Unit,
    )

    @AssistedFactory
    interface Factory {
        fun create(output: Output): WatchHistoryViewModel
    }

    private val uiStateInternal = MutableStateFlow(WatchHistoryUiState())
    val uiState = uiStateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            mediaRepository.observeRecentlyPlayed().collect { items ->
                uiStateInternal.update { it.copy(items = items, isLoading = false) }
            }
        }
    }

    fun onNavigateUp() = output.navigateUp()

    /**
     * Reopens an item. A file on a share needs a fresh proxy URL, which cannot be had once the
     * server is no longer configured; the entry stays, since the share may well come back.
     */
    fun resume(medium: RecentMedium) {
        viewModelScope.launch {
            val playable = playableMediaResolver.resolve(medium.mediaKey)
            if (playable == null) {
                systemService.showToast(
                    text = systemService.getString(R.string.error_playback_source_unavailable),
                    duration = Toast.LENGTH_SHORT,
                )
                return@launch
            }
            output.resumeWatching(playable, medium.mediaKey, medium.title, medium.workId)
        }
    }

    fun remove(medium: RecentMedium) {
        viewModelScope.launch { mediaRepository.removeFromRecentlyPlayed(medium.mediaKey) }
    }

    fun clearAll() {
        viewModelScope.launch { mediaRepository.clearRecentlyPlayed() }
    }
}

@Stable
data class WatchHistoryUiState(
    val items: List<RecentMedium> = emptyList(),
    val isLoading: Boolean = true,
)
