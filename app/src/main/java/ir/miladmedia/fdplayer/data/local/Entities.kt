package ir.miladmedia.fdplayer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tracks", indices = [Index(value = ["uri"], unique = true)])
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val source: String,
    val missing: Boolean = false
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(PlaylistEntity::class, ["id"], ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TrackEntity::class, ["id"], ["trackId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("trackId")]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val addedAt: Long
)

@Entity(
    tableName = "downloads",
    indices = [Index("url", unique = true), Index("keyword"), Index("sha256")]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val artist: String,
    val keyword: String,
    val nameKey: String,
    val titleKey: String,
    val status: String,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val error: String? = null,
    val fileUri: String? = null,
    val sha256: String? = null,
    val createdAt: Long
)

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String
)

@Entity(tableName = "bt_rules")
data class BtRuleEntity(
    @PrimaryKey val address: String,
    val name: String,
    val playlistId: Long?,
    val shuffle: Boolean,
    val latencySec: Int
)

@Entity(tableName = "bt_schedules", indices = [Index("address")])
data class BtScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val weekday: Int,
    val startMinute: Int,
    val endMinute: Int
)
