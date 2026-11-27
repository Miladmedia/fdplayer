package ir.miladmedia.fdplayer.domain.repository

import ir.miladmedia.fdplayer.domain.model.DownloadableAudio
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeDownloads(): Flow<List<DownloadableAudio>>
    suspend fun extractAudioLinks(pageUrl: String): Result<List<DownloadableAudio>>
    fun enqueueDownload(item: DownloadableAudio)
    fun enqueueAll(items: List<DownloadableAudio>)
    fun updateItemState(
        id: String,
        progress: Float,
        status: ir.miladmedia.fdplayer.domain.model.DownloadStatus,
        error: String? = null
    )
}
