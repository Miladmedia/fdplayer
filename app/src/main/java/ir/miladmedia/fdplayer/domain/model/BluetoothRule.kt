package ir.miladmedia.fdplayer.domain.model

data class BluetoothRule(
    val id: Long = 0L,
    val deviceAddress: String?, // null means ALL Bluetooth devices
    val deviceName: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val weekdays: Set<Int>, // Calendar.SATURDAY (7), SUNDAY (1)..FRIDAY (6)
    val playlistId: Long?, // null means fallback to current queue
    val shuffle: Boolean = false,
    val latencySec: Int = 0,
    val enabled: Boolean = true
)
