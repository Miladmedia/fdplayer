package ir.miladmedia.fdplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import ir.miladmedia.fdplayer.service.NotificationChannels
import javax.inject.Inject

@HiltAndroidApp
class FdApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    NotificationChannels.DOWNLOAD,
                    getString(R.string.channel_download),
                    NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    NotificationChannels.PLAYBACK,
                    getString(R.string.channel_playback),
                    NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    NotificationChannels.BLUETOOTH,
                    getString(R.string.channel_bluetooth),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
        )
    }
}
