package ir.miladmedia.fdplayer.data.repository

import ir.miladmedia.fdplayer.data.local.dao.PlaylistDao
import ir.miladmedia.fdplayer.data.local.entity.PlaylistEntity
import ir.miladmedia.fdplayer.data.mapper.toDomain
import ir.miladmedia.fdplayer.data.mapper.toEntity
import ir.miladmedia.fdplayer.domain.model.Playlist
import ir.miladmedia.fdplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> {
        return playlistDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPlaylistById(id: Long): Playlist? {
        return playlistDao.getById(id)?.toDomain()
    }

    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.insert(PlaylistEntity(name = name.trim()))
    }

    override suspend fun deletePlaylist(id: Long) {
        playlistDao.deleteById(id)
    }

    override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        val existing = playlistDao.getById(playlistId)?.toDomain() ?: return
        if (trackId !in existing.trackIds) {
            val updated = existing.copy(trackIds = existing.trackIds + trackId)
            playlistDao.update(updated.toEntity())
        }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        val existing = playlistDao.getById(playlistId)?.toDomain() ?: return
        val updated = existing.copy(trackIds = existing.trackIds.filterNot { it == trackId })
        playlistDao.update(updated.toEntity())
    }
}
