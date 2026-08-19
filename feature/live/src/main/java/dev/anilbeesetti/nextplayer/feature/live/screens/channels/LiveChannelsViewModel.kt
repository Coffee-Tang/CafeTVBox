package dev.anilbeesetti.nextplayer.feature.live.screens.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.EpgRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveChannelRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveSourceRepository
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.model.LiveProgramme
import dev.anilbeesetti.nextplayer.core.model.channelKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
    /** Title of what is on air, by channel name. Guides cover only a fraction of the channels. */
    val nowPlaying: Map<String, String> = emptyMap(),
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
    private val epgRepository: EpgRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(sourceId: Long): LiveChannelsViewModel
    }

    private val uiStateInternal = MutableStateFlow(LiveChannelsUiState())
    val uiState: StateFlow<LiveChannelsUiState> = uiStateInternal.asStateFlow()

    private val guide = MutableStateFlow<Map<String, List<LiveProgramme>>>(emptyMap())

    init {
        loadChannels()
        loadGuide()
        followNowPlaying()
    }

    fun refresh() {
        loadChannels()
        loadGuide()
    }

    private fun loadGuide() {
        viewModelScope.launch {
            epgRepository.getGuide().onSuccess { guide.value = it }
        }
    }

    /**
     * Restates what is on air whenever the channels or the guide arrive, and then as programmes
     * end. A guide covers a whole day, so only the reading of it repeats.
     */
    private fun followNowPlaying() {
        viewModelScope.launch {
            combine(
                uiState.map { it.groups }.distinctUntilChanged(),
                guide,
                everyMinute(),
            ) { groups, guide, _ -> onAir(groups, guide) }
                .collect { onAir -> uiStateInternal.update { it.copy(nowPlaying = onAir) } }
        }
    }

    /**
     * What is on air now, by channel name, for the channels the guide happens to list. Names are
     * matched through [channelKey], as a playlist and a guide rarely spell a station the same way.
     */
    private fun onAir(
        groups: List<LiveChannelGroup>,
        guide: Map<String, List<LiveProgramme>>,
    ): Map<String, String> {
        if (guide.isEmpty()) return emptyMap()
        val now = System.currentTimeMillis()
        return groups.asSequence()
            .flatMap { it.channels }
            .mapNotNull { channel ->
                guide[channelKey(channel.name)]
                    ?.firstOrNull { it.isOnAt(now) }
                    ?.let { programme -> channel.name to programme.title }
            }
            .toMap()
    }

    private fun everyMinute(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(NOW_PLAYING_INTERVAL_MS)
        }
    }

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

    private companion object {
        const val NOW_PLAYING_INTERVAL_MS = 60_000L
    }
}
