package com.example.data

import androidx.compose.runtime.Immutable

enum class NotificationAlertStyle(val label: String) {
    SOUND_VIBRATE("Sound + Vibe"),
    VIBRATE_ONLY("Vibrate"),
    SILENT("Silent")
}

enum class VibrationPattern(val label: String, val timings: LongArray) {
    SHORT("Short", longArrayOf(0L, 140L)),
    DOUBLE("Double", longArrayOf(0L, 140L, 100L, 180L)),
    URGENT("Urgent", longArrayOf(0L, 180L, 90L, 180L, 90L, 280L))
}

@Immutable
data class NotificationPreferences(
    val masterEnabled: Boolean = true,
    val liveCountdownEnabled: Boolean = true,
    val showReadyClock: Boolean = true,
    val showReturnHomeInfo: Boolean = true,
    val readyAlertEnabled: Boolean = true,
    /** Minutes before timer completes to fire an early heads-up alert (0 = Off). */
    val preAlertMinutes: Int = 2,
    val alertStyle: NotificationAlertStyle = NotificationAlertStyle.SOUND_VIBRATE,
    val vibrationPattern: VibrationPattern = VibrationPattern.DOUBLE,
    val showOnLockScreen: Boolean = true,
    val customReadyMessage: String = "Cooldown complete — safe to act now!"
) {
    val activeAlertSummary: String
        get() = when {
            !masterEnabled -> "Muted"
            preAlertMinutes > 0 && readyAlertEnabled -> "${preAlertMinutes}m early · Ready"
            readyAlertEnabled -> "Ready alert"
            liveCountdownEnabled -> "Live bar"
            else -> "Custom"
        }
}

@Immutable
data class NotificationLogEntry(
    val id: String,
    val title: String,
    val body: String,
    val timestampMillis: Long,
    val isPreAlert: Boolean = false
)
