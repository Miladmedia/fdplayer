package ir.miladmedia.fdplayer.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.miladmedia.fdplayer.data.worker.AudioDownloadWorker
import ir.miladmedia.fdplayer.domain.model.DownloadStatus
import ir.miladmedia.fdplayer.domain.model.DownloadableAudio
import ir.miladmedia.fdplayer.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : DownloadRepository {

    private val _downloads = MutableStateFlow<List<DownloadableAudio>>(emptyList())

    private val audioExtensions = setOf("mp3", "m4a", "aac", "ogg", "wav", "flac", "opus")

    override fun observeDownloads(): Flow<List<DownloadableAudio>> = _downloads.asStateFlow()

    override suspend fun extractAudioLinks(pageUrl: String): Result<List<DownloadableAudio>> =
        withContext(Dispatchers.IO) {
            try {
                if (isDirectAudioUrl(pageUrl)) {
                    val item = buildAudioItem(pageUrl, null)
                    _downloads.value = listOf(item)
                    return@withContext Result.success(listOf(item))
                }

                val request = Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; FDPlayer/1.0.0)")
                    .build()

                val html = okHttpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("HTTP ${resp.code}"))
                    }
                    resp.body?.string().orEmpty()
                }

                val doc = Jsoup.parse(html, pageUrl)
                val foundMap = linkedMapOf<String, DownloadableAudio>()

                // Scan anchor hrefs, audio tags, and source tags
                doc.select("a[href], audio[src], source[src]").forEach { element ->
                    val rawLink = when {
                        element.hasAttr("href") -> element.absUrl("href")
                        else -> element.absUrl("src")
                    }.trim()

                    if (rawLink.isNotBlank() && isDirectAudioUrl(rawLink) && !foundMap.containsKey(rawLink)) {
                        val anchorText = element.text().trim().takeIf { it.isNotBlank() }
                        foundMap[rawLink] = buildAudioItem(rawLink, anchorText)
                    }
                }

                val extracted = foundMap.values.toList()
                _downloads.value = extracted
                Result.success(extracted)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun enqueueDownload(item: DownloadableAudio) {
        val workRequest = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                workDataOf(
                    AudioDownloadWorker.KEY_ID to item.id,
                    AudioDownloadWorker.KEY_URL to item.url,
                    AudioDownloadWorker.KEY_FILE_NAME to item.fileName
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "fd_download_${item.id}",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun enqueueAll(items: List<DownloadableAudio>) {
        items.filter { it.status == DownloadStatus.IDLE || it.status == DownloadStatus.FAILED }
            .forEach { enqueueDownload(it) }
    }

    override fun updateItemState(
        id: String,
        progress: Float,
        status: DownloadStatus,
        error: String?
    ) {
        _downloads.update { list ->
            list.map { item ->
                if (item.id == id) {
                    item.copy(progress = progress, status = status, errorMessage = error)
                } else {
                    item
                }
            }
        }
    }

    private fun isDirectAudioUrl(url: String): Boolean {
        val path = Uri.parse(url).path?.lowercase().orEmpty()
        val ext = path.substringAfterLast('.', "")
        return ext in audioExtensions
    }

    private fun buildAudioItem(url: String, linkLabel: String?): DownloadableAudio {
        val rawLastSegment = Uri.parse(url).lastPathSegment.orEmpty()
        val decodedName = try {
            URLDecoder.decode(rawLastSegment, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            rawLastSegment
        }.ifBlank { "audio_${System.currentTimeMillis()}.mp3" }

        val cleanTitle = linkLabel
            ?.takeIf { it.length in 2..80 && !it.startsWith("http", ignoreCase = true) }
            ?: decodedName.substringBeforeLast('.')

        return DownloadableAudio(
            id = UUID.nameUUIDFromBytes(url.toByteArray()).toString(),
            url = url,
            fileName = decodedName,
            title = cleanTitle
        )
    }
}
