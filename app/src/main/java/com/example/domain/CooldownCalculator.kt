package com.example.domain

import com.example.data.CooldownChartRow
import com.example.data.CooldownTier
import com.example.data.CoordPoint
import com.example.data.DistanceUnit
import com.example.data.RouteLeg
import com.example.data.RouteSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object CooldownCalculator {

    private const val EARTH_RADIUS_KM = 6371.0088
    private const val KM_TO_MILES = 0.6213711922

    /**
     * Exact 30-breakpoint Distance -> Cooldown reference table from the user's chart.
     */
    val referenceChart: List<CooldownChartRow> = listOf(
        CooldownChartRow(1.0, 0.5, "< 1 min", isLessThan = true),
        CooldownChartRow(2.0, 1.0, "1 min"),
        CooldownChartRow(3.0, 1.5, "< 2 min", isLessThan = true),
        CooldownChartRow(5.0, 2.0, "2 min"),
        CooldownChartRow(7.0, 5.0, "5 min"),
        CooldownChartRow(9.0, 6.5, "< 7 min", isLessThan = true),
        CooldownChartRow(10.0, 7.0, "7 min"),
        CooldownChartRow(12.0, 8.0, "8 min"),
        CooldownChartRow(18.0, 10.0, "10 min"),
        CooldownChartRow(26.0, 15.0, "15 min"),
        CooldownChartRow(42.0, 19.0, "19 min"),
        CooldownChartRow(65.0, 22.0, "22 min"),
        CooldownChartRow(76.0, 24.5, "< 25 min", isLessThan = true),
        CooldownChartRow(81.0, 25.0, "25 min"),
        CooldownChartRow(100.0, 35.0, "35 min"),
        CooldownChartRow(220.0, 39.5, "< 40 min", isLessThan = true),
        CooldownChartRow(250.0, 45.0, "45 min"),
        CooldownChartRow(350.0, 50.5, "< 51 min", isLessThan = true),
        CooldownChartRow(375.0, 54.0, "54 min"),
        CooldownChartRow(460.0, 62.0, "62 min"),
        CooldownChartRow(500.0, 64.5, "< 65 min", isLessThan = true),
        CooldownChartRow(565.0, 69.0, "69 min"),
        CooldownChartRow(700.0, 78.0, "78 min"),
        CooldownChartRow(800.0, 84.0, "84 min"),
        CooldownChartRow(900.0, 92.0, "92 min"),
        CooldownChartRow(1000.0, 99.0, "99 min"),
        CooldownChartRow(1100.0, 107.0, "107 min"),
        CooldownChartRow(1200.0, 113.5, "< 114 min", isLessThan = true),
        CooldownChartRow(1300.0, 117.0, "117 min"),
        CooldownChartRow(1350.0, 120.0, "2 hours")
    )

    /**
     * Haversine great-circle distance in kilometers.
     */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(rLat1) * cos(rLat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Initial compass bearing from point 1 to point 2 in degrees [0, 360).
     */
    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLambda = Math.toRadians(lon2 - lon1)

        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        val theta = atan2(y, x)
        return ((theta * 180.0 / PI) + 360.0) % 360.0
    }

    fun cardinalDirection(bearingDegrees: Double): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (((bearingDegrees + 22.5) % 360.0) / 45.0).toInt().coerceIn(0, 7)
        return dirs[index]
    }

    /**
     * Computes exact cooldown duration in seconds using piecewise linear interpolation
     * across all 30 reference chart breakpoints, capped at 2 hours (7200s) for >= 1350 km.
     */
    fun calculateCooldownSeconds(distanceKm: Double): Int {
        if (distanceKm <= 0.05) return 0
        if (distanceKm <= 1.0) {
            return (distanceKm * 30.0).roundToInt().coerceIn(5, 30)
        }
        val last = referenceChart.last()
        if (distanceKm >= last.distanceKm) {
            return (last.minutes * 60.0).roundToInt()
        }

        for (i in 0 until referenceChart.lastIndex) {
            val lower = referenceChart[i]
            val upper = referenceChart[i + 1]
            if (distanceKm >= lower.distanceKm && distanceKm <= upper.distanceKm) {
                val fraction = (distanceKm - lower.distanceKm) / (upper.distanceKm - lower.distanceKm)
                val interpolatedMinutes = lower.minutes + fraction * (upper.minutes - lower.minutes)
                return (interpolatedMinutes * 60.0).roundToInt()
            }
        }
        return (last.minutes * 60.0).roundToInt()
    }

    /**
     * Finds the nearest chart tier row for highlighting on the reference table.
     */
    fun nearestChartRowIndex(distanceKm: Double): Int {
        if (distanceKm <= referenceChart.first().distanceKm) return 0
        if (distanceKm >= referenceChart.last().distanceKm) return referenceChart.lastIndex
        var bestIdx = 0
        var bestDiff = Double.MAX_VALUE
        referenceChart.forEachIndexed { idx, row ->
            val diff = kotlin.math.abs(row.distanceKm - distanceKm)
            if (diff < bestDiff) {
                bestDiff = diff
                bestIdx = idx
            }
        }
        return bestIdx
    }

    fun tierForCooldownSeconds(seconds: Int): CooldownTier {
        val minutes = seconds / 60.0
        return when {
            seconds <= 0 -> CooldownTier.READY
            minutes <= 10.0 -> CooldownTier.FAST
            minutes <= 35.0 -> CooldownTier.MODERATE
            minutes <= 75.0 -> CooldownTier.LONG
            else -> CooldownTier.MAX
        }
    }

    fun formatCooldown(seconds: Int): String {
        if (seconds <= 0) return "0m"
        if (seconds < 60) return "< 1 min"
        if (seconds < 600) {
            val mins = seconds / 60
            val remSec = seconds % 60
            return if (remSec == 0) "${mins} min" else "${mins}m ${remSec}s"
        }
        val totalMinutes = (seconds + 30) / 60
        return when {
            totalMinutes >= 60 -> {
                val h = totalMinutes / 60
                val m = totalMinutes % 60
                if (m == 0) "${h}h" else "${h}h ${m}m"
            }
            else -> "${totalMinutes} min"
        }
    }

    fun formatTimerClock(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val secs = safe % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, secs)
        } else {
            "%02d:%02d".format(minutes, secs)
        }
    }

    fun formatDistance(distanceKm: Double, unit: DistanceUnit): String {
        val value = if (unit == DistanceUnit.KM) distanceKm else distanceKm * KM_TO_MILES
        return when {
            value < 1.0 -> "%.2f %s".format(value, unit.shortLabel)
            value < 100.0 -> "%.1f %s".format(value, unit.shortLabel)
            else -> "%,.0f %s".format(value, unit.shortLabel)
        }
    }

    fun formatReadyTime(epochMillis: Long): String {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }

    fun formatComparisonSummary(
        evaluated: List<com.example.data.EvaluatedCoord>,
        unit: DistanceUnit
    ): String {
        if (evaluated.isEmpty()) return ""
        val anchor = evaluated.first()
        return buildString {
            appendLine("Current: ${anchor.point.label} (${anchor.point.formattedCoords(5)})")
            evaluated.drop(1).forEachIndexed { idx, item ->
                val dist = formatDistance(item.distanceKm, unit)
                val wait = if (item.remainingCooldownSeconds == 0) {
                    "READY NOW"
                } else {
                    formatCooldown(item.remainingCooldownSeconds)
                }
                val bestTag = if (item.isBestPick) " [BEST]" else ""
                appendLine("${idx + 1}. ${item.point.label} (${item.point.formattedCoords(5)}) — $dist · $wait$bestTag")
            }
        }.trim()
    }

    fun computeRouteSummary(
        points: List<CoordPoint>,
        safetyBufferMinutes: Int
    ): RouteSummary? {
        if (points.size < 2) return null

        val bufferSec = safetyBufferMinutes * 60
        val currentLegs = buildLegs(points, bufferSec)

        // Greedy nearest-neighbor shortest cooldown path starting from Current Anchor (index 0)
        val remaining = points.drop(1).toMutableList()
        val optimalOrder = mutableListOf(points.first())
        while (remaining.isNotEmpty()) {
            val last = optimalOrder.last()
            val next = remaining.minByOrNull { candidate ->
                distanceKm(last.latitude, last.longitude, candidate.latitude, candidate.longitude)
            } ?: break
            optimalOrder.add(next)
            remaining.remove(next)
        }
        val optimalLegs = buildLegs(optimalOrder, bufferSec)

        return RouteSummary(
            currentOrderLegs = currentLegs,
            currentTotalDistanceKm = currentLegs.sumOf { it.distanceKm },
            currentTotalCooldownSeconds = currentLegs.sumOf { it.cooldownSeconds },
            optimalOrderPoints = optimalOrder,
            optimalOrderLegs = optimalLegs,
            optimalTotalDistanceKm = optimalLegs.sumOf { it.distanceKm },
            optimalTotalCooldownSeconds = optimalLegs.sumOf { it.cooldownSeconds }
        )
    }

    private fun buildLegs(ordered: List<CoordPoint>, bufferSec: Int): List<RouteLeg> {
        return ordered.zipWithNext { a, b ->
            val dist = distanceKm(a.latitude, a.longitude, b.latitude, b.longitude)
            val rawCd = calculateCooldownSeconds(dist)
            val totalCd = if (rawCd > 0) (rawCd + bufferSec).coerceAtMost(7200 + bufferSec) else 0
            RouteLeg(
                from = a,
                to = b,
                distanceKm = dist,
                cooldownSeconds = totalCd,
                tier = tierForCooldownSeconds(totalCd)
            )
        }
    }
}
