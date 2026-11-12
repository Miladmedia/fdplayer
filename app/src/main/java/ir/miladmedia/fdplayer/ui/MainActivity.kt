package ir.miladmedia.fdplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import ir.miladmedia.fdplayer.domain.model.AppSettings
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import ir.miladmedia.fdplayer.ui.navigation.FdNavHost
import ir.miladmedia.fdplayer.ui.onboarding.OnboardingScreen
import ir.miladmedia.fdplayer.ui.theme.FdPlayerTheme
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            AppLocale(settings.language)
            // Defaults to true so returning users never see a flash before DataStore loads.
            val onboardingSeen by remember { settingsRepository.settings.map { it.onboardingSeen } }
                .collectAsStateWithLifecycle(initialValue = true)
            LaunchedEffect(settings.keepScreenOnDriving) {
                if (settings.keepScreenOnDriving) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            FdPlayerTheme(themeMode = settings.theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FdNavHost()
                    if (!onboardingSeen) {
                        BackHandler { markOnboardingSeen() }
                        Surface(modifier = Modifier.fillMaxSize()) {
                            OnboardingScreen(onDone = ::markOnboardingSeen)
                        }
                    }
                }
            }
            RequestRuntimePermissions()
        }
    }

    private fun markOnboardingSeen() {
        lifecycleScope.launch { settingsRepository.update { it.copy(onboardingSeen = true) } }
    }

    /** Language "" means the app default (Persian). */
    @Composable
    private fun AppLocale(language: String) {
        LaunchedEffect(language) {
            val tag = language.ifEmpty { DEFAULT_LANGUAGE }
            val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (current != tag) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
    }

    @Composable
    private fun RequestRuntimePermissions() {
        val request = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { }
        LaunchedEffect(Unit) {
            val needed = buildList {
                add(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
            }.filter {
                ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) request.launch(needed.toTypedArray())
        }
    }

    companion object {
        const val DEFAULT_LANGUAGE = "fa"
    }
}
