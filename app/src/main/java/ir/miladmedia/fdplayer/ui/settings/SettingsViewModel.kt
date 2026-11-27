package ir.miladmedia.fdplayer.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.miladmedia.fdplayer.data.preferences.TipsPreferences
import ir.miladmedia.fdplayer.domain.model.AppThemeMode
import ir.miladmedia.fdplayer.domain.model.UserPreferences
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val showGuideDialog: Boolean = false,
    val showFirstTimeTip: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val tipsPreferences: TipsPreferences
) : ViewModel() {

    private val showGuideDialog = MutableStateFlow(false)
    private val showTip = MutableStateFlow(!tipsPreferences.isTipDismissed("settings"))

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observePreferences(),
        showGuideDialog,
        showTip
    ) { prefs, guide, tip ->
        SettingsUiState(
            preferences = prefs,
            showGuideDialog = guide,
            showFirstTimeTip = tip
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setCustomPaletteIndex(index: Int) {
        viewModelScope.launch {
            settingsRepository.setCustomPaletteIndex(index)
        }
    }

    fun setLanguage(tag: String) {
        viewModelScope.launch {
            settingsRepository.setLanguageTag(tag)
            val appLocales = LocaleListCompat.forLanguageTags(tag)
            AppCompatDelegate.setApplicationLocales(appLocales)
        }
    }

    fun openGuideDialog() {
        showGuideDialog.value = true
    }

    fun closeGuideDialog() {
        showGuideDialog.value = false
    }

    fun dismissTip() {
        tipsPreferences.dismissTip("settings")
        showTip.value = false
    }
}
