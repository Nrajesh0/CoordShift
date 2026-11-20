package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.WeakHashMap

private object CoordShiftDataStoreHolder {
    private val instances = WeakHashMap<Context, DataStore<Preferences>>()

    fun get(context: Context): DataStore<Preferences> {
        val appContext = context.applicationContext ?: context
        return synchronized(instances) {
            instances.getOrPut(appContext) {
                PreferenceDataStoreFactory.create(
                    produceFile = { appContext.preferencesDataStoreFile("coordshift_prefs") }
                )
            }
        }
    }
}

private val Context.coordShiftDataStore: DataStore<Preferences>
    get() = CoordShiftDataStoreHolder.get(this)

data class PersistedSession(
    val permanentCoord: CoordPoint?,
    val activeCoordinates: List<CoordPoint> = emptyList(),
    val lastLocationCoord: CoordPoint? = null,
    val distanceUnit: DistanceUnit,
    val themeMode: ThemeMode,
    val safetyBufferMinutes: Int,
    val lastActionEpochMillis: Long?,
    val timerState: TimerState,
    val notificationPrefs: NotificationPreferences,
    val notificationLog: List<NotificationLogEntry> = emptyList()
)

/**
 * Persists the user's Permanent Base coordinate, active workspace coordinates (so jumped spots
 * and comparisons survive app restarts while waiting for cooldown notifications until explicitly cleared),
 * preferences, notification settings, recent alert history, and active countdown timer.
 */
class CoordPreferencesRepository(private val context: Context) {

    private object Keys {
        val PERMANENT_COORD_JSON = stringPreferencesKey("permanent_coord_json")
        val ACTIVE_COORDS_JSON = stringPreferencesKey("active_coords_json")
        val LAST_LOCATION_JSON = stringPreferencesKey("last_location_json")
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
        val NOTIF_LOG_JSON = stringPreferencesKey("notif_log_json")
    }

    val sessionFlow: Flow<PersistedSession> = context.coordShiftDataStore.data.map { prefs ->
        val permanent = decodeCoord(prefs[Keys.PERMANENT_COORD_JSON])
        val activeCoords = decodeCoordList(prefs[Keys.ACTIVE_COORDS_JSON])
        val lastLocation = decodeCoord(prefs[Keys.LAST_LOCATION_JSON])
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
        val notifLog = decodeLog(prefs[Keys.NOTIF_LOG_JSON])
        PersistedSession(
            permanentCoord = permanent,
            activeCoordinates = activeCoords,
            lastLocationCoord = lastLocation,
            distanceUnit = unit,
            themeMode = theme,
            safetyBufferMinutes = buffer,
            lastActionEpochMillis = lastAction,
            timerState = timer,
            notificationPrefs = notifPrefs,
            notificationLog = notifLog
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

    suspend fun saveActiveWorkspace(coordinates: List<CoordPoint>, lastLocation: CoordPoint?) {
        context.coordShiftDataStore.edit { prefs ->
            if (coordinates.isEmpty()) {
                prefs.remove(Keys.ACTIVE_COORDS_JSON)
            } else {
                prefs[Keys.ACTIVE_COORDS_JSON] = encodeCoordList(coordinates)
            }
            if (lastLocation == null) {
                prefs.remove(Keys.LAST_LOCATION_JSON)
            } else {
                prefs[Keys.LAST_LOCATION_JSON] = encodeCoord(lastLocation)
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

    suspend fun appendNotificationLog(entry: NotificationLogEntry) {
        context.coordShiftDataStore.edit { prefs ->
            val current = decodeLog(prefs[Keys.NOTIF_LOG_JSON])
            val updated = (listOf(entry) + current).take(15)
            prefs[Keys.NOTIF_LOG_JSON] = encodeLog(updated)
        }
    }

    suspend fun clearNotificationLog() {
        context.coordShiftDataStore.edit { prefs ->
            prefs.remove(Keys.NOTIF_LOG_JSON)
        }
    }

    private fun encodeCoord(pt: CoordPoint): String {
        return coordToJson(pt).toString()
    }

    private fun coordToJson(pt: CoordPoint): JSONObject {
        return JSONObject()
            .put("id", pt.id)
            .put("lat", pt.latitude)
            .put("lng", pt.longitude)
            .put("label", pt.label)
            .put("createdAt", pt.createdAtMillis)
    }

    private fun decodeCoord(json: String?): CoordPoint? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            jsonToCoord(JSONObject(json))
        }.getOrNull()
    }

    private fun jsonToCoord(obj: JSONObject): CoordPoint {
        return CoordPoint(
            id = obj.getString("id"),
            latitude = obj.getDouble("lat"),
            longitude = obj.getDouble("lng"),
            label = obj.optString("label", "Home Base"),
            createdAtMillis = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun encodeCoordList(coords: List<CoordPoint>): String {
        val arr = JSONArray()
        coords.forEach { pt -> arr.put(coordToJson(pt)) }
        return arr.toString()
    }

    private fun decodeCoordList(json: String?): List<CoordPoint> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    add(jsonToCoord(arr.getJSONObject(i)))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeLog(entries: List<NotificationLogEntry>): String {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("title", e.title)
                    .put("body", e.body)
                    .put("ts", e.timestampMillis)
                    .put("pre", e.isPreAlert)
            )
        }
        return arr.toString()
    }

    private fun decodeLog(json: String?): List<NotificationLogEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        NotificationLogEntry(
                            id = obj.optString("id", "$i"),
                            title = obj.optString("title", ""),
                            body = obj.optString("body", ""),
                            timestampMillis = obj.optLong("ts", 0L),
                            isPreAlert = obj.optBoolean("pre", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
