package ir.miladmedia.fdplayer.domain.repository

import ir.miladmedia.fdplayer.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface AudioRepository {
    fun observeTracks(): Flow<List<Track>>
    suspend fun refreshTracks(): List<Track>
    suspend fun getTracksByIds(ids: List<Long>): List<Track>
}
