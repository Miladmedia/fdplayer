package ir.miladmedia.fdplayer.domain.repository

import ir.miladmedia.fdplayer.domain.model.AppSettings
import ir.miladmedia.fdplayer.domain.model.BondedDevice
import ir.miladmedia.fdplayer.domain.model.BtRule
import ir.miladmedia.fdplayer.domain.model.BtSchedule
import ir.miladmedia.fdplayer.domain.model.CheckResult
import ir.miladmedia.fdplayer.domain.model.DownloadItem
import ir.miladmedia.fdplayer.domain.model.DownloadStatus
import ir.miladmedia.fdplayer.domain.model.ExtractResult
import ir.miladmedia.fdplayer.domain.model.NewDownload
import ir.miladmedia.fdplayer.domain.model.PlaybackSnapshot
import ir.miladmedia.fdplayer.domain.model.Playlist
import ir.miladmedia.fdplayer.domain.model.SearchResult
import ir.miladmedia.fdplayer.domain.model.Source
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.domain.model.TrackSource
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeTracks(source: TrackSource): Flow<List<Track>>
    suspend fun getTrack(id: Long): Track?
    suspend fun getTrackByUri(uri: String): Track?
    suspend fun markMissing(uri: String, missing: Boolean)
    suspend fun scanDeviceAudio(): Int
    suspend fun addDownloaded(uri: String, title: String, artist: String, durationMs: Long)
    suspend fun removeTrackByUri(uri: String)
}

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    fun observePlaylistTracks(playlistId: Long): Flow<List<Track>>
    suspend fun getPlaylistTracks(playlistId: Long): List<Track>
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
    suspend fun addTrack(playlistId: Long, trackId: Long)
    suspend fun removeTrack(playlistId: Long, trackId: Long)
}

interface DownloadRepository {
    fun observeAll(): Flow<List<DownloadItem>>
    suspend fun get(id: Long): DownloadItem?
    suspend fun enqueue(item: NewDownload): Long
    suspend fun updateProgress(id: Long, downloaded: Long, total: Long)
    suspend fun setStatus(id: Long, status: DownloadStatus, error: String?)
    suspend fun markDone(id: Long, fileUri: String, sha256: String)
    suspend fun countDone(keyword: String): Int
    suspend fun countActive(keyword: String): Int
    suspend fun urlExists(url: String): Boolean
    suspend fun nameExists(nameKey: String, titleKey: String): Boolean
    suspend fun hashExists(sha256: String, excludeId: Long): Boolean
    suspend fun totalDoneBytes(): Long
    suspend fun delete(id: Long)
    suspend fun clearFinished()
}

interface SourceRepository {
    fun observe(): Flow<List<Source>>
    suspend fun getAll(): List<Source>
    suspend fun add(name: String, url: String)
    suspend fun remove(id: Long)
}

/** Network access to download sources (implemented in data). */
interface SourceGateway {
    suspend fun search(sources: List<Source>, keyword: String, limit: Int): List<SearchResult>
    suspend fun extract(url: String): ExtractResult
    suspend fun test(url: String): List<CheckResult>
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

interface PlaybackStateRepository {
    suspend fun save(snapshot: PlaybackSnapshot)
    suspend fun load(): PlaybackSnapshot
}

interface BluetoothRepository {
    fun observeRules(): Flow<List<BtRule>>
    suspend fun getRule(address: String): BtRule?
    suspend fun saveRule(rule: BtRule)
    suspend fun deleteRule(address: String)
    fun observeSchedules(address: String): Flow<List<BtSchedule>>
    suspend fun getSchedules(address: String): List<BtSchedule>
    suspend fun addSchedule(schedule: BtSchedule)
    suspend fun removeSchedule(id: Long)
    /** Paired devices; empty when Bluetooth permission is missing. */
    fun bondedDevices(): List<BondedDevice>
}
