package ir.miladmedia.fdplayer.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TipsPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("tips", Context.MODE_PRIVATE)

    fun isTipDismissed(sectionKey: String): Boolean {
        return prefs.getBoolean("tip_dismissed_$sectionKey", false)
    }

    fun dismissTip(sectionKey: String) {
        prefs.edit().putBoolean("tip_dismissed_$sectionKey", true).apply()
    }
}
