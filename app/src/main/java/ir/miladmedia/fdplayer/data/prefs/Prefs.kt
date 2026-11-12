package ir.miladmedia.fdplayer.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.miladmedia.fdplayer.domain.model.AppSettings
import ir.miladmedia.fdplayer.domain.model.PlaybackSnapshot
import ir.miladmedia.fdplayer.domain.model.ThemeMode
import ir.miladmedia.fdplayer.domain.repository.PlaybackStateRepository
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore(name = "settings")
private val Context.playbackStore by preferencesDataStore(name = "playback")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object K {
        val theme = stringPreferencesKey("theme")
        val language = stringPreferencesKey("language")
        val wifiOnly = booleanPreferencesKey("wifi_only")
        val audioFocus = booleanPreferencesKey("audio_focus")
        val storageTree = stringPreferencesKey("storage_tree")
        val storageCap = intPreferencesKey("storage_cap_mb")
        val searchLimit = intPreferencesKey("search_limit")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val btMonitor = booleanPreferencesKey("bt_monitor")
        val onboardingSeen = booleanPreferencesKey("onboarding_seen")
    }

    override val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            theme = runCatching { ThemeMode.valueOf(p[K.theme] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            language = p[K.language] ?: "",
            wifiOnly = p[K.wifiOnly] ?: false,
            audioFocus = p[K.audioFocus] ?: true,
            storageTreeUri = p[K.storageTree],
            storageCapMb = p[K.storageCap] ?: 0,
            searchLimit = p[K.searchLimit] ?: 20,
            keepScreenOnDriving = p[K.keepScreenOn] ?: false,
            btMonitorEnabled = p[K.btMonitor] ?: false,
            onboardingSeen = p[K.onboardingSeen] ?: false
        )
    }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        val current = settings.first()
        val next = transform(current)
        context.settingsStore.edit { p ->
            p[K.theme] = next.theme.name
            p[K.language] = next.language
            p[K.wifiOnly] = next.wifiOnly
            p[K.audioFocus] = next.audioFocus
            setOrRemove(p, K.storageTree, next.storageTreeUri)
            p[K.storageCap] = next.storageCapMb
            p[K.searchLimit] = next.searchLimit
            p[K.keepScreenOn] = next.keepScreenOnDriving
            p[K.btMonitor] = next.btMonitorEnabled
            p[K.onboardingSeen] = next.onboardingSeen
        }
    }

    private fun setOrRemove(p: androidx.datastore.preferences.core.MutablePreferences,
                            key: Preferences.Key<String>, value: String?) {
        if (value == null) p.remove(key) else p[key] = value
    }
}

@Singleton
class PlaybackStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PlaybackStateRepository {
    private val kUri = stringPreferencesKey("uri")
    private val kPos = longPreferencesKey("pos")
    private val kShuffle = booleanPreferencesKey("shuffle")
    private val kRepeat = intPreferencesKey("repeat")

    override suspend fun save(snapshot: PlaybackSnapshot) {
        context.playbackStore.edit { p ->
            val u = snapshot.trackUri
            if (u == null) p.remove(kUri) else p[kUri] = u
            p[kPos] = snapshot.positionMs
            p[kShuffle] = snapshot.shuffle
            p[kRepeat] = snapshot.repeatMode
        }
    }

    override suspend fun load(): PlaybackSnapshot {
        val p = context.playbackStore.data.first()
        return PlaybackSnapshot(p[kUri], p[kPos] ?: 0L, p[kShuffle] ?: false, p[kRepeat] ?: 0)
    }
}
