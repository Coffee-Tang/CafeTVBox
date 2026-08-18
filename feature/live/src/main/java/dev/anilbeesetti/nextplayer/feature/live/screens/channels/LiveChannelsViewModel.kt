package dev.anilbeesetti.nextplayer.feature.live.screens.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.LiveChannelRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveSourceRepository
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveChannelGroup(
    val name: String,
    val channels: List<LiveChannel>,
)

data class LiveChannelsUiState(
    val sourceName: String = "",
    val groups: List<LiveChannelGroup> = emptyList(),
    val selectedGroupIndex: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val selectedChannels: List<LiveChannel>
        get() = groups.getOrNull(selectedGroupIndex)?.channels.orEmpty()

    val channelCount: Int get() = groups.sumOf { it.channels.size }
}

@HiltViewModel(assistedFactory = LiveChannelsViewModel.Factory::class)
class LiveChannelsViewModel @AssistedInject constructor(
    @Assisted private val sourceId: Long,
    private val sourceRepository: LiveSourceRepository,
    private val channelRepository: LiveChannelRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(sourceId: Long): LiveChannelsViewModel
    }

    private val uiStateInternal = MutableStateFlow(LiveChannelsUiState())
    val uiState: StateFlow<LiveChannelsUiState> = uiStateInternal.asStateFlow()

    init {
        loadChannels()
    }

    fun refresh() = loadChannels()

    fun selectGroup(index: Int) {
        uiStateInternal.update { state ->
            if (index in state.groups.indices) state.copy(selectedGroupIndex = index) else state
        }
    }

    private fun loadChannels() {
        uiStateInternal.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val source = sourceRepository.getSource(sourceId)
            if (source == null) {
                uiStateInternal.update {
                    it.copy(isLoading = false, errorMessage = "Source not found")
                }
                return@launch
            }

            channelRepository.getChannels(source.url)
                .onSuccess { channels ->
                    uiStateInternal.update {
                        it.copy(
                            sourceName = source.name,
                            groups = channels.toGroups(),
                            selectedGroupIndex = 0,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    uiStateInternal.update {
                        it.copy(
                            sourceName = source.name,
                            isLoading = false,
                            errorMessage = error.message,
                        )
                    }
                }
        }
    }

    /** Groups channels by `group-title`, keeping the order in which groups appear in the playlist. */
    private fun List<LiveChannel>.toGroups(): List<LiveChannelGroup> =
        groupBy { it.group }
            .map { (group, channels) -> LiveChannelGroup(name = group, channels = channels) }
}
