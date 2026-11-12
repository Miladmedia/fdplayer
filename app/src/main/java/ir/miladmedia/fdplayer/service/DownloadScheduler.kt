package ir.miladmedia.fdplayer.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues [DownloadWorker] for a download row. One unique work per download id. */
@Singleton
class DownloadScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule(downloadId: Long, wifiOnly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_ID to downloadId))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(downloadId),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(downloadId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(downloadId))
    }

    private fun uniqueName(downloadId: Long) = "download-$downloadId"

    companion object {
        const val TAG = "downloads"
    }
}
