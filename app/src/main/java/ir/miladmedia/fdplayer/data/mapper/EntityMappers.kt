package ir.miladmedia.fdplayer.data.mapper

import ir.miladmedia.fdplayer.data.local.entity.BluetoothRuleEntity
import ir.miladmedia.fdplayer.data.local.entity.PlaylistEntity
import ir.miladmedia.fdplayer.domain.model.BluetoothRule
import ir.miladmedia.fdplayer.domain.model.Playlist

private const val STATUS_ENABLED = "ENABLED"
private const val STATUS_DISABLED = "DISABLED"

fun PlaylistEntity.toDomain(): Playlist {
    val ids = trackIdsCsv
        .split(",")
        .mapNotNull { it.trim().toLongOrNull() }
    return Playlist(
        id = id,
        name = name,
        trackIds = ids,
        createdAt = createdAt
    )
}

fun Playlist.toEntity(): PlaylistEntity {
    return PlaylistEntity(
        id = id,
        name = name,
        trackIdsCsv = trackIds.joinToString(","),
        createdAt = createdAt
    )
}

fun BluetoothRuleEntity.toDomain(): BluetoothRule {
    val days = weekdaysCsv
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .toSet()
    return BluetoothRule(
        id = id,
        deviceAddress = deviceAddress,
        deviceName = deviceName,
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        weekdays = days,
        playlistId = playlistId,
        shuffle = shuffle,
        latencySec = latencySec,
        enabled = status == STATUS_ENABLED
    )
}

fun BluetoothRule.toEntity(): BluetoothRuleEntity {
    return BluetoothRuleEntity(
        id = id,
        deviceAddress = deviceAddress,
        deviceName = deviceName,
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        weekdaysCsv = weekdays.sorted().joinToString(","),
        playlistId = playlistId,
        shuffle = shuffle,
        latencySec = latencySec,
        status = if (enabled) STATUS_ENABLED else STATUS_DISABLED
    )
}
