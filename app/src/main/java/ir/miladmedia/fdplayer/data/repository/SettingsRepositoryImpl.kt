package ir.miladmedia.fdplayer.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.miladmedia.fdplayer.domain.model.AppThemeMode
import ir.miladmedia.fdplayer.domain.model.UserPreferences
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "fd_player_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CUSTOM_PALETTE_INDEX = intPreferencesKey("custom_palette_index")
        val MIC_CONSENT_GRANTED = booleanPreferencesKey("mic_consent_granted")
        val MIC_RHYTHM_ENABLED = booleanPreferencesKey("mic_rhythm_enabled")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
    }

    override fun observePreferences(): Flow<UserPreferences> {
        return context.settingsDataStore.data.map { prefs ->
            UserPreferences(
                themeMode = AppThemeMode.fromKey(prefs[Keys.THEME_MODE]),
                customPaletteIndex = (prefs[Keys.CUSTOM_PALETTE_INDEX] ?: 0).coerceIn(0, 7),
                micRhythmConsentGranted = prefs[Keys.MIC_CONSENT_GRANTED] ?: false,
                micRhythmEnabled = prefs[Keys.MIC_RHYTHM_ENABLED] ?: false,
                languageTag = prefs[Keys.LANGUAGE_TAG] ?: "fa"
            )
        }
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    override suspend fun setCustomPaletteIndex(index: Int) {
        context.settingsDataStore.edit { it[Keys.CUSTOM_PALETTE_INDEX] = index.coerceIn(0, 7) }
    }

    override suspend fun setMicRhythmConsent(granted: Boolean, enabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.MIC_CONSENT_GRANTED] = granted
            it[Keys.MIC_RHYTHM_ENABLED] = enabled
        }
    }

    override suspend fun setMicRhythmEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MIC_RHYTHM_ENABLED] = enabled }
    }

    override suspend fun setLanguageTag(tag: String) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE_TAG] = tag }
    }
}
