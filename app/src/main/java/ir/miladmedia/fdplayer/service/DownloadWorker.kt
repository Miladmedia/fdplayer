package ir.miladmedia.fdplayer.service

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.model.DownloadStatus
import ir.miladmedia.fdplayer.domain.repository.DownloadRepository
import ir.miladmedia.fdplayer.domain.repository.LibraryRepository
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import ir.miladmedia.fdplayer.data.storage.StorageWriter
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Downloads a single queued item. Streams to a temp file in cache, hashes it,
 * then either marks it DUPLICATE (same content already downloaded) or moves it
 * to the chosen destination and adds it to the library.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloads: DownloadRepository,
    private val library: LibraryRepository,
    private val settings: SettingsRepository,
    private val http: OkHttpClient
) : CoroutineWorker(appContext, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(applicationContext.getString(R.string.downloading_title))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1L)
        if (id <= 0L) return Result.failure()
        val item = downloads.get(id) ?: return Result.failure()
        if (item.status == DownloadStatus.DONE || item.status == DownloadStatus.CANCELED) {
            return Result.success()
        }

        setForeground(getForegroundInfo())
        downloads.setStatus(id, DownloadStatus.DOWNLOADING, null)

        val temp = File(applicationContext.cacheDir, "dl-$id.part")
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val total = streamTo(item.url, temp, digest, id)
            val sha = digest.digest().joinToString("") { "%02x".format(it) }

            if (downloads.hashExists(sha, id)) {
                downloads.setStatus(id, DownloadStatus.DUPLICATE, null)
                temp.delete()
                return Result.success()
            }

            val settingsNow = settings.settings.first()
            if (settingsNow.storageCapMb > 0) {
                val capBytes = settingsNow.storageCapMb * 1024L * 1024L
                if (downloads.totalDoneBytes() + total > capBytes) {
                    downloads.setStatus(id, DownloadStatus.FAILED, "cap")
                    temp.delete()
                    return Result.failure()
                }
            }

            val uri = StorageWriter.publish(applicationContext, settingsNow.storageTreeUri, temp, item.title)
            downloads.markDone(id, uri, sha)
            library.addDownloaded(uri, item.title, item.artist, 0L)
            temp.delete()
            Result.success()
        } catch (e: Exception) {
            temp.delete()
            if (runAttemptCount < MAX_ATTEMPTS) {
                downloads.setStatus(id, DownloadStatus.QUEUED, e.message)
                Result.retry()
            } else {
                downloads.setStatus(id, DownloadStatus.FAILED, e.message)
                Result.failure()
            }
        }
    }

    /** Returns the number of bytes written. Reports progress to the repository. */
    private suspend fun streamTo(url: String, dest: File, digest: MessageDigest, id: Long): Long {
        val request = Request.Builder().url(url).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            val body = response.body ?: throw IllegalStateException("empty body")
            val total = body.contentLength()
            var written = 0L
            var lastReport = 0L
            body.byteStream().use { input: InputStream ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(DEFAULT_BUFFER)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        digest.update(buf, 0, n)
                        written += n
                        if (written - lastReport >= REPORT_STEP) {
                            downloads.updateProgress(id, written, total)
                            lastReport = written
                        }
                    }
                }
            }
            downloads.updateProgress(id, written, total)
            return written
        }
    }

    companion object {
        const val KEY_ID = "download_id"
        private const val NOTIFICATION_ID = 3001
        private const val MAX_ATTEMPTS = 3
        private const val DEFAULT_BUFFER = 64 * 1024
        private const val REPORT_STEP = 256 * 1024L
    }
}
