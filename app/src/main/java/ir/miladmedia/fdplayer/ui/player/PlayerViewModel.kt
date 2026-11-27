package ir.miladmedia.fdplayer.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.miladmedia.fdplayer.data.preferences.TipsPreferences
import ir.miladmedia.fdplayer.domain.model.PlaybackState
import ir.miladmedia.fdplayer.domain.repository.PlaybackController
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import ir.miladmedia.fdplayer.service.MicAmplitudeMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PlayerUiState(
    val playback: PlaybackState = PlaybackState(),
    val isDrivingMode: Boolean = false,
    val micRhythmConsentGranted: Boolean = false,
    val micRhythmEnabled: Boolean = false,
    val showMicConsentDialog: Boolean = false,
    val showFirstTimeTip: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackController: PlaybackController,
    private val settingsRepository: SettingsRepository,
    private val tipsPreferences: TipsPreferences
) : ViewModel() {

    private val _isDrivingMode = MutableStateFlow(false)
    private val _showMicConsentDialog = MutableStateFlow(false)
    private val _showFirstTimeTip = MutableStateFlow(!tipsPreferences.isTipDismissed("player"))

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val micAmplitude: StateFlow<Float> = settingsRepository.observePreferences()
        .map { it.micRhythmEnabled && it.micRhythmConsentGranted }
        .distinctUntilChanged()
        .flatMapLatest { enabled ->
            if (enabled) {
                MicAmplitudeMonitor.observeNormalizedAmplitude(context)
            } else {
                flowOf(0f)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init {
        viewModelScope.launch {
            playbackController.playbackState.collect { playback ->
                _uiState.value = _uiState.value.copy(playback = playback)
            }
        }
        viewModelScope.launch {
            settingsRepository.observePreferences().collect { prefs ->
                _uiState.value = _uiState.value.copy(
                    micRhythmConsentGranted = prefs.micRhythmConsentGranted,
                    micRhythmEnabled = prefs.micRhythmEnabled
                )
            }
        }
        viewModelScope.launch {
            _isDrivingMode.collect { driving ->
                _uiState.value = _uiState.value.copy(isDrivingMode = driving)
            }
        }
        viewModelScope.launch {
            _showMicConsentDialog.collect { show ->
                _uiState.value = _uiState.value.copy(showMicConsentDialog = show)
            }
        }
        viewModelScope.launch {
            _showFirstTimeTip.collect { show ->
                _uiState.value = _uiState.value.copy(showFirstTimeTip = show)
            }
        }

        // Extract dominant colors whenever currentTrack changes
        viewModelScope.launch {
            playbackController.playbackState
                .map { it.currentTrack?.albumArtUri }
                .distinctUntilChanged()
                .collect { artUri ->
                    val colors = extractPaletteColors(artUri)
                    playbackController.updateDominantColors(colors)
                }
        }
    }

    fun toggleDrivingMode() {
        _isDrivingMode.value = !_isDrivingMode.value
    }

    fun onMicButtonClicked(hasRecordPermission: Boolean, onRequestPermission: () -> Unit) {
        val state = _uiState.value
        if (state.micRhythmEnabled) {
            viewModelScope.launch {
                settingsRepository.setMicRhythmEnabled(false)
            }
            return
        }
        if (!state.micRhythmConsentGranted) {
            _showMicConsentDialog.value = true
            return
        }
        if (hasRecordPermission) {
            viewModelScope.launch {
                settingsRepository.setMicRhythmEnabled(true)
            }
        } else {
            onRequestPermission()
        }
    }

    fun onMicConsentAccepted(hasRecordPermission: Boolean, onRequestPermission: () -> Unit) {
        _showMicConsentDialog.value = false
        viewModelScope.launch {
            if (hasRecordPermission) {
                settingsRepository.setMicRhythmConsent(granted = true, enabled = true)
            } else {
                settingsRepository.setMicRhythmConsent(granted = true, enabled = false)
                onRequestPermission()
            }
        }
    }

    fun onMicConsentDismissed() {
        _showMicConsentDialog.value = false
    }

    fun onRecordPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMicRhythmConsent(granted = granted, enabled = granted)
        }
    }

    fun dismissFirstTimeTip() {
        tipsPreferences.dismissTip("player")
        _showFirstTimeTip.value = false
    }

    fun togglePlayPause() = playbackController.togglePlayPause()
    fun skipNext() = playbackController.skipNext()
    fun skipPrevious() = playbackController.skipPrevious()
    fun toggleShuffle() = playbackController.toggleShuffle()
    fun cycleRepeatMode() = playbackController.cycleRepeatMode()
    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)

    @Suppress("DEPRECATION")
    private suspend fun extractPaletteColors(artUriString: String?): List<Int> =
        withContext(Dispatchers.IO) {
            if (artUriString.isNullOrBlank()) return@withContext emptyList()
            try {
                val uri = Uri.parse(artUriString)
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                        decoder.setTargetSize(128, 128)
                    }
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val palette = Palette.from(bitmap).generate()
                listOfNotNull(
                    palette.vibrantSwatch?.rgb,
                    palette.lightVibrantSwatch?.rgb,
                    palette.dominantSwatch?.rgb,
                    palette.mutedSwatch?.rgb
                ).distinct()
            } catch (_: Exception) {
                emptyList()
            }
        }
}
