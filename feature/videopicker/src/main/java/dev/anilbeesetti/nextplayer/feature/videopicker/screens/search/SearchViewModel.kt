package dev.anilbeesetti.nextplayer.feature.videopicker.screens.search

import android.net.Uri
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
import dev.anilbeesetti.nextplayer.core.data.repository.CatalogueRepository
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.repository.SearchHistoryRepository
import dev.anilbeesetti.nextplayer.core.domain.GetLiveChannelsUseCase
import dev.anilbeesetti.nextplayer.core.domain.SearchMediaUseCase
import dev.anilbeesetti.nextplayer.core.domain.SearchResults
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.LibraryWork
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.model.RecentMedium
import dev.anilbeesetti.nextplayer.core.model.search.rankedBy
import dev.anilbeesetti.nextplayer.core.ui.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SearchViewModel.Factory::class)
class SearchViewModel @AssistedInject constructor(
    private val searchMediaUseCase: SearchMediaUseCase,
    private val getLiveChannels: GetLiveChannelsUseCase,
    private val catalogueRepository: CatalogueRepository,
    private val mediaRepository: MediaRepository,
    private val playableMediaResolver: PlayableMediaResolver,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val systemService: SystemService,
    @Assisted private val output: Output,
) : ViewModel() {

    data class Output(
        val navigateUp: () -> Unit,
        val playVideo: (uri: Uri) -> Unit,
        val openFolder: (folderPath: String) -> Unit,
        val playWork: (media: PlayableMedia, mediaKey: String, title: String, workId: Long) -> Unit,
        val resumeWatching: (media: PlayableMedia, mediaKey: String, title: String, workId: Long?) -> Unit,
        val playChannel: (channel: LiveChannel) -> Unit,
    )

    @AssistedFactory
    interface Factory {
        fun create(output: Output): SearchViewModel
    }

    private val uiStateInternal = MutableStateFlow(SearchUiState())
    val uiState = uiStateInternal.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val works = MutableStateFlow<List<LibraryWork>>(emptyList())
    private val channels = MutableStateFlow<List<LiveChannel>>(emptyList())

    init {
        collectSearchHistory()
        collectRecentlyPlayed()
        collectPreferences()
        collectWorks()
        readChannels()
        collectSearchResults()
    }

    private fun collectSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.searchHistory.collect { history ->
                uiStateInternal.update { it.copy(searchHistory = history) }
            }
        }
    }

    /** What to offer before anything is typed: what was watched, which is what is watched again. */
    private fun collectRecentlyPlayed() {
        viewModelScope.launch {
            mediaRepository.observeRecentlyPlayed(limit = RECENT_SUGGESTIONS).collect { recent ->
                uiStateInternal.update { it.copy(recentlyPlayed = recent) }
            }
        }
    }

    private fun collectPreferences() {
        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect { prefs ->
                uiStateInternal.update { it.copy(preferences = prefs) }
            }
        }
    }

    private fun collectWorks() {
        viewModelScope.launch {
            catalogueRepository.observeWorks().collect(works::emit)
        }
    }

    /**
     * Channels are read once and searched in memory afterwards, since a list of them costs a read
     * of the stored playlists and the same list answers every keystroke.
     *
     * Nothing is stored until the live list has been opened once, and then the sources have to be
     * read over the network. That takes seconds, so it happens while the viewer types rather than
     * before: results are gathered from a flow, and the channels join them when they arrive.
     */
    private fun readChannels() {
        viewModelScope.launch {
            val read = getLiveChannels.stored() ?: getLiveChannels()
            channels.emit(read.channels)
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun collectSearchResults() {
        viewModelScope.launch {
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .flatMapLatest { query ->
                    combine(searchMediaUseCase(query), works, channels) { files, works, channels ->
                        Triple(
                            files,
                            works.rankedBy(query) { namesOf(it) },
                            channels.rankedBy(query) { namesOf(it) },
                        )
                    }
                }
                .collect { (files, works, channels) ->
                    uiStateInternal.update {
                        it.copy(
                            searchResults = files,
                            workResults = works,
                            channelResults = channels,
                            isSearching = false,
                        )
                    }
                }
        }
    }

    fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.OnQueryChange -> onQueryChange(event.query)
            is SearchUiEvent.OnSearch -> onSearch(event.query)
            is SearchUiEvent.OnHistoryItemClick -> onHistoryItemClick(event.query)
            is SearchUiEvent.OnRemoveHistoryItem -> removeHistoryItem(event.query)
            is SearchUiEvent.OnClearHistory -> clearHistory()
        }
    }

    fun onNavigateUp() = output.navigateUp()

    fun onPlayVideo(uri: Uri) = output.playVideo(uri)

    fun onOpenFolder(folderPath: String) = output.openFolder(folderPath)

    fun onPlayChannel(channel: LiveChannel) = output.playChannel(channel)

    /** Opens a work where it was left, which is what its poster does on the home screen. */
    fun onPlayWork(work: LibraryWork) {
        viewModelScope.launch {
            val mediaKey = catalogueRepository.keyToResume(work.id)
            val playable = mediaKey?.let { playableMediaResolver.resolve(it) }
            if (mediaKey == null || playable == null) {
                systemService.showToast(
                    text = systemService.getString(R.string.error_playback_source_unavailable),
                    duration = Toast.LENGTH_SHORT,
                )
                return@launch
            }
            output.playWork(playable, mediaKey, work.title, work.id)
        }
    }

    /** Reopens a played item, the way the history page does. */
    fun onResume(medium: RecentMedium) {
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

    private fun onQueryChange(query: String) {
        uiStateInternal.update { it.copy(query = query, isSearching = query.isNotBlank()) }
        searchQuery.value = query
    }

    private fun onSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            searchHistoryRepository.addSearchQuery(query)
        }
    }

    private fun onHistoryItemClick(query: String) {
        uiStateInternal.update { it.copy(query = query, isSearching = true) }
        searchQuery.value = query
        onSearch(query)
    }

    private fun removeHistoryItem(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeSearchQuery(query)
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearHistory()
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val RECENT_SUGGESTIONS = 10
    }
}

/**
 * A work answers to its title, to the title it is also known by — often the other language's — and
 * to its title with the year, since that is how someone tells two films of the same name apart.
 */
private fun namesOf(work: LibraryWork): List<String?> =
    listOf(work.title, work.otherTitle, work.year?.let { "${work.title} $it" })

/** A channel answers to its own name and to the group a playlist filed it under. */
private fun namesOf(channel: LiveChannel): List<String?> = listOf(channel.name, channel.group)

@Stable
data class SearchUiState(
    val query: String = "",
    val searchHistory: List<String> = emptyList(),
    val recentlyPlayed: List<RecentMedium> = emptyList(),
    val searchResults: SearchResults = SearchResults(),
    val workResults: List<LibraryWork> = emptyList(),
    val channelResults: List<LiveChannel> = emptyList(),
    val isSearching: Boolean = false,
    val preferences: ApplicationPreferences = ApplicationPreferences(),
) {

    val hasResults: Boolean
        get() = workResults.isNotEmpty() || channelResults.isNotEmpty() || !searchResults.isEmpty
}

sealed interface SearchUiEvent {
    data class OnQueryChange(val query: String) : SearchUiEvent
    data class OnSearch(val query: String) : SearchUiEvent
    data class OnHistoryItemClick(val query: String) : SearchUiEvent
    data class OnRemoveHistoryItem(val query: String) : SearchUiEvent
    data object OnClearHistory : SearchUiEvent
}
