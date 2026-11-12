package ir.miladmedia.fdplayer.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE source = :source ORDER BY title COLLATE NOCASE")
    fun observe(source: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun get(id: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE uri = :uri")
    suspend fun getByUri(uri: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(track: TrackEntity): Long

    @Query("UPDATE tracks SET title = :title, artist = :artist, durationMs = :durationMs, missing = 0 WHERE uri = :uri")
    suspend fun updateMeta(uri: String, title: String, artist: String, durationMs: Long)

    @Query("UPDATE tracks SET missing = :missing WHERE uri = :uri")
    suspend fun setMissing(uri: String, missing: Boolean)

    @Query("UPDATE tracks SET missing = 1 WHERE source = 'DEVICE' AND uri NOT IN (:uris)")
    suspend fun markDeviceMissingExcept(uris: List<String>)

    @Query("DELETE FROM tracks WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)
}

@Dao
interface PlaylistDao {
    @Query(
        "SELECT p.id AS id, p.name AS name, " +
            "(SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlistId = p.id) AS trackCount " +
            "FROM playlists p ORDER BY p.name COLLATE NOCASE"
    )
    fun observeAll(): Flow<List<PlaylistRow>>

    @Query(
        "SELECT t.* FROM tracks t INNER JOIN playlist_tracks pt ON pt.trackId = t.id " +
            "WHERE pt.playlistId = :playlistId ORDER BY pt.addedAt"
    )
    fun observeTracks(playlistId: Long): Flow<List<TrackEntity>>

    @Query(
        "SELECT t.* FROM tracks t INNER JOIN playlist_tracks pt ON pt.trackId = t.id " +
            "WHERE pt.playlistId = :playlistId ORDER BY pt.addedAt"
    )
    suspend fun getTracks(playlistId: Long): List<TrackEntity>

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrack(link: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: Long)
}

data class PlaylistRow(val id: Long, val name: String, val trackCount: Int)

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun get(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: DownloadEntity): Long

    @Query("UPDATE downloads SET downloadedBytes = :downloaded, totalBytes = :total WHERE id = :id")
    suspend fun updateProgress(id: Long, downloaded: Long, total: Long)

    @Query("UPDATE downloads SET status = :status, error = :error WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, error: String?)

    @Query("UPDATE downloads SET status = 'DONE', fileUri = :fileUri, sha256 = :sha, error = NULL WHERE id = :id")
    suspend fun markDone(id: Long, fileUri: String, sha: String)

    @Query("SELECT COUNT(*) FROM downloads WHERE keyword = :keyword AND status = 'DONE'")
    suspend fun countDone(keyword: String): Int

    @Query("SELECT COUNT(*) FROM downloads WHERE keyword = :keyword AND status IN ('QUEUED','DOWNLOADING')")
    suspend fun countActive(keyword: String): Int

    @Query("SELECT COUNT(*) FROM downloads WHERE url = :url AND status NOT IN ('FAILED','CANCELED','DUPLICATE')")
    suspend fun urlCount(url: String): Int

    @Query(
        "SELECT COUNT(*) FROM downloads WHERE status NOT IN ('FAILED','CANCELED','DUPLICATE') " +
            "AND (nameKey = :nameKey OR titleKey = :titleKey)"
    )
    suspend fun nameCount(nameKey: String, titleKey: String): Int

    @Query("SELECT COUNT(*) FROM downloads WHERE sha256 = :sha AND id != :excludeId AND status = 'DONE'")
    suspend fun hashCount(sha: String, excludeId: Long): Int

    @Query("SELECT COALESCE(SUM(totalBytes),0) FROM downloads WHERE status = 'DONE'")
    suspend fun totalDoneBytes(): Long

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM downloads WHERE url = :url AND status IN ('FAILED','CANCELED','DUPLICATE')")
    suspend fun deleteInactiveByUrl(url: String)

    @Query("DELETE FROM downloads WHERE status IN ('FAILED','CANCELED','DUPLICATE')")
    suspend fun clearFinished()
}

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY id")
    fun observe(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources ORDER BY id")
    suspend fun getAll(): List<SourceEntity>

    @Insert
    suspend fun insert(source: SourceEntity): Long

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BluetoothDao {
    @Query("SELECT * FROM bt_rules ORDER BY name")
    fun observeRules(): Flow<List<BtRuleEntity>>

    @Query("SELECT * FROM bt_rules WHERE address = :address")
    suspend fun getRule(address: String): BtRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRule(rule: BtRuleEntity)

    @Query("DELETE FROM bt_rules WHERE address = :address")
    suspend fun deleteRule(address: String)

    @Query("DELETE FROM bt_schedules WHERE address = :address")
    suspend fun deleteSchedules(address: String)

    @Query("SELECT * FROM bt_schedules WHERE address = :address ORDER BY weekday, startMinute")
    fun observeSchedules(address: String): Flow<List<BtScheduleEntity>>

    @Query("SELECT * FROM bt_schedules WHERE address = :address")
    suspend fun getSchedules(address: String): List<BtScheduleEntity>

    @Insert
    suspend fun addSchedule(s: BtScheduleEntity): Long

    @Query("DELETE FROM bt_schedules WHERE id = :id")
    suspend fun removeSchedule(id: Long)
}

@Database(
    entities = [
        TrackEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class,
        DownloadEntity::class, SourceEntity::class, BtRuleEntity::class, BtScheduleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun downloadDao(): DownloadDao
    abstract fun sourceDao(): SourceDao
    abstract fun bluetoothDao(): BluetoothDao
}
