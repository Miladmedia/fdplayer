package ir.miladmedia.fdplayer.ui.player

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import ir.miladmedia.fdplayer.service.audio.AudioLevels
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

/** Thin ViewModel over the shared [PlayerConnection]; transport actions are delegated. */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val connection: PlayerConnection,
    private val settings: SettingsRepository
) : ViewModel() {

    val state: StateFlow<PlayerUiState> = connection.state
    val bands: StateFlow<FloatArray> = AudioLevels.bands

    val driving: StateFlow<Boolean> = settings.settings
        .map { it.keepScreenOnDriving }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleDriving() {
        viewModelScope.launch { settings.update { it.copy(keepScreenOnDriving = !it.keepScreenOnDriving) } }
    }

    fun togglePlay() = connection.togglePlay()
    fun next() = connection.next()
    fun previous() = connection.previous()
    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)
    fun toggleShuffle() = connection.toggleShuffle()
    fun cycleRepeat() = connection.cycleRepeat()
}
