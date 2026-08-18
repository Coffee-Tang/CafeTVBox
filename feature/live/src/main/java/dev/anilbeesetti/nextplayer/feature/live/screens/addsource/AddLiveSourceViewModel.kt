package dev.anilbeesetti.nextplayer.feature.live.screens.addsource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.LiveChannelRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveSourceRepository
import dev.anilbeesetti.nextplayer.core.model.LiveSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface SaveState {
    data object Idle : SaveState
    data object Verifying : SaveState
    data class Error(val message: String?) : SaveState
}

@HiltViewModel(assistedFactory = AddLiveSourceViewModel.Factory::class)
class AddLiveSourceViewModel @AssistedInject constructor(
    @Assisted private val sourceId: Long?,
    private val sourceRepository: LiveSourceRepository,
    private val channelRepository: LiveChannelRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(sourceId: Long?): AddLiveSourceViewModel
    }

    val isEdit: Boolean = sourceId != null

    private val _existingSource = MutableStateFlow<LiveSource?>(null)
    val existingSource: StateFlow<LiveSource?> = _existingSource.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _savedEvents = Channel<Unit>(Channel.BUFFERED)
    val savedEvents = _savedEvents.receiveAsFlow()

    init {
        if (sourceId != null) {
            viewModelScope.launch { _existingSource.value = sourceRepository.getSource(sourceId) }
        }
    }

    /** Verifies that [url] yields channels before persisting the source. */
    fun verifyAndSave(name: String, url: String) {
        if (_saveState.value == SaveState.Verifying) return
        _saveState.value = SaveState.Verifying
        viewModelScope.launch {
            channelRepository.getChannels(url.trim())
                .onSuccess {
                    val source = LiveSource(
                        id = sourceId ?: 0,
                        name = name.trim(),
                        url = url.trim(),
                        createdAt = _existingSource.value?.createdAt ?: System.currentTimeMillis(),
                    )
                    sourceRepository.upsert(source)
                    _saveState.value = SaveState.Idle
                    _savedEvents.send(Unit)
                }
                .onFailure { _saveState.value = SaveState.Error(it.message) }
        }
    }

    fun clearError() {
        if (_saveState.value is SaveState.Error) _saveState.value = SaveState.Idle
    }
}
