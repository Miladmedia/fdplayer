package ir.miladmedia.fdplayer.data.worker

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.miladmedia.fdplayer.domain.model.DownloadStatus
import ir.miladmedia.fdplayer.domain.repository.AudioRepository
import ir.miladmedia.fdplayer.domain.repository.DownloadRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class AudioDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val downloadRepository: DownloadRepository,
    private val audioRepository: AudioRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getString(KEY_ID) ?: return Result.failure()
        val audioUrl = inputData.getString(KEY_URL) ?: return Result.failure()
        val rawFileName = inputData.getString(KEY_FILE_NAME) ?: "audio_${System.currentTimeMillis()}.mp3"
        val safeFileName = rawFileName.replace(Regex("[^a-zA-Z0-9._\-\u0600-\u06FF ]"), "_")

        downloadRepository.updateItemState(itemId, 0.02f, DownloadStatus.DOWNLOADING)

        return try {
            val request = Request.Builder()
                .url(audioUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; FDPlayer/1.0.0)")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("Empty body")
                val totalBytes = body.contentLength()
                val mimeType = resolveMimeType(safeFileName, response.header("Content-Type"))

                saveToDownloadsAppFolder(
                    fileName = safeFileName,
                    mimeType = mimeType,
                    totalBytes = totalBytes,
                    inputStream = body.byteStream(),
                    onProgress = { progress ->
                        downloadRepository.updateItemState(itemId, progress, DownloadStatus.DOWNLOADING)
                    }
                )
            }

            downloadRepository.updateItemState(itemId, 1f, DownloadStatus.COMPLETED)
            audioRepository.refreshTracks()
            Result.success()
        } catch (e: Exception) {
            downloadRepository.updateItemState(
                id = itemId,
                progress = 0f,
                status = DownloadStatus.FAILED,
                error = e.localizedMessage ?: "Download error"
            )
            Result.failure()
        }
    }

    private fun saveToDownloadsAppFolder(
        fileName: String,
        mimeType: String,
        totalBytes: Long,
        inputStream: java.io.InputStream,
        onProgress: (Float) -> Unit
    ) {
        val buffer = ByteArray(8192)
        var downloaded = 0L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = applicationContext.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/FD Player"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Unable to create MediaStore download entry")

            try {
                resolver.openOutputStream(uri)?.use { out ->
                    while (true) {
                        val read = inputStream.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0L) {
                            onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0.05f, 0.99f))
                        }
                    }
                    out.flush()
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appFolder = File(downloadsDir, "FD Player").apply { mkdirs() }
            val outFile = File(appFolder, fileName)
            FileOutputStream(outFile).use { out ->
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    downloaded += read
                    if (totalBytes > 0L) {
                        onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0.05f, 0.99f))
                    }
                }
                out.flush()
            }
        }
    }

    private fun resolveMimeType(fileName: String, headerMime: String?): String {
        if (!headerMime.isNullOrBlank() && headerMime.startsWith("audio/")) {
            return headerMime.substringBefore(";")
        }
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            else -> "audio/mpeg"
        }
    }

    companion object {
        const val KEY_ID = "key_id"
        const val KEY_URL = "key_url"
        const val KEY_FILE_NAME = "key_file_name"
    }
}
