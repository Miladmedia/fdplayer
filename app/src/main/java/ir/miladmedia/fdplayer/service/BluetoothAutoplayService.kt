package ir.miladmedia.fdplayer.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
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
import ir.miladmedia.fdplayer.MainActivity
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.repository.AudioRepository
import ir.miladmedia.fdplayer.domain.repository.BluetoothRuleRepository
import ir.miladmedia.fdplayer.domain.repository.PlaybackController
import ir.miladmedia.fdplayer.domain.repository.PlaylistRepository
import ir.miladmedia.fdplayer.domain.usecase.MatchBluetoothRuleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothAutoplayService : Service() {

    @Inject lateinit var bluetoothRuleRepository: BluetoothRuleRepository
    @Inject lateinit var playlistRepository: PlaylistRepository
    @Inject lateinit var audioRepository: AudioRepository
    @Inject lateinit var playbackController: PlaybackController
    @Inject lateinit var matchBluetoothRuleUseCase: MatchBluetoothRuleUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val isAutoplayInProgress = AtomicBoolean(false)
    private var lastHandledAddress: String? = null
    private var lastHandledTimestampMs: Long = 0L
    private var autoplayJob: Job? = null
    private var receiverRegistered = false

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!hasBluetoothConnectPermission(context)) {
                stopSelf()
                return
            }

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device: BluetoothDevice? = extractBluetoothDevice(intent)
                    device?.let { onDeviceConnected(it) }
                }

                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED
                    )
                    val device: BluetoothDevice? = extractBluetoothDevice(intent)
                    if (state == BluetoothProfile.STATE_CONNECTED && device != null) {
                        onDeviceConnected(device)
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED &&
                        device?.address == lastHandledAddress
                    ) {
                        lastHandledAddress = null
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device: BluetoothDevice? = extractBluetoothDevice(intent)
                    if (device?.address == lastHandledAddress) {
                        lastHandledAddress = null
                    }
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val adapterState = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    if (adapterState == BluetoothAdapter.STATE_OFF) {
                        lastHandledAddress = null
                        isAutoplayInProgress.set(false)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!hasBluetoothConnectPermission(this)) {
            stopSelf()
            return
        }

        createNotificationChannel()
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            btReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasBluetoothConnectPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun onDeviceConnected(device: BluetoothDevice) {
        val address = try {
            device.address
        } catch (_: SecurityException) {
            stopSelf()
            return
        }

        val now = System.currentTimeMillis()
        // Ignore duplicate ACL + A2DP connection events for the same device while in progress or within cooldown
        if (isAutoplayInProgress.get() && address == lastHandledAddress) {
            return
        }
        if (address == lastHandledAddress && (now - lastHandledTimestampMs) < DUPLICATE_COOLDOWN_MS) {
            return
        }

        lastHandledAddress = address
        lastHandledTimestampMs = now
        isAutoplayInProgress.set(true)

        autoplayJob?.cancel()
        autoplayJob = serviceScope.launch {
            try {
                val activeRules = bluetoothRuleRepository.getActiveRules()
                val matchedRule = matchBluetoothRuleUseCase(activeRules, address)

                // Base 2.5s stabilization delay + optional rule latencySec
                val extraDelayMs = (matchedRule?.latencySec ?: 0).coerceAtLeast(0) * 1000L
                delay(BASE_STABILIZATION_DELAY_MS + extraDelayMs)

                if (matchedRule != null) {
                    val targetPlaylistId = matchedRule.playlistId
                    if (targetPlaylistId != null) {
                        val playlist = playlistRepository.getPlaylistById(targetPlaylistId)
                        val tracks = if (playlist != null && playlist.trackIds.isNotEmpty()) {
                            audioRepository.getTracksByIds(playlist.trackIds)
                        } else {
                            emptyList()
                        }

                        if (tracks.isNotEmpty()) {
                            playbackController.playTracks(
                                tracks = tracks,
                                startIndex = 0,
                                shuffle = matchedRule.shuffle
                            )
                            return@launch
                        }
                    }
                    // Matched rule uses current queue or playlist had no tracks -> fallback to current queue
                    if (matchedRule.shuffle) {
                        playbackController.toggleShuffle()
                    }
                    resumeOrFallbackToLibrary()
                } else {
                    // No rule matched -> play current playlist/queue
                    resumeOrFallbackToLibrary()
                }
            } finally {
                isAutoplayInProgress.set(false)
            }
        }
    }

    private suspend fun resumeOrFallbackToLibrary() {
        val currentState = playbackController.playbackState.value
        if (currentState.queue.isNotEmpty()) {
            playbackController.playOrResumeCurrent()
        } else {
            val allTracks = audioRepository.refreshTracks()
            if (allTracks.isNotEmpty()) {
                playbackController.playTracks(allTracks, startIndex = 0, shuffle = false)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun extractBluetoothDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_bt_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            101,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notif_bt_title))
            .setContentText(getString(R.string.notif_bt_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        autoplayJob?.cancel()
        if (receiverRegistered) {
            try {
                unregisterReceiver(btReceiver)
            } catch (_: IllegalArgumentException) {
            }
            receiverRegistered = false
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "fd_bluetooth_autoplay_channel"
        private const val NOTIFICATION_ID = 2042
        private const val BASE_STABILIZATION_DELAY_MS = 2500L
        private const val DUPLICATE_COOLDOWN_MS = 10_000L

        fun hasBluetoothConnectPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun startIfPermitted(context: Context) {
            if (!hasBluetoothConnectPermission(context)) return
            try {
                val intent = Intent(context, BluetoothAutoplayService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }
    }
}
