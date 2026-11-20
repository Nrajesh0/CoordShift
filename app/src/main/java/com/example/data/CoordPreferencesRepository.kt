package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.coordShiftDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "coordshift_prefs"
)

data class PersistedSession(
    val permanentCoord: CoordPoint?,
    val distanceUnit: DistanceUnit,
    val themeMode: ThemeMode,
    val safetyBufferMinutes: Int,
    val lastActionEpochMillis: Long?,
    val timerState: TimerState,
    val notificationPrefs: NotificationPreferences
)

/**
 * Persists the user's Permanent Base coordinate, preferences, notification settings,
 * and active countdown timer. Temporary comparison coordinates stay cleanly in-memory.
 */
class CoordPreferencesRepository(private val context: Context) {

    private object Keys {
        val PERMANENT_COORD_JSON = stringPreferencesKey("permanent_coord_json")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SAFETY_BUFFER_MIN = intPreferencesKey("safety_buffer_min")
        val LAST_ACTION_EPOCH = longPreferencesKey("last_action_epoch")
        val TIMER_LABEL = stringPreferencesKey("timer_label")
        val TIMER_TOTAL_SEC = intPreferencesKey("timer_total_sec")
        val TIMER_TARGET_EPOCH = longPreferencesKey("timer_target_epoch")
        val TIMER_PAUSED_SEC = intPreferencesKey("timer_paused_sec")
        val TIMER_RUNNING = booleanPreferencesKey("timer_running")

        // Notification Center preferences
        val NOTIF_MASTER_ENABLED = booleanPreferencesKey("notif_master_enabled")
        val NOTIF_LIVE_COUNTDOWN = booleanPreferencesKey("notif_live_countdown")
        val NOTIF_SHOW_READY_CLOCK = booleanPreferencesKey("notif_show_ready_clock")
        val NOTIF_SHOW_RETURN_HOME = booleanPreferencesKey("notif_show_return_home")
        val NOTIF_READY_ALERT = booleanPreferencesKey("notif_ready_alert")
        val NOTIF_PRE_ALERT_MIN = intPreferencesKey("notif_pre_alert_min")
        val NOTIF_ALERT_STYLE = stringPreferencesKey("notif_alert_style")
        val NOTIF_VIBRATION_PATTERN = stringPreferencesKey("notif_vibration_pattern")
        val NOTIF_SHOW_LOCKSCREEN = booleanPreferencesKey("notif_show_lockscreen")
        val NOTIF_CUSTOM_MESSAGE = stringPreferencesKey("notif_custom_message")
    }

    val sessionFlow: Flow<PersistedSession> = context.coordShiftDataStore.data.map { prefs ->
        val permanent = decodeCoord(prefs[Keys.PERMANENT_COORD_JSON])
        val unit = runCatching {
            DistanceUnit.valueOf(prefs[Keys.DISTANCE_UNIT] ?: DistanceUnit.KM.name)
        }.getOrDefault(DistanceUnit.KM)
        val theme = runCatching {
            ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name)
        }.getOrDefault(ThemeMode.DARK)
        val buffer = (prefs[Keys.SAFETY_BUFFER_MIN] ?: 0).coerceIn(0, 5)
        val lastAction = prefs[Keys.LAST_ACTION_EPOCH]?.takeIf { it > 0L }
        val timer = TimerState(
            label = prefs[Keys.TIMER_LABEL].orEmpty(),
            totalDurationSeconds = prefs[Keys.TIMER_TOTAL_SEC] ?: 0,
            targetEndEpochMillis = prefs[Keys.TIMER_TARGET_EPOCH] ?: 0L,
            pausedRemainingSeconds = prefs[Keys.TIMER_PAUSED_SEC] ?: 0,
            isRunning = prefs[Keys.TIMER_RUNNING] ?: false
        )
        val alertStyle = runCatching {
            NotificationAlertStyle.valueOf(
                prefs[Keys.NOTIF_ALERT_STYLE] ?: NotificationAlertStyle.SOUND_VIBRATE.name
            )
        }.getOrDefault(NotificationAlertStyle.SOUND_VIBRATE)
        val vibPattern = runCatching {
            VibrationPattern.valueOf(
                prefs[Keys.NOTIF_VIBRATION_PATTERN] ?: VibrationPattern.DOUBLE.name
            )
        }.getOrDefault(VibrationPattern.DOUBLE)
        val notifPrefs = NotificationPreferences(
            masterEnabled = prefs[Keys.NOTIF_MASTER_ENABLED] ?: true,
            liveCountdownEnabled = prefs[Keys.NOTIF_LIVE_COUNTDOWN] ?: true,
            showReadyClock = prefs[Keys.NOTIF_SHOW_READY_CLOCK] ?: true,
            showReturnHomeInfo = prefs[Keys.NOTIF_SHOW_RETURN_HOME] ?: true,
            readyAlertEnabled = prefs[Keys.NOTIF_READY_ALERT] ?: true,
            preAlertMinutes = (prefs[Keys.NOTIF_PRE_ALERT_MIN] ?: 2).coerceIn(0, 15),
            alertStyle = alertStyle,
            vibrationPattern = vibPattern,
            showOnLockScreen = prefs[Keys.NOTIF_SHOW_LOCKSCREEN] ?: true,
            customReadyMessage = prefs[Keys.NOTIF_CUSTOM_MESSAGE]
                ?: "Cooldown complete — safe to act now!"
        )
        PersistedSession(
            permanentCoord = permanent,
            distanceUnit = unit,
            themeMode = theme,
            safetyBufferMinutes = buffer,
            lastActionEpochMillis = lastAction,
            timerState = timer,
            notificationPrefs = notifPrefs
        )
    }

    suspend fun savePermanentCoord(coord: CoordPoint?) {
        context.coordShiftDataStore.edit { prefs ->
            if (coord == null) {
                prefs.remove(Keys.PERMANENT_COORD_JSON)
            } else {
                prefs[Keys.PERMANENT_COORD_JSON] = encodeCoord(coord)
            }
        }
    }

    suspend fun saveDistanceUnit(unit: DistanceUnit) {
        context.coordShiftDataStore.edit { prefs ->
            prefs[Keys.DISTANCE_UNIT] = unit.name
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.coordShiftDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun saveSafetyBuffer(minutes: Int) {
        context.coordShiftDataStore.edit { prefs ->
            prefs[Keys.SAFETY_BUFFER_MIN] = minutes.coerceIn(0, 5)
        }
    }

    suspend fun saveLastActionEpoch(epochMillis: Long?) {
        context.coordShiftDataStore.edit { prefs ->
            if (epochMillis == null) {
                prefs.remove(Keys.LAST_ACTION_EPOCH)
            } else {
                prefs[Keys.LAST_ACTION_EPOCH] = epochMillis
            }
        }
    }

    suspend fun saveTimerState(timer: TimerState) {
        context.coordShiftDataStore.edit { prefs ->
            prefs[Keys.TIMER_LABEL] = timer.label
            prefs[Keys.TIMER_TOTAL_SEC] = timer.totalDurationSeconds
            prefs[Keys.TIMER_TARGET_EPOCH] = timer.targetEndEpochMillis
            prefs[Keys.TIMER_PAUSED_SEC] = timer.pausedRemainingSeconds
            prefs[Keys.TIMER_RUNNING] = timer.isRunning
        }
    }

    suspend fun saveNotificationPreferences(np: NotificationPreferences) {
        context.coordShiftDataStore.edit { prefs ->
            prefs[Keys.NOTIF_MASTER_ENABLED] = np.masterEnabled
            prefs[Keys.NOTIF_LIVE_COUNTDOWN] = np.liveCountdownEnabled
            prefs[Keys.NOTIF_SHOW_READY_CLOCK] = np.showReadyClock
            prefs[Keys.NOTIF_SHOW_RETURN_HOME] = np.showReturnHomeInfo
            prefs[Keys.NOTIF_READY_ALERT] = np.readyAlertEnabled
            prefs[Keys.NOTIF_PRE_ALERT_MIN] = np.preAlertMinutes.coerceIn(0, 15)
            prefs[Keys.NOTIF_ALERT_STYLE] = np.alertStyle.name
            prefs[Keys.NOTIF_VIBRATION_PATTERN] = np.vibrationPattern.name
            prefs[Keys.NOTIF_SHOW_LOCKSCREEN] = np.showOnLockScreen
            prefs[Keys.NOTIF_CUSTOM_MESSAGE] = np.customReadyMessage.take(80)
        }
    }

    private fun encodeCoord(pt: CoordPoint): String {
        return JSONObject()
            .put("id", pt.id)
            .put("lat", pt.latitude)
            .put("lng", pt.longitude)
            .put("label", pt.label)
            .put("createdAt", pt.createdAtMillis)
            .toString()
    }

    private fun decodeCoord(json: String?): CoordPoint? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(json)
            CoordPoint(
                id = obj.getString("id"),
                latitude = obj.getDouble("lat"),
                longitude = obj.getDouble("lng"),
                label = obj.optString("label", "Home Base"),
                createdAtMillis = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }.getOrNull()
    }
}
