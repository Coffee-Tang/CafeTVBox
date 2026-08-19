package dev.anilbeesetti.nextplayer.feature.live.screens.channels

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.live.LiveChannels
import dev.anilbeesetti.nextplayer.core.data.repository.EpgRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveChannelRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveSourceRepository
import dev.anilbeesetti.nextplayer.core.domain.GetLiveChannelsUseCase
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.model.LiveProgramme
import dev.anilbeesetti.nextplayer.core.model.LiveSource
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
 * ungrouped. A row this app derived rather than read from a playlist has no name a playlist gave
 * it and carries [labelRes] instead, so that naming it needs no `Context` here.
 */
data class LiveChannelGroup(
    val name: String,
    val channels: List<LiveChannel>,
    @StringRes val labelRes: Int? = null,
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
    /** Sources that could not be read while others could, so the list is still worth showing. */
    val failedSourceCount: Int = 0,
    /** False only when no source is configured at all, which is a matter of asking for one. */
    val hasSources: Boolean = true,
    /** Title of what is on air, by channel name. Guides cover only a fraction of the channels. */
    val nowPlaying: Map<String, String> = emptyMap(),
) {
    val selectedChannels: List<LiveChannel>
        get() = groups.getOrNull(selectedGroupIndex)?.channels.orEmpty()
}

/**
 * The state to show once every source has been read.
 *
 * Reading gave nothing while channels are already shown means a start with no network over channels
 * kept from an earlier run. Those are of more use than an error page, so they stay and
 * [LiveChannelsUiState.failedSourceCount] says what happened; a reader who had picked a group keeps
 * it as long as the new list still has one there.
 */
internal fun LiveChannelsUiState.afterReading(read: LiveChannels): LiveChannelsUiState {
    val keepShown = read.channels.isEmpty() && groups.isNotEmpty()
    val shown = if (keepShown) groups else categoryGroups(read.channels)
    return copy(
        groups = shown,
        selectedGroupIndex = selectedGroupIndex.takeIf { it in shown.indices } ?: 0,
        isLoading = false,
        loadFailed = !keepShown && read.channels.isEmpty() && read.sourceCount > 0,
        failedSourceCount = read.failedSources.size,
        hasSources = read.sourceCount > 0,
    )
}

/**
 * The state to show once the one source being browsed has been read.
 *
 * Keeps what it gave in an earlier run when it cannot be reached now, for the reason the reading of
 * every source keeps it, and counts the source as failed so that the same notice says so.
 */
internal fun LiveChannelsUiState.afterReading(
    source: LiveSource,
    read: Result<List<LiveChannel>>,
): LiveChannelsUiState {
    val channels = read.getOrNull()
    val shown = channels?.let(::playlistGroups) ?: groups
    return copy(
        sourceName = source.name,
        groups = shown,
        selectedGroupIndex = selectedGroupIndex.takeIf { it in shown.indices } ?: 0,
        isLoading = false,
        loadFailed = channels == null && shown.isEmpty(),
        errorMessage = read.exceptionOrNull()?.message,
        failedSourceCount = if (channels == null && shown.isNotEmpty()) 1 else 0,
    )
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

    /**
     * Shows the channels the sources gave last time, while they are read again.
     *
     * Reading them spends seconds reaching hosts abroad, and a list that arrives that late reads as
     * this app being slow rather than the network being slow. Not done when a refresh was asked for,
     * which is a request to see what the sources say now and not what they said before.
     */
    private suspend fun showStoredChannels() {
        val stored = getLiveChannels.stored() ?: return
        uiStateInternal.update {
            it.copy(
                groups = categoryGroups(stored.channels),
                isLoading = false,
                hasSources = stored.sourceCount > 0,
            )
        }
    }

    /** As [showStoredChannels], for the one source being browsed rather than all of them. */
    private suspend fun showStoredChannels(source: LiveSource) {
        val stored = channelRepository.getStoredChannels(source.url) ?: return
        uiStateInternal.update {
            it.copy(sourceName = source.name, groups = playlistGroups(stored), isLoading = false)
        }
    }

    private suspend fun loadEverySource(refresh: Boolean) {
        if (!refresh) showStoredChannels()
        val read = getLiveChannels(refresh)
        uiStateInternal.update { state -> state.afterReading(read) }
    }

    private suspend fun loadOneSource(sourceId: Long, refresh: Boolean) {
        val source = sourceRepository.getSource(sourceId)
        if (source == null) {
            uiStateInternal.update {
                it.copy(isLoading = false, loadFailed = true, errorMessage = "Source not found")
            }
            return
        }

        if (!refresh) showStoredChannels(source)
        val read = channelRepository.getChannels(source.url, refresh)
        uiStateInternal.update { state -> state.afterReading(source, read) }
    }

    private companion object {
        const val NOW_PLAYING_INTERVAL_MS = 60_000L
    }
}
