package com.example.data

import androidx.compose.runtime.Immutable

enum class AppTab(val route: String) {
    COMPARE("compare"),
    RADAR("radar"),
    TIMER("timer"),
    CHART("chart")
}

enum class DistanceUnit(val shortLabel: String) {
    KM("km"),
    MI("mi")
}

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}

enum class CooldownTier {
    READY,
    FAST,
    MODERATE,
    LONG,
    MAX
}

@Immutable
data class CoordPoint(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun formattedCoords(decimals: Int = 5): String {
        return "%.${decimals}f, %.${decimals}f".format(latitude, longitude)
    }
}

@Immutable
data class EvaluatedCoord(
    val point: CoordPoint,
    val isCurrent: Boolean,
    val isPermanent: Boolean,
    val isLastLocation: Boolean,
    val isBestPick: Boolean,
    val distanceKm: Double,
    val bearingDegrees: Double,
    val cardinalDirection: String,
    val rawCooldownSeconds: Int,
    val bufferedCooldownSeconds: Int,
    val remainingCooldownSeconds: Int,
    val readyAtEpochMillis: Long,
    val chartLabel: String,
    val tier: CooldownTier,
    val savingsVsFarthestSeconds: Int,
    // Secondary reference comparisons:
    val distanceToPermanentKm: Double? = null,
    val cooldownToPermanentSeconds: Int? = null,
    val distanceFromLastLocationKm: Double? = null,
    val cooldownFromLastLocationSeconds: Int? = null
)

@Immutable
data class CooldownChartRow(
    val distanceKm: Double,
    val minutes: Double,
    val displayCooldown: String,
    val isLessThan: Boolean = false
)

@Immutable
data class RouteLeg(
    val from: CoordPoint,
    val to: CoordPoint,
    val distanceKm: Double,
    val cooldownSeconds: Int,
    val tier: CooldownTier
)

@Immutable
data class RouteSummary(
    val currentOrderLegs: List<RouteLeg>,
    val currentTotalDistanceKm: Double,
    val currentTotalCooldownSeconds: Int,
    val optimalOrderPoints: List<CoordPoint>,
    val optimalOrderLegs: List<RouteLeg>,
    val optimalTotalDistanceKm: Double,
    val optimalTotalCooldownSeconds: Int
) {
    val savedCooldownSeconds: Int
        get() = (currentTotalCooldownSeconds - optimalTotalCooldownSeconds).coerceAtLeast(0)
    val isAlreadyOptimal: Boolean
        get() = savedCooldownSeconds <= 30
}

@Immutable
data class TimerState(
    val label: String = "",
    val totalDurationSeconds: Int = 0,
    val targetEndEpochMillis: Long = 0L,
    val pausedRemainingSeconds: Int = 0,
    val isRunning: Boolean = false
) {
    fun remainingSeconds(nowMillis: Long): Int {
        if (!isRunning) return pausedRemainingSeconds.coerceAtLeast(0)
        val diffMs = targetEndEpochMillis - nowMillis
        return if (diffMs <= 0L) 0 else ((diffMs + 999L) / 1000L).toInt()
    }

    val isActive: Boolean
        get() = totalDurationSeconds > 0 && (isRunning || pausedRemainingSeconds > 0)

    val isFinished: Boolean
        get() = totalDurationSeconds > 0 && !isRunning && pausedRemainingSeconds == 0
}

@Immutable
data class JumpOutcome(
    val destination: CoordPoint,
    val jumpCooldownSeconds: Int,
    val returnToPermanentSeconds: Int?
)

@Immutable
data class CoordShiftUiState(
    val selectedTab: AppTab = AppTab.COMPARE,
    /** Saved persistently across app launches. Never wiped by Clear Temps. */
    val permanentCoord: CoordPoint? = null,
    /** Active ordered list shown in Compare & Radar (Slot 0 = Active Current Anchor). */
    val coordinates: List<CoordPoint> = emptyList(),
    /** Remembers the last active Temp location when switching back to Permanent so dual comparison works. */
    val lastLocationCoord: CoordPoint? = null,
    val evaluatedCoords: List<EvaluatedCoord> = emptyList(),
    val routeSummary: RouteSummary? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val safetyBufferMinutes: Int = 0,
    val lastActionEpochMillis: Long? = null,
    val nowEpochMillis: Long = System.currentTimeMillis(),
    val timerState: TimerState = TimerState(),
    val inputText: String = "",
    val inputError: String? = null,
    val lastDeletedCoord: Pair<Int, CoordPoint>? = null,
    val lastClearedSnapshot: Triple<List<CoordPoint>, CoordPoint?, CoordPoint?>? = null
) {
    val currentAnchor: CoordPoint?
        get() = coordinates.firstOrNull()

    val isCurrentPermanent: Boolean
        get() = permanentCoord != null && currentAnchor?.id == permanentCoord.id

    val tempCount: Int
        get() = coordinates.count { it.id != permanentCoord?.id }

    val returnToPermanentInfo: EvaluatedCoord?
        get() = if (permanentCoord != null && !isCurrentPermanent) {
            evaluatedCoords.firstOrNull { it.isPermanent }
        } else {
            null
        }

    val bestCandidate: EvaluatedCoord?
        get() = evaluatedCoords.firstOrNull { it.isBestPick }

    val farthestCandidate: EvaluatedCoord?
        get() = evaluatedCoords.filterNot { it.isCurrent }.maxByOrNull { it.bufferedCooldownSeconds }

    val elapsedSinceLastActionSeconds: Int
        get() {
            val last = lastActionEpochMillis ?: return 0
            return ((nowEpochMillis - last) / 1000L).toInt().coerceAtLeast(0)
        }
}
