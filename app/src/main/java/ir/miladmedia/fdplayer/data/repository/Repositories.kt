package ir.miladmedia.fdplayer.data.repository

import android.content.ContentUris
import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.miladmedia.fdplayer.data.local.BluetoothDao
import ir.miladmedia.fdplayer.data.local.DownloadDao
import ir.miladmedia.fdplayer.data.local.DownloadEntity
import ir.miladmedia.fdplayer.data.local.PlaylistDao
import ir.miladmedia.fdplayer.data.local.PlaylistEntity
import ir.miladmedia.fdplayer.data.local.PlaylistTrackEntity
import ir.miladmedia.fdplayer.data.local.SourceDao
import ir.miladmedia.fdplayer.data.local.SourceEntity
import ir.miladmedia.fdplayer.data.local.TrackDao
import ir.miladmedia.fdplayer.data.local.TrackEntity
import ir.miladmedia.fdplayer.data.mapper.toDomain
import ir.miladmedia.fdplayer.data.mapper.toEntity
import ir.miladmedia.fdplayer.domain.model.BondedDevice
import ir.miladmedia.fdplayer.domain.model.BtRule
import ir.miladmedia.fdplayer.domain.model.BtSchedule
import ir.miladmedia.fdplayer.domain.model.DownloadItem
import ir.miladmedia.fdplayer.domain.model.DownloadStatus
import ir.miladmedia.fdplayer.domain.model.NewDownload
import ir.miladmedia.fdplayer.domain.model.Playlist
import ir.miladmedia.fdplayer.domain.model.Source
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.domain.model.TrackSource
import ir.miladmedia.fdplayer.domain.repository.BluetoothRepository
import ir.miladmedia.fdplayer.domain.repository.DownloadRepository
import ir.miladmedia.fdplayer.domain.repository.LibraryRepository
import ir.miladmedia.fdplayer.domain.repository.PlaylistRepository
import ir.miladmedia.fdplayer.domain.repository.SourceRepository
import ir.miladmedia.fdplayer.domain.util.TextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TrackDao
) : LibraryRepository {

    override fun observeTracks(source: TrackSource): Flow<List<Track>> =
        dao.observe(source.name).map { list -> list.map { it.toDomain() } }

    override suspend fun getTrack(id: Long) = dao.get(id)?.toDomain()
    override suspend fun getTrackByUri(uri: String) = dao.getByUri(uri)?.toDomain()
    override suspend fun markMissing(uri: String, missing: Boolean) = dao.setMissing(uri, missing)
    override suspend fun removeTrackByUri(uri: String) = dao.deleteByUri(uri)

    override suspend fun scanDeviceAudio(): Int = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val seen = ArrayList<String>()
        context.contentResolver.query(
            collection, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE"
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val uri = ContentUris.withAppendedId(collection, c.getLong(iId)).toString()
                val title = c.getString(iTitle) ?: ""
                val artist = (c.getString(iArtist) ?: "").takeUnless { it == "<unknown>" } ?: ""
                val dur = c.getLong(iDur)
                val rowId = dao.insert(TrackEntity(0, uri, title, artist, dur, TrackSource.DEVICE.name))
                if (rowId == -1L) dao.updateMeta(uri, title, artist, dur)
                seen.add(uri)
            }
        }
        if (seen.isNotEmpty() && seen.size <= 900) dao.markDeviceMissingExcept(seen)
        seen.size
    }

    override suspend fun addDownloaded(uri: String, title: String, artist: String, durationMs: Long) {
        val id = dao.insert(TrackEntity(0, uri, title, artist, durationMs, TrackSource.DOWNLOAD.name))
        if (id == -1L) dao.updateMeta(uri, title, artist, durationMs)
    }
}

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val dao: PlaylistDao
) : PlaylistRepository {
    override fun observePlaylists(): Flow<List<Playlist>> =
        dao.observeAll().map { l -> l.map { it.toDomain() } }

    override fun observePlaylistTracks(playlistId: Long): Flow<List<Track>> =
        dao.observeTracks(playlistId).map { l -> l.map { it.toDomain() } }

    override suspend fun getPlaylistTracks(playlistId: Long) =
        dao.getTracks(playlistId).map { it.toDomain() }

    override suspend fun create(name: String) = dao.insert(PlaylistEntity(0, name.trim()))
    override suspend fun rename(id: Long, name: String) = dao.rename(id, name.trim())
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun addTrack(playlistId: Long, trackId: Long) =
        dao.addTrack(PlaylistTrackEntity(playlistId, trackId, System.currentTimeMillis()))

    override suspend fun removeTrack(playlistId: Long, trackId: Long) =
        dao.removeTrack(playlistId, trackId)
}

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val dao: DownloadDao
) : DownloadRepository {
    override fun observeAll(): Flow<List<DownloadItem>> =
        dao.observeAll().map { l -> l.map { it.toDomain() } }

    override suspend fun get(id: Long) = dao.get(id)?.toDomain()

    override suspend fun enqueue(item: NewDownload): Long {
        dao.deleteInactiveByUrl(item.url)
        return dao.insert(
        DownloadEntity(
            url = item.url,
            title = item.title,
            artist = item.artist,
            keyword = item.keyword,
            nameKey = TextNormalizer.nameKey(item.title, item.artist),
            titleKey = TextNormalizer.titleKey(item.title),
            status = DownloadStatus.QUEUED.name,
            createdAt = System.currentTimeMillis()
        )
        )
    }

    override suspend fun updateProgress(id: Long, downloaded: Long, total: Long) =
        dao.updateProgress(id, downloaded, total)

    override suspend fun setStatus(id: Long, status: DownloadStatus, error: String?) =
        dao.setStatus(id, status.name, error)

    override suspend fun markDone(id: Long, fileUri: String, sha256: String) =
        dao.markDone(id, fileUri, sha256)

    override suspend fun countDone(keyword: String) = dao.countDone(keyword)
    override suspend fun countActive(keyword: String) = dao.countActive(keyword)
    override suspend fun urlExists(url: String) = dao.urlCount(url) > 0
    override suspend fun nameExists(nameKey: String, titleKey: String) =
        dao.nameCount(nameKey, titleKey) > 0

    override suspend fun hashExists(sha256: String, excludeId: Long) =
        dao.hashCount(sha256, excludeId) > 0

    override suspend fun totalDoneBytes() = dao.totalDoneBytes()
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun clearFinished() = dao.clearFinished()
}

@Singleton
class SourceRepositoryImpl @Inject constructor(
    private val dao: SourceDao
) : SourceRepository {
    override fun observe(): Flow<List<Source>> = dao.observe().map { l -> l.map { it.toDomain() } }
    override suspend fun getAll() = dao.getAll().map { it.toDomain() }
    override suspend fun add(name: String, url: String) {
        dao.insert(SourceEntity(0, name.trim(), url.trim()))
    }
    override suspend fun remove(id: Long) = dao.delete(id)
}

@Singleton
class BluetoothRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ir.miladmedia.fdplayer.data.local.BluetoothDao
) : BluetoothRepository {
    override fun bondedDevices(): List<BondedDevice> {
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) return emptyList()
        val manager = context.getSystemService(BluetoothManager::class.java) ?: return emptyList()
        @Suppress("MissingPermission")
        val bonded = manager.adapter?.bondedDevices.orEmpty()
        return bonded.map { BondedDevice(it.address, it.name ?: it.address) }
            .sortedBy { it.name }
    }

    override fun observeRules(): Flow<List<BtRule>> =
        dao.observeRules().map { l -> l.map { it.toDomain() } }

    override suspend fun getRule(address: String) = dao.getRule(address)?.toDomain()
    override suspend fun saveRule(rule: BtRule) = dao.saveRule(rule.toEntity())
    override suspend fun deleteRule(address: String) {
        dao.deleteSchedules(address)
        dao.deleteRule(address)
    }

    override fun observeSchedules(address: String): Flow<List<BtSchedule>> =
        dao.observeSchedules(address).map { l -> l.map { it.toDomain() } }

    override suspend fun getSchedules(address: String) =
        dao.getSchedules(address).map { it.toDomain() }

    override suspend fun addSchedule(schedule: BtSchedule) {
        dao.addSchedule(schedule.toEntity())
    }

    override suspend fun removeSchedule(id: Long) = dao.removeSchedule(id)
}
