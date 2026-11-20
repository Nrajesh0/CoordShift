package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.CoordPreferencesRepository
import com.example.data.NotificationLogEntry
import com.example.data.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Receives scheduled background cooldown alarms (pre-alert & completion) as well as
 * interactive shade notification buttons (Pause/Resume, +1m, Stop).
 */
class CooldownNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = CoordPreferencesRepository(appContext)
                val session = repo.sessionFlow.first()
                val prefs = session.notificationPrefs
                val timer = session.timerState
                val now = System.currentTimeMillis()
                val returnText = intent.getStringExtra(CooldownNotificationManager.EXTRA_RETURN_TEXT)
                val targetEpoch = intent.getLongExtra(CooldownNotificationManager.EXTRA_TARGET_EPOCH, 0L)

                when (action) {
                    CooldownNotificationManager.ACTION_PRE_ALERT -> {
                        val matchesTimer = targetEpoch <= 0L || timer.targetEndEpochMillis == targetEpoch
                        if (timer.isRunning && matchesTimer && timer.remainingSeconds(now) > 0) {
                            val label = intent.getStringExtra(CooldownNotificationManager.EXTRA_LABEL)
                                ?: timer.label
                            val mins = intent.getIntExtra(
                                CooldownNotificationManager.EXTRA_PRE_MIN,
                                prefs.preAlertMinutes
                            ).coerceAtLeast(1)
                            val delivered = CooldownNotificationManager.firePreAlertNotification(
                                context = appContext,
                                label = label,
                                minutesLeft = mins,
                                prefs = prefs,
                                targetEndEpochMillis = timer.targetEndEpochMillis
                            )
                            if (delivered) {
                                repo.appendNotificationLog(
                                    NotificationLogEntry(
                                        id = UUID.randomUUID().toString(),
                                        title = "${mins}m left · ${label.ifBlank { "Cooldown" }}",
                                        body = "Early heads-up alert triggered",
                                        timestampMillis = now,
                                        isPreAlert = true
                                    )
                                )
                            }
                        }
                    }

                    CooldownNotificationManager.ACTION_READY_ALERT -> {
                        val matchesTimer = targetEpoch <= 0L || timer.targetEndEpochMillis == targetEpoch
                        if (timer.isRunning && matchesTimer) {
                            val label = intent.getStringExtra(CooldownNotificationManager.EXTRA_LABEL)
                                ?: timer.label
                            val finished = timer.copy(
                                isRunning = false,
                                pausedRemainingSeconds = 0
                            )
                            repo.saveTimerState(finished)
                            val delivered = CooldownNotificationManager.fireReadyAlertNotification(
                                context = appContext,
                                label = label,
                                prefs = prefs,
                                returnHomeSummary = returnText,
                                targetEndEpochMillis = timer.targetEndEpochMillis
                            )
                            if (delivered) {
                                repo.appendNotificationLog(
                                    NotificationLogEntry(
                                        id = UUID.randomUUID().toString(),
                                        title = "Ready · ${label.ifBlank { "Cooldown" }}",
                                        body = prefs.customReadyMessage.ifBlank {
                                            "Cooldown complete — safe to act now!"
                                        },
                                        timestampMillis = now,
                                        isPreAlert = false
                                    )
                                )
                            }
                        }
                    }

                    CooldownNotificationManager.ACTION_PAUSE_RESUME -> {
                        val updated = if (timer.isRunning) {
                            val rem = timer.remainingSeconds(now)
                            timer.copy(isRunning = false, pausedRemainingSeconds = rem)
                        } else if (timer.pausedRemainingSeconds > 0) {
                            timer.copy(
                                isRunning = true,
                                targetEndEpochMillis = now + timer.pausedRemainingSeconds * 1000L
                            )
                        } else {
                            null
                        }
                        if (updated != null) {
                            repo.saveTimerState(updated)
                            CooldownNotificationManager.syncTimerNotifications(
                                context = appContext,
                                timer = updated,
                                prefs = prefs,
                                returnHomeSummary = returnText
                            )
                        }
                    }

                    CooldownNotificationManager.ACTION_ADD_ONE_MIN -> {
                        if (timer.isActive) {
                            val updated = if (timer.isRunning) {
                                val rem = (timer.remainingSeconds(now) + 60).coerceAtLeast(10)
                                timer.copy(
                                    totalDurationSeconds = maxOf(timer.totalDurationSeconds, rem),
                                    targetEndEpochMillis = now + rem * 1000L
                                )
                            } else {
                                val rem = (timer.pausedRemainingSeconds + 60).coerceAtLeast(10)
                                timer.copy(
                                    totalDurationSeconds = maxOf(timer.totalDurationSeconds, rem),
                                    pausedRemainingSeconds = rem
                                )
                            }
                            repo.saveTimerState(updated)
                            CooldownNotificationManager.syncTimerNotifications(
                                context = appContext,
                                timer = updated,
                                prefs = prefs,
                                returnHomeSummary = returnText
                            )
                        }
                    }

                    CooldownNotificationManager.ACTION_STOP_TIMER -> {
                        val cleared = TimerState()
                        repo.saveTimerState(cleared)
                        CooldownNotificationManager.cancelAllScheduledAndLive(appContext)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
