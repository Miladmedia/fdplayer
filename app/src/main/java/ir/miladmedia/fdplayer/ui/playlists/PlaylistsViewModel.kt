package ir.miladmedia.fdplayer.ui.playlists

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.miladmedia.fdplayer.data.preferences.TipsPreferences
import ir.miladmedia.fdplayer.domain.model.BluetoothRule
import ir.miladmedia.fdplayer.domain.model.Playlist
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.domain.repository.AudioRepository
import ir.miladmedia.fdplayer.domain.repository.BluetoothRuleRepository
import ir.miladmedia.fdplayer.domain.repository.PlaybackController
import ir.miladmedia.fdplayer.domain.repository.PlaylistRepository
import ir.miladmedia.fdplayer.service.BluetoothAutoplayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairedBtDevice(
    val address: String,
    val name: String
)

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val allTracks: List<Track> = emptyList(),
    val rules: List<BluetoothRule> = emptyList(),
    val pairedDevices: List<PairedBtDevice> = emptyList(),
    val hasBluetoothPermission: Boolean = false,
    val showFirstTimeTip: Boolean = false
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository,
    private val audioRepository: AudioRepository,
    private val bluetoothRuleRepository: BluetoothRuleRepository,
    private val playbackController: PlaybackController,
    private val tipsPreferences: TipsPreferences
) : ViewModel() {

    private val pairedDevices = MutableStateFlow<List<PairedBtDevice>>(emptyList())
    private val hasBtPermission = MutableStateFlow(
        BluetoothAutoplayService.hasBluetoothConnectPermission(context)
    )
    private val showTip = MutableStateFlow(!tipsPreferences.isTipDismissed("playlists"))

    val uiState: StateFlow<PlaylistsUiState> = combine(
        playlistRepository.observePlaylists(),
        audioRepository.observeTracks(),
        bluetoothRuleRepository.observeRules(),
        pairedDevices,
        hasBtPermission,
        showTip
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        PlaylistsUiState(
            playlists = flows[0] as List<Playlist>,
            allTracks = flows[1] as List<Track>,
            rules = flows[2] as List<BluetoothRule>,
            pairedDevices = flows[3] as List<PairedBtDevice>,
            hasBluetoothPermission = flows[4] as Boolean,
            showFirstTimeTip = flows[5] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistsUiState()
    )

    init {
        refreshPairedDevices()
    }

    fun onBluetoothPermissionUpdated(granted: Boolean) {
        hasBtPermission.value = granted
        if (granted) {
            refreshPairedDevices()
            BluetoothAutoplayService.startIfPermitted(context)
        }
    }

    fun refreshPairedDevices() {
        if (!BluetoothAutoplayService.hasBluetoothConnectPermission(context)) {
            hasBtPermission.value = false
            pairedDevices.value = emptyList()
            return
        }
        hasBtPermission.value = true
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bonded = manager?.adapter?.bondedDevices.orEmpty()
            pairedDevices.value = bonded.map { device ->
                PairedBtDevice(
                    address = device.address,
                    name = device.name?.takeIf { it.isNotBlank() } ?: device.address
                )
            }
        } catch (_: SecurityException) {
            pairedDevices.value = emptyList()
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistRepository.createPlaylist(name.trim())
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(id)
        }
    }

    fun playPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            val tracks = audioRepository.getTracksByIds(playlist.trackIds)
            if (tracks.isNotEmpty()) {
                playbackController.playTracks(tracks, startIndex = 0, shuffle = false)
            }
        }
    }

    fun saveBluetoothRule(rule: BluetoothRule) {
        viewModelScope.launch {
            bluetoothRuleRepository.saveRule(rule)
            BluetoothAutoplayService.startIfPermitted(context)
        }
    }

    fun toggleBluetoothRule(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            bluetoothRuleRepository.toggleRule(id, enabled)
            if (enabled) {
                BluetoothAutoplayService.startIfPermitted(context)
            }
        }
    }

    fun deleteBluetoothRule(id: Long) {
        viewModelScope.launch {
            bluetoothRuleRepository.deleteRule(id)
        }
    }

    fun dismissTip() {
        tipsPreferences.dismissTip("playlists")
        showTip.value = false
    }
}
