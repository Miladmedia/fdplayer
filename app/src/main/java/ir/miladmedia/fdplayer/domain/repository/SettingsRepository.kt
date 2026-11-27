package ir.miladmedia.fdplayer.domain.repository

import ir.miladmedia.fdplayer.domain.model.AppThemeMode
import ir.miladmedia.fdplayer.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observePreferences(): Flow<UserPreferences>
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setCustomPaletteIndex(index: Int)
    suspend fun setMicRhythmConsent(granted: Boolean, enabled: Boolean)
    suspend fun setMicRhythmEnabled(enabled: Boolean)
    suspend fun setLanguageTag(tag: String)
}
