package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CoordShiftUiState
import com.example.data.DistanceUnit
import com.example.data.ThemeMode
import com.example.domain.CooldownCalculator
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.accentFor
import com.example.ui.components.containerFor
import com.example.ui.theme.LocalCooldownPalette
import com.example.ui.theme.MonoStyles

@Composable
fun ChartScreen(
    uiState: CoordShiftUiState,
    onSetDistanceUnit: (DistanceUnit) -> Unit,
    onSetSafetyBuffer: (Int) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onStartTimerFromChart: (Int, String) -> Unit,
    onOpenNotificationCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCooldownPalette.current
    var testDistanceKm by remember { mutableFloatStateOf(42f) }

    val testRawSeconds = CooldownCalculator.calculateCooldownSeconds(testDistanceKm.toDouble())
    val testBufferedSeconds = if (testRawSeconds > 0) {
        (testRawSeconds + uiState.safetyBufferMinutes * 60)
            .coerceAtMost(7200 + uiState.safetyBufferMinutes * 60)
    } else 0
    val testTier = CooldownCalculator.tierForCooldownSeconds(testBufferedSeconds)
    val nearestRowIdx = CooldownCalculator.nearestChartRowIndex(testDistanceKm.toDouble())

    val chartRows = CooldownCalculator.referenceChart
    val halfSize = chartRows.size / 2
    val leftColumn = chartRows.take(halfSize)
    val rightColumn = chartRows.drop(halfSize)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "chart_header") {
                Column {
                    Text(
                        text = "Cooldown Chart",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Official 30-tier distance table — tap any row to inspect or time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Interactive Distance -> Cooldown Estimator
            item(key = "live_estimator") {
                val accent = palette.accentFor(testTier)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, palette.hairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "QUICK DISTANCE TESTER",
                                    style = MonoStyles.badge,
                                    color = palette.mutedText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CooldownCalculator.formatDistance(
                                        testDistanceKm.toDouble(),
                                        uiState.distanceUnit
                                    ),
                                    style = MonoStyles.metricLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusPillBadge(
                                    text = CooldownCalculator.formatCooldown(testBufferedSeconds),
                                    accentColor = accent,
                                    containerColor = palette.containerFor(testTier),
                                    showDot = true
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    onClick = {
                                        val distLabel = CooldownCalculator.formatDistance(
                                            testDistanceKm.toDouble(),
                                            uiState.distanceUnit
                                        )
                                        onStartTimerFromChart(testBufferedSeconds, "$distLabel Jump")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, palette.hairline),
                                    modifier = Modifier.testTag("chart_start_timer_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Start timer for this distance",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Start Timer",
                                            style = MonoStyles.badge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Slider(
                            value = testDistanceKm,
                            onValueChange = { testDistanceKm = it },
                            valueRange = 1f..1400f,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = palette.hairline
                            ),
                            modifier = Modifier.testTag("distance_estimator_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(1.0, 25.0, 100.0, 500.0, 1350.0).forEach { km ->
                                Text(
                                    text = CooldownCalculator.formatDistance(km, uiState.distanceUnit),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.mutedText
                                )
                            }
                        }
                    }
                }
            }

            // Full 2-Column Tappable Reference Chart matching user's uploaded reference table
            item(key = "reference_table") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, palette.hairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.fastContainer)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DIST",
                                style = MonoStyles.badge,
                                color = palette.fast,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "WAIT",
                                style = MonoStyles.badge,
                                color = palette.fast,
                                modifier = Modifier.weight(1.1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DIST",
                                style = MonoStyles.badge,
                                color = palette.fast,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "WAIT",
                                style = MonoStyles.badge,
                                color = palette.fast,
                                modifier = Modifier.weight(1.1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        for (i in 0 until halfSize) {
                            val left = leftColumn[i]
                            val right = rightColumn[i]
                            val isLeftHighlighted = nearestRowIdx == i
                            val isRightHighlighted = nearestRowIdx == (i + halfSize)
                            val stripeBg = if (i % 2 == 1) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(stripeBg)
                                    .padding(horizontal = 6.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChartCellPair(
                                    distanceKm = left.distanceKm,
                                    cooldownLabel = left.displayCooldown,
                                    unit = uiState.distanceUnit,
                                    highlighted = isLeftHighlighted,
                                    onClick = { testDistanceKm = left.distanceKm.toFloat() },
                                    modifier = Modifier.weight(2.1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                ChartCellPair(
                                    distanceKm = right.distanceKm,
                                    cooldownLabel = right.displayCooldown,
                                    unit = uiState.distanceUnit,
                                    highlighted = isRightHighlighted,
                                    onClick = { testDistanceKm = right.distanceKm.toFloat() },
                                    modifier = Modifier.weight(2.1f)
                                )
                            }
                        }
                    }
                }
            }

            // Preferences Card (Units, Safety Buffer, Appearance)
            item(key = "preferences_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, palette.hairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "PREFERENCES",
                            style = MonoStyles.badge,
                            color = palette.mutedText
                        )

                        SegmentedPrefRow(
                            label = "Distance Unit",
                            options = DistanceUnit.entries.map { it to it.shortLabel.uppercase() },
                            selected = uiState.distanceUnit,
                            onSelect = onSetDistanceUnit
                        )

                        HorizontalDivider(thickness = 1.dp, color = palette.hairline.copy(alpha = 0.6f))

                        SegmentedPrefRow(
                            label = "Safety Buffer",
                            options = listOf(0 to "0m", 1 to "+1m", 2 to "+2m", 3 to "+3m"),
                            selected = uiState.safetyBufferMinutes,
                            onSelect = onSetSafetyBuffer
                        )

                        HorizontalDivider(thickness = 1.dp, color = palette.hairline.copy(alpha = 0.6f))

                        SegmentedPrefRow(
                            label = "Appearance",
                            options = ThemeMode.entries.map { it to it.label },
                            selected = uiState.themeMode,
                            onSelect = onSetThemeMode
                        )

                        HorizontalDivider(thickness = 1.dp, color = palette.hairline.copy(alpha = 0.6f))

                        Surface(
                            onClick = onOpenNotificationCenter,
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, palette.hairline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chart_open_notifications_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = null,
                                        tint = if (uiState.notificationPrefs.masterEnabled) {
                                            palette.fast
                                        } else {
                                            palette.mutedText
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Notification Center",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = uiState.notificationPrefs.activeAlertSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open Notification Center",
                                    tint = palette.mutedText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCellPair(
    distanceKm: Double,
    cooldownLabel: String,
    unit: DistanceUnit,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCooldownPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .then(
                if (highlighted) {
                    Modifier
                        .background(palette.fastContainer)
                        .border(1.dp, palette.fast.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 7.dp)
                } else {
                    Modifier.padding(horizontal = 4.dp, vertical = 7.dp)
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (unit == DistanceUnit.KM) {
                "${distanceKm.toInt()} km"
            } else {
                CooldownCalculator.formatDistance(distanceKm, unit)
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) palette.fast else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = cooldownLabel,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (highlighted) palette.fast else MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun <T> SegmentedPrefRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    val palette = LocalCooldownPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, title) ->
                val isSelected = value == selected
                Surface(
                    onClick = { onSelect(value) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) palette.fastContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) palette.fast else palette.hairline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) palette.fast else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
