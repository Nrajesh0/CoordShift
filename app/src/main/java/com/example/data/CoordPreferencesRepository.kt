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
    val timerState: TimerState
)

/**
 * Persists ONLY the user's Permanent Base coordinate, preferences, and active countdown timer.
 * Temporary comparison coordinates live cleanly in-memory for the session without polluting history.
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
        PersistedSession(
            permanentCoord = permanent,
            distanceUnit = unit,
            themeMode = theme,
            safetyBufferMinutes = buffer,
            lastActionEpochMillis = lastAction,
            timerState = timer
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
