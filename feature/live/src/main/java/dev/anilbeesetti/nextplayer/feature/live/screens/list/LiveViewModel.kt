package dev.anilbeesetti.nextplayer.feature.live.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.LiveSourceRepository
import dev.anilbeesetti.nextplayer.core.model.LiveSource
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LiveUiState(
    val sources: List<LiveSource> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val repository: LiveSourceRepository,
) : ViewModel() {

    val uiState: StateFlow<LiveUiState> = repository.getSources()
        .map { LiveUiState(sources = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LiveUiState(),
        )

    fun deleteSource(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
