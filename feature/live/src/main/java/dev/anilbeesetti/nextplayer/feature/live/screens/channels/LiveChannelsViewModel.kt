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
import dev.anilbeesetti.nextplayer.core.domain.GetLiveChannelsUseCase
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

/**
 * A row of the group pane, holding the channels to list when it is picked.
 *
 * [name] is the group as the playlist wrote it, and is empty for channels a playlist left
 * ungrouped.
 */
data class LiveChannelGroup(
    val name: String,
    val channels: List<LiveChannel>,
)

data class LiveChannelsUiState(
    val sourceName: String = "",
    val groups: List<LiveChannelGroup> = emptyList(),
    val selectedGroupIndex: Int = 0,
    val isLoading: Boolean = true,
    /** Nothing could be read at all, so there is no list to show. */
    val loadFailed: Boolean = false,
    /** What the failure said, when it said anything worth repeating. */
    val errorMessage: String? = null,
    /** Title of what is on air, by channel name. Guides cover only a fraction of the channels. */
    val nowPlaying: Map<String, String> = emptyMap(),
) {
    val selectedChannels: List<LiveChannel>
        get() = groups.getOrNull(selectedGroupIndex)?.channels.orEmpty()

    val channelCount: Int get() = groups.sumOf { it.channels.size }
}

/**
 * The channels to browse, either those of one source or those every source offers between them.
 *
 * Both views show the same list of the same channels, told what is on air the same way, and differ
 * only in where the channels come from and how they are grouped.
 */
@HiltViewModel(assistedFactory = LiveChannelsViewModel.Factory::class)
class LiveChannelsViewModel @AssistedInject constructor(
    @Assisted private val sourceId: Long?,
    private val sourceRepository: LiveSourceRepository,
    private val channelRepository: LiveChannelRepository,
    private val getLiveChannels: GetLiveChannelsUseCase,
    private val epgRepository: EpgRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        /** [sourceId] is null to browse every configured source at once. */
        fun create(sourceId: Long?): LiveChannelsViewModel
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
        loadChannels(refresh = true)
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

    private fun loadChannels(refresh: Boolean = false) {
        uiStateInternal.update { it.copy(isLoading = true, loadFailed = false, errorMessage = null) }
        viewModelScope.launch {
            if (sourceId == null) loadEverySource(refresh) else loadOneSource(sourceId, refresh)
        }
    }

    private suspend fun loadEverySource(refresh: Boolean) {
        val result = getLiveChannels(refresh)
        uiStateInternal.update {
            it.copy(
                groups = playlistGroups(result.channels),
                selectedGroupIndex = 0,
                isLoading = false,
                loadFailed = result.channels.isEmpty() && result.sourceCount > 0,
            )
        }
    }

    private suspend fun loadOneSource(sourceId: Long, refresh: Boolean) {
        val source = sourceRepository.getSource(sourceId)
        if (source == null) {
            uiStateInternal.update {
                it.copy(isLoading = false, loadFailed = true, errorMessage = "Source not found")
            }
            return
        }

        channelRepository.getChannels(source.url, refresh)
            .onSuccess { channels ->
                uiStateInternal.update {
                    it.copy(
                        sourceName = source.name,
                        groups = playlistGroups(channels),
                        selectedGroupIndex = 0,
                        isLoading = false,
                        loadFailed = false,
                        errorMessage = null,
                    )
                }
            }
            .onFailure { error ->
                uiStateInternal.update {
                    it.copy(
                        sourceName = source.name,
                        isLoading = false,
                        loadFailed = true,
                        errorMessage = error.message,
                    )
                }
            }
    }

    private companion object {
        const val NOW_PLAYING_INTERVAL_MS = 60_000L
    }
}
