package ir.miladmedia.fdplayer.domain.usecase

import ir.miladmedia.fdplayer.domain.model.BluetoothRule
import java.util.Calendar
import javax.inject.Inject

class MatchBluetoothRuleUseCase @Inject constructor() {

    operator fun invoke(
        rules: List<BluetoothRule>,
        connectedAddress: String?,
        calendar: Calendar = Calendar.getInstance()
    ): BluetoothRule? {
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val enabledRules = rules.filter { it.enabled }

        // Prefer device-specific matching rules before wildcard (all-devices) rules
        val specificMatch = enabledRules.firstOrNull { rule ->
            !rule.deviceAddress.isNullOrBlank() &&
                rule.deviceAddress.equals(connectedAddress, ignoreCase = true) &&
                matchesDayAndTime(rule, currentDay, currentMinutes)
        }
        if (specificMatch != null) return specificMatch

        return enabledRules.firstOrNull { rule ->
            rule.deviceAddress.isNullOrBlank() &&
                matchesDayAndTime(rule, currentDay, currentMinutes)
        }
    }

    private fun matchesDayAndTime(
        rule: BluetoothRule,
        currentDay: Int,
        currentMinutes: Int
    ): Boolean {
        if (rule.weekdays.isNotEmpty() && currentDay !in rule.weekdays) {
            return false
        }
        val startMin = rule.startHour * 60 + rule.startMinute
        val endMin = rule.endHour * 60 + rule.endMinute

        return when {
            startMin == endMin -> true // Full 24h window when start == end
            startMin < endMin -> currentMinutes in startMin..endMin
            else -> currentMinutes >= startMin || currentMinutes <= endMin // Overnight window
        }
    }
}
