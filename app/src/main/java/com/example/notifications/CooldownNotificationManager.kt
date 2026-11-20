package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.NotificationAlertStyle
import com.example.data.NotificationPreferences
import com.example.data.TimerState
import com.example.data.VibrationPattern
import com.example.domain.CooldownCalculator

/**
 * Manages all Android notification channels, live chronometer shade notifications,
 * scheduled pre-alert & completion alarms, and customizable alert delivery.
 */
object CooldownNotificationManager {

    const val CHANNEL_LIVE = "coordshift_live_countdown_v1"
    const val CHANNEL_ALERT_SOUND = "coordshift_alert_sound_v2"
    const val CHANNEL_ALERT_VIBRATE = "coordshift_alert_vibrate_v2"
    const val CHANNEL_ALERT_SILENT = "coordshift_alert_silent_v1"

    const val NOTIF_ID_LIVE = 4101
    const val NOTIF_ID_PRE_ALERT = 4102
    const val NOTIF_ID_READY = 4103
    const val NOTIF_ID_TEST = 4104

    const val REQ_ALARM_PRE = 5101
    const val REQ_ALARM_READY = 5102

    const val ACTION_PRE_ALERT = "com.example.action.COOLDOWN_PRE_ALERT"
    const val ACTION_READY_ALERT = "com.example.action.COOLDOWN_READY_ALERT"
    const val ACTION_PAUSE_RESUME = "com.example.action.COOLDOWN_PAUSE_RESUME"
    const val ACTION_ADD_ONE_MIN = "com.example.action.COOLDOWN_ADD_ONE_MIN"
    const val ACTION_STOP_TIMER = "com.example.action.COOLDOWN_STOP_TIMER"

    const val EXTRA_LABEL = "extra_label"
    const val EXTRA_PRE_MIN = "extra_pre_min"
    const val EXTRA_RETURN_TEXT = "extra_return_text"
    const val EXTRA_TARGET_EPOCH = "extra_target_epoch"

    private const val DEDUP_PREFS_NAME = "coordshift_notif_dedup"
    private const val KEY_LAST_PRE_EPOCH = "last_pre_epoch"
    private const val KEY_LAST_READY_EPOCH = "last_ready_epoch"

    private val dedupLock = Any()

    /**
     * Atomically marks [targetEndEpochMillis] as delivered for pre-alert or ready-alert.
     * Returns true if this caller should deliver the notification, or false if already delivered
     * (prevents duplicate alerts when foreground ticker and AlarmManager fire concurrently).
     */
    fun claimDeliverySlot(
        context: Context,
        isPreAlert: Boolean,
        targetEndEpochMillis: Long
    ): Boolean {
        if (targetEndEpochMillis <= 0L) return true
        synchronized(dedupLock) {
            val sp = context.applicationContext.getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
            val key = if (isPreAlert) KEY_LAST_PRE_EPOCH else KEY_LAST_READY_EPOCH
            val previous = sp.getLong(key, -1L)
            if (previous == targetEndEpochMillis) return false
            sp.edit().putLong(key, targetEndEpochMillis).apply()
            return true
        }
    }

    fun clearDeliverySlots(context: Context) {
        synchronized(dedupLock) {
            context.applicationContext
                .getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val liveChannel = NotificationChannel(
            CHANNEL_LIVE,
            "Live Cooldown Bar",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Silent ongoing countdown timer shown while a cooldown is active"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Vibration is handled precisely by triggerCustomVibration() so user's chosen Short/Double/Urgent
        // waveform plays cleanly without double-buzzing on Android O+.
        val soundChannel = NotificationChannel(
            CHANNEL_ALERT_SOUND,
            "Cooldown Alerts (Sound + Vibrate)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority alerts with sound and customizable vibration when cooldown is ready"
            enableVibration(false)
            setSound(defaultSound, audioAttrs)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val vibrateChannel = NotificationChannel(
            CHANNEL_ALERT_VIBRATE,
            "Cooldown Alerts (Vibrate Only)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Silent ringtone alerts with customizable vibration when cooldown is ready"
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val silentChannel = NotificationChannel(
            CHANNEL_ALERT_SILENT,
            "Cooldown Alerts (Silent)",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Visual banner notifications without sound or vibration"
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannels(
            listOf(liveChannel, soundChannel, vibrateChannel, silentChannel)
        )
    }

    /**
     * Synchronizes both the live ongoing shade notification and background scheduled alarms
     * with the current [timer] and [prefs].
     */
    fun syncTimerNotifications(
        context: Context,
        timer: TimerState,
        prefs: NotificationPreferences,
        returnHomeSummary: String? = null
    ) {
        ensureChannels(context)
        val now = System.currentTimeMillis()
        val remainingSec = timer.remainingSeconds(now)

        if (!prefs.masterEnabled || !timer.isActive || remainingSec <= 0) {
            cancelAllScheduledAndLive(context)
            return
        }

        // 1. Live ongoing shade countdown
        if (prefs.liveCountdownEnabled && hasNotificationPermission(context)) {
            postLiveCountdownNotification(context, timer, prefs, returnHomeSummary, now)
        } else {
            cancelNotification(context, NOTIF_ID_LIVE)
        }

        // 2. Background scheduled alarms (only while actively running)
        if (timer.isRunning) {
            scheduleAlarms(context, timer, prefs, returnHomeSummary, now)
        } else {
            cancelScheduledAlarms(context)
        }
    }

    fun cancelAllScheduledAndLive(context: Context) {
        cancelScheduledAlarms(context)
        cancelNotification(context, NOTIF_ID_LIVE)
    }

    fun cancelNotification(context: Context, id: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(id) }
    }

    private fun postLiveCountdownNotification(
        context: Context,
        timer: TimerState,
        prefs: NotificationPreferences,
        returnHomeSummary: String?,
        nowMillis: Long
    ) {
        if (!hasNotificationPermission(context)) return
        val remainingSec = timer.remainingSeconds(nowMillis)
        val spotLabel = timer.label.ifBlank { "Cooldown" }

        val title = if (timer.isRunning) {
            "Waiting · $spotLabel"
        } else {
            "Paused (${CooldownCalculator.formatTimerClock(remainingSec)}) · $spotLabel"
        }

        val bodyParts = buildList {
            if (prefs.showReadyClock && timer.isRunning) {
                add("Ready at ${CooldownCalculator.formatReadyTime(timer.targetEndEpochMillis)}")
            } else if (!timer.isRunning) {
                add("Tap Resume to continue countdown")
            }
            if (prefs.showReturnHomeInfo && !returnHomeSummary.isNullOrBlank()) {
                add(returnHomeSummary)
            }
        }
        val body = bodyParts.joinToString(" · ").ifBlank { "Cooldown timer active" }

        val openAppIntent = createOpenAppPendingIntent(context, 6001)
        val pauseResumeIntent = createActionPendingIntent(context, ACTION_PAUSE_RESUME, 6002, returnHomeSummary)
        val addMinIntent = createActionPendingIntent(context, ACTION_ADD_ONE_MIN, 6003, returnHomeSummary)
        val stopIntent = createActionPendingIntent(context, ACTION_STOP_TIMER, 6004, null)

        val builder = NotificationCompat.Builder(context, CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_notif_timer)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(timer.isRunning)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(
                if (prefs.showOnLockScreen) NotificationCompat.VISIBILITY_PUBLIC
                else NotificationCompat.VISIBILITY_PRIVATE
            )
            .setContentIntent(openAppIntent)
            .addAction(
                0,
                if (timer.isRunning) "Pause" else "Resume",
                pauseResumeIntent
            )
            .addAction(0, "+1m", addMinIntent)
            .addAction(0, "Stop", stopIntent)

        if (timer.isRunning) {
            builder
                .setShowWhen(true)
                .setWhen(timer.targetEndEpochMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        } else {
            builder.setShowWhen(false)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_LIVE, builder.build())
        }
    }

    private fun scheduleAlarms(
        context: Context,
        timer: TimerState,
        prefs: NotificationPreferences,
        returnHomeSummary: String?,
        nowMillis: Long
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val endMillis = timer.targetEndEpochMillis
        val spotLabel = timer.label.ifBlank { "Cooldown" }

        // Pre-alert alarm
        val preOffsetMs = prefs.preAlertMinutes * 60_000L
        val preTriggerMs = endMillis - preOffsetMs
        if (prefs.preAlertMinutes > 0 && preTriggerMs > nowMillis + 5_000L) {
            val prePi = createAlarmPendingIntent(
                context = context,
                action = ACTION_PRE_ALERT,
                requestCode = REQ_ALARM_PRE,
                label = spotLabel,
                preMinutes = prefs.preAlertMinutes,
                returnText = returnHomeSummary,
                targetEpoch = endMillis
            )
            setAlarmSafely(am, preTriggerMs, prePi)
        } else {
            cancelAlarm(context, am, ACTION_PRE_ALERT, REQ_ALARM_PRE)
        }

        // Completion alarm
        if (prefs.readyAlertEnabled && endMillis > nowMillis + 1_000L) {
            val readyPi = createAlarmPendingIntent(
                context = context,
                action = ACTION_READY_ALERT,
                requestCode = REQ_ALARM_READY,
                label = spotLabel,
                preMinutes = 0,
                returnText = returnHomeSummary,
                targetEpoch = endMillis
            )
            setAlarmSafely(am, endMillis, readyPi)
        } else {
            cancelAlarm(context, am, ACTION_READY_ALERT, REQ_ALARM_READY)
        }
    }

    private fun setAlarmSafely(am: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        runCatching {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    private fun cancelScheduledAlarms(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelAlarm(context, am, ACTION_PRE_ALERT, REQ_ALARM_PRE)
        cancelAlarm(context, am, ACTION_READY_ALERT, REQ_ALARM_READY)
    }

    private fun cancelAlarm(context: Context, am: AlarmManager, action: String, requestCode: Int) {
        val intent = Intent(context, CooldownNotificationReceiver::class.java).apply {
            this.action = action
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    fun firePreAlertNotification(
        context: Context,
        label: String,
        minutesLeft: Int,
        prefs: NotificationPreferences,
        targetEndEpochMillis: Long = 0L
    ): Boolean {
        if (!prefs.masterEnabled || !hasNotificationPermission(context)) return false
        if (!claimDeliverySlot(context, isPreAlert = true, targetEndEpochMillis = targetEndEpochMillis)) {
            return false
        }
        ensureChannels(context)
        val spot = label.ifBlank { "Cooldown" }
        val title = "${minutesLeft}m left · $spot"
        val body = "Almost ready to act — get ready for your next move."
        postAlert(context, NOTIF_ID_PRE_ALERT, title, body, prefs)
        return true
    }

    fun fireReadyAlertNotification(
        context: Context,
        label: String,
        prefs: NotificationPreferences,
        returnHomeSummary: String? = null,
        targetEndEpochMillis: Long = 0L
    ): Boolean {
        cancelNotification(context, NOTIF_ID_LIVE)
        if (!prefs.masterEnabled || !prefs.readyAlertEnabled || !hasNotificationPermission(context)) {
            return false
        }
        if (!claimDeliverySlot(context, isPreAlert = false, targetEndEpochMillis = targetEndEpochMillis)) {
            return false
        }
        ensureChannels(context)
        val spot = label.ifBlank { "Cooldown" }
        val title = "Ready · $spot"
        val customMsg = prefs.customReadyMessage.trim().ifBlank { "Cooldown complete — safe to act now!" }
        val body = if (prefs.showReturnHomeInfo && !returnHomeSummary.isNullOrBlank()) {
            "$customMsg · $returnHomeSummary"
        } else {
            customMsg
        }
        postAlert(context, NOTIF_ID_READY, title, body, prefs)
        return true
    }

    fun sendTestNotification(
        context: Context,
        prefs: NotificationPreferences
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)
        val customMsg = prefs.customReadyMessage.trim().ifBlank { "Cooldown complete — safe to act now!" }
        val styleNote = "${prefs.alertStyle.label} · ${prefs.vibrationPattern.label} pulse"
        postAlert(
            context = context,
            notifId = NOTIF_ID_TEST,
            title = "CoordShift Test Alert",
            body = "$customMsg ($styleNote)",
            prefs = prefs
        )
        return true
    }

    fun previewVibrationPattern(context: Context, pattern: VibrationPattern) {
        triggerCustomVibration(context, pattern, respectDnd = false)
    }

    private fun postAlert(
        context: Context,
        notifId: Int,
        title: String,
        body: String,
        prefs: NotificationPreferences
    ) {
        val channelId = when (prefs.alertStyle) {
            NotificationAlertStyle.SOUND_VIBRATE -> CHANNEL_ALERT_SOUND
            NotificationAlertStyle.VIBRATE_ONLY -> CHANNEL_ALERT_VIBRATE
            NotificationAlertStyle.SILENT -> CHANNEL_ALERT_SILENT
        }
        val priority = when (prefs.alertStyle) {
            NotificationAlertStyle.SILENT -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_HIGH
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notif_timer)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(
                if (prefs.showOnLockScreen) NotificationCompat.VISIBILITY_PUBLIC
                else NotificationCompat.VISIBILITY_PRIVATE
            )
            .setContentIntent(createOpenAppPendingIntent(context, notifId))

        if (prefs.alertStyle != NotificationAlertStyle.SILENT) {
            builder.setVibrate(prefs.vibrationPattern.timings)
            triggerCustomVibration(context, prefs.vibrationPattern, respectDnd = true)
        } else {
            builder.setSilent(true)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }

    private fun triggerCustomVibration(
        context: Context,
        pattern: VibrationPattern,
        respectDnd: Boolean
    ) {
        runCatching {
            if (respectDnd) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val filter = nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
                if (filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                    filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
                ) {
                    return
                }
            }
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern.timings, -1)
                }
            }
        }
    }

    private fun createOpenAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createActionPendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
        returnText: String?
    ): PendingIntent {
        val intent = Intent(context, CooldownNotificationReceiver::class.java).apply {
            this.action = action
            if (!returnText.isNullOrBlank()) {
                putExtra(EXTRA_RETURN_TEXT, returnText)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createAlarmPendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
        label: String,
        preMinutes: Int,
        returnText: String?,
        targetEpoch: Long
    ): PendingIntent {
        val intent = Intent(context, CooldownNotificationReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_PRE_MIN, preMinutes)
            putExtra(EXTRA_RETURN_TEXT, returnText)
            putExtra(EXTRA_TARGET_EPOCH, targetEpoch)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
