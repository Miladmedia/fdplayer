package ir.miladmedia.fdplayer.domain.model

enum class TrackSource { DOWNLOAD, DEVICE }

data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val source: TrackSource,
    val missing: Boolean
)

data class Playlist(val id: Long, val name: String, val trackCount: Int)

enum class DownloadStatus { QUEUED, DOWNLOADING, DONE, FAILED, DUPLICATE, CANCELED }

data class DownloadItem(
    val id: Long,
    val url: String,
    val title: String,
    val artist: String,
    val keyword: String,
    val status: DownloadStatus,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val error: String?,
    val fileUri: String?,
    val createdAt: Long
)

data class NewDownload(
    val url: String,
    val title: String,
    val artist: String,
    val keyword: String
)

data class Source(val id: Long, val name: String, val url: String)

data class SearchResult(val title: String, val artist: String, val url: String)

data class ExtractResult(
    val links: List<SearchResult>,
    val maybeJavaScript: Boolean,
    val error: String?
)

enum class CheckKind { REACHABLE, LINKS, RANGE }

data class CheckResult(val kind: CheckKind, val passed: Boolean, val reason: String)

enum class ThemeMode { SYSTEM, LIGHT, DARK, RANDOM }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "",
    val wifiOnly: Boolean = false,
    val audioFocus: Boolean = true,
    val storageTreeUri: String? = null,
    val storageCapMb: Int = 0,
    val searchLimit: Int = 20,
    val keepScreenOnDriving: Boolean = false,
    val btMonitorEnabled: Boolean = false,
    val onboardingSeen: Boolean = false
)

data class BtRule(
    val address: String,
    val name: String,
    val playlistId: Long?,
    val shuffle: Boolean,
    val latencySec: Int
)

/** weekday uses java.util.Calendar.DAY_OF_WEEK (1 = Sunday .. 7 = Saturday). */
data class BtSchedule(
    val id: Long,
    val address: String,
    val weekday: Int,
    val startMinute: Int,
    val endMinute: Int
)

data class BondedDevice(val address: String, val name: String)

data class PlaybackSnapshot(
    val trackUri: String?,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int
)

sealed interface QueueResult {
    data object Queued : QueueResult
    data class QuotaExhausted(val cap: Int) : QueueResult
    data object DuplicateUrl : QueueResult
    data object DuplicateName : QueueResult
    data object InvalidUrl : QueueResult
}
