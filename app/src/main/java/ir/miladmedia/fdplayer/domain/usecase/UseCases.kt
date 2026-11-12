package ir.miladmedia.fdplayer.domain.usecase

import ir.miladmedia.fdplayer.domain.model.NewDownload
import ir.miladmedia.fdplayer.domain.model.QueueResult
import ir.miladmedia.fdplayer.domain.repository.BluetoothRepository
import ir.miladmedia.fdplayer.domain.repository.DownloadRepository
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import ir.miladmedia.fdplayer.domain.util.TextNormalizer
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Layer 1 of duplicate prevention plus per-keyword quota. Returns the new id via [lastId]. */
class QueueDownloadUseCase @Inject constructor(
    private val downloads: DownloadRepository,
    private val settings: SettingsRepository
) {
    data class Outcome(val result: QueueResult, val id: Long = -1L)

    suspend operator fun invoke(
        url: String,
        title: String,
        artist: String,
        rawKeyword: String
    ): Outcome {
        val cleanUrl = url.trim()
        if (!(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://"))) {
            return Outcome(QueueResult.InvalidUrl)
        }
        val keyword = TextNormalizer.normalize(rawKeyword)
        val cap = settings.settings.first().searchLimit
        val remaining = cap - downloads.countDone(keyword) - downloads.countActive(keyword)
        if (remaining <= 0) return Outcome(QueueResult.QuotaExhausted(cap))
        if (downloads.urlExists(cleanUrl)) return Outcome(QueueResult.DuplicateUrl)
        val titleKey = TextNormalizer.titleKey(title)
        val nameKey = TextNormalizer.nameKey(title, artist)
        if (downloads.nameExists(nameKey, titleKey)) return Outcome(QueueResult.DuplicateName)
        val id = downloads.enqueue(NewDownload(cleanUrl, title, artist, keyword))
        return Outcome(QueueResult.Queued, id)
    }
}

/** Per-device schedule check. No schedules means playing is allowed. */
class IsAutoPlayAllowedUseCase @Inject constructor(
    private val bluetooth: BluetoothRepository
) {
    /** [weekday] is Calendar.DAY_OF_WEEK of the current moment, [minuteOfDay] 0..1439. */
    suspend operator fun invoke(address: String, weekday: Int, minuteOfDay: Int): Boolean {
        val schedules = bluetooth.getSchedules(address)
        if (schedules.isEmpty()) return true
        return schedules.any { s ->
            s.weekday == weekday && if (s.startMinute <= s.endMinute) {
                minuteOfDay in s.startMinute until s.endMinute
            } else {
                minuteOfDay >= s.startMinute || minuteOfDay < s.endMinute
            }
        }
    }
}
