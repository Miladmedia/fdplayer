package ir.miladmedia.fdplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bluetooth_rules")
data class BluetoothRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val deviceAddress: String?,
    val deviceName: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val weekdaysCsv: String,
    val playlistId: Long?,
    val shuffle: Boolean,
    val latencySec: Int,
    val status: String // Stored as String per spec ("ENABLED" / "DISABLED")
)
