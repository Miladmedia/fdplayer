package ir.miladmedia.fdplayer.data.mapper

import ir.miladmedia.fdplayer.data.local.BtRuleEntity
import ir.miladmedia.fdplayer.data.local.BtScheduleEntity
import ir.miladmedia.fdplayer.data.local.DownloadEntity
import ir.miladmedia.fdplayer.data.local.PlaylistRow
import ir.miladmedia.fdplayer.data.local.SourceEntity
import ir.miladmedia.fdplayer.data.local.TrackEntity
import ir.miladmedia.fdplayer.domain.model.BtRule
import ir.miladmedia.fdplayer.domain.model.BtSchedule
import ir.miladmedia.fdplayer.domain.model.DownloadItem
import ir.miladmedia.fdplayer.domain.model.DownloadStatus
import ir.miladmedia.fdplayer.domain.model.Playlist
import ir.miladmedia.fdplayer.domain.model.Source
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.domain.model.TrackSource

fun TrackEntity.toDomain() = Track(
    id = id, uri = uri, title = title, artist = artist, durationMs = durationMs,
    source = runCatching { TrackSource.valueOf(source) }.getOrDefault(TrackSource.DEVICE),
    missing = missing
)

fun PlaylistRow.toDomain() = Playlist(id, name, trackCount)

fun DownloadEntity.toDomain() = DownloadItem(
    id = id, url = url, title = title, artist = artist, keyword = keyword,
    status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED),
    downloadedBytes = downloadedBytes, totalBytes = totalBytes, error = error,
    fileUri = fileUri, createdAt = createdAt
)

fun SourceEntity.toDomain() = Source(id, name, url)

fun BtRuleEntity.toDomain() = BtRule(address, name, playlistId, shuffle, latencySec)

fun BtRule.toEntity() = BtRuleEntity(address, name, playlistId, shuffle, latencySec)

fun BtScheduleEntity.toDomain() = BtSchedule(id, address, weekday, startMinute, endMinute)

fun BtSchedule.toEntity() = BtScheduleEntity(0, address, weekday, startMinute, endMinute)
