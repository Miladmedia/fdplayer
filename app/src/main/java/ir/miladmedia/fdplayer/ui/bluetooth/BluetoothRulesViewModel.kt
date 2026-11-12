package ir.miladmedia.fdplayer.ui.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.miladmedia.fdplayer.domain.model.BondedDevice
import ir.miladmedia.fdplayer.domain.model.BtRule
import ir.miladmedia.fdplayer.domain.model.BtSchedule
import ir.miladmedia.fdplayer.domain.model.Playlist
import ir.miladmedia.fdplayer.domain.repository.BluetoothRepository
import ir.miladmedia.fdplayer.domain.repository.PlaylistRepository
import ir.miladmedia.fdplayer.domain.util.TextNormalizer
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BluetoothRulesViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    val rules: StateFlow<List<BtRule>> = bluetoothRepository.observeRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _devices = MutableStateFlow<List<BondedDevice>>(emptyList())
    val devices: StateFlow<List<BondedDevice>> = _devices.asStateFlow()

    init {
        refreshDevices()
    }

    /** Re-reads paired devices. Call after the Bluetooth permission is granted. */
    fun refreshDevices() {
        _devices.value = bluetoothRepository.bondedDevices()
    }

    fun schedules(address: String): Flow<List<BtSchedule>> = bluetoothRepository.observeSchedules(address)

    /** Enabling creates a default rule; disabling removes the rule for that device. */
    fun setEnabled(device: BondedDevice, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                if (bluetoothRepository.getRule(device.address) == null) {
                    bluetoothRepository.saveRule(
                        BtRule(device.address, device.name, null, false, 0)
                    )
                }
            } else {
                bluetoothRepository.deleteRule(device.address)
            }
        }
    }

    fun saveRule(rule: BtRule) {
        viewModelScope.launch {
            bluetoothRepository.saveRule(
                rule.copy(latencySec = rule.latencySec.coerceIn(0, 600))
            )
        }
    }

    /** Returns false when the time values are invalid or no weekday is selected. */
    fun addSchedule(address: String, days: Set<Int>, startText: String, endText: String): Boolean {
        val start = parseMinute(startText) ?: return false
        val end = parseMinute(endText) ?: return false
        if (start == end) return false
        val picked = days.filter { it in 1..7 }
        if (picked.isEmpty()) return false
        viewModelScope.launch {
            picked.forEach { weekday ->
                bluetoothRepository.addSchedule(BtSchedule(0L, address, weekday, start, end))
            }
        }
        return true
    }

    fun removeSchedule(id: Long) {
        viewModelScope.launch { bluetoothRepository.removeSchedule(id) }
    }

    companion object {
        /** Parses "H:MM" or "HH:MM" (Persian/Arabic digits allowed) into minutes since midnight. */
        fun parseMinute(text: String): Int? {
            val match = Regex("""^(\d{1,2}):(\d{2})$""").find(TextNormalizer.normalize(text).trim())
                ?: return null
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            if (hour !in 0..23 || minute !in 0..59) return null
            return hour * 60 + minute
        }
    }
}
