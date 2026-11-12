package ir.miladmedia.fdplayer.service

import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.repository.BluetoothRepository
import ir.miladmedia.fdplayer.domain.usecase.IsAutoPlayAllowedUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothMonitorService : Service() {

    @Inject lateinit var bluetooth: BluetoothRepository
    @Inject lateinit var isAutoPlayAllowed: IsAutoPlayAllowedUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingAutoplay: Job? = null

    /** Address of the device whose connection actually started playback, if any. */
    @Volatile
    private var autoplayAddress: String? = null

    private val aclReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = deviceFrom(intent) ?: return
            val address = addressOf(device) ?: return
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> onConnected(address)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> onDisconnected(address)
            }
        }
    }

    /** Reading the address needs BLUETOOTH_CONNECT on API 31+. */
    private fun addressOf(device: BluetoothDevice): String? {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        return runCatching { device.address }.getOrNull()
    }

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            this, aclReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        pendingAutoplay?.cancel()
        runCatching { unregisterReceiver(aclReceiver) }
        scope.cancel()
        super.onDestroy()
    }

    private fun onConnected(address: String) {
        pendingAutoplay?.cancel()
        pendingAutoplay = scope.launch {
            val rule = bluetooth.getRule(address) ?: return@launch
            val now = Calendar.getInstance()
            val allowed = isAutoPlayAllowed(
                address = address,
                weekday = now.get(Calendar.DAY_OF_WEEK),
                minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            )
            if (!allowed) return@launch
            if (rule.latencySec > 0) delay(rule.latencySec * 1000L)
            autoplayAddress = address
            val intent = Intent(this@BluetoothMonitorService, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_AUTOPLAY
                putExtra(PlaybackService.EXTRA_PLAYLIST_ID, rule.playlistId ?: -1L)
                putExtra(PlaybackService.EXTRA_SHUFFLE, rule.shuffle)
            }
            ContextCompat.startForegroundService(this@BluetoothMonitorService, intent)
        }
    }

    private fun onDisconnected(address: String) {
        pendingAutoplay?.cancel()
        // Only stop playback when this very device started it; otherwise a random
        // headset disconnect would silence music the user started by hand.
        if (autoplayAddress != address) return
        autoplayAddress = null
        pendingAutoplay = scope.launch {
            bluetooth.getRule(address) ?: return@launch
            val intent = Intent(this@BluetoothMonitorService, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_PAUSE
            }
            runCatching { ContextCompat.startForegroundService(this@BluetoothMonitorService, intent) }
        }
    }

    private fun deviceFrom(intent: Intent): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

    private fun startAsForeground() {
        val notification: Notification = NotificationCompat.Builder(this, NotificationChannels.BLUETOOTH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.bt_monitor))
            .setContentText(getString(R.string.bt_monitoring))
            .setOngoing(true)
            .setSilent(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
    }
}
