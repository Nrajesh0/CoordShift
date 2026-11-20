package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CoordShiftUiState
import com.example.data.EvaluatedCoord
import com.example.domain.CooldownCalculator
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.accentFor
import com.example.ui.components.containerFor
import com.example.ui.theme.LocalCooldownPalette
import com.example.ui.theme.MonoStyles

@Composable
fun TimerScreen(
    uiState: CoordShiftUiState,
    onStartTimer: (Int, String) -> Unit,
    onTogglePauseResume: () -> Unit,
    onAdjustTimerSeconds: (Int) -> Unit,
    onResetTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCooldownPalette.current
    val haptic = LocalHapticFeedback.current
    val timer = uiState.timerState
    val remaining = timer.remainingSeconds(uiState.nowEpochMillis)
    val total = timer.totalDurationSeconds.coerceAtLeast(1)
    val progress = if (timer.isActive) {
        (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else if (timer.isFinished) {
        1f
    } else {
        0f
    }
    val tier = CooldownCalculator.tierForCooldownSeconds(remaining)
    val ringAccent = if (timer.isActive) palette.accentFor(tier) else palette.fast
    val trackColor = palette.hairline

    var showCustomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(timer.isFinished) {
        if (timer.isFinished) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val quickPresets = listOf(
        120 to "2m",
        600 to "10m",
        1500 to "25m",
        2700 to "45m",
        3600 to "1h",
        7200 to "2h"
    )

    val candidateTimers: List<EvaluatedCoord> = uiState.evaluatedCoords
        .filter { !it.isCurrent && it.remainingCooldownSeconds > 0 }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "timer_header") {
                Column {
                    Text(
                        text = "Cooldown Timer",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track safe jump window with exact ready-at clock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Completion Banner when timer finishes
            if (timer.isFinished) {
                item(key = "timer_complete_banner") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.fastContainer,
                        border = BorderStroke(1.dp, palette.fast.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = palette.fast,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "COOLDOWN COMPLETE",
                                        style = MonoStyles.badge,
                                        color = palette.fast
                                    )
                                    Text(
                                        text = "${timer.label.ifBlank { "Timer" }} is safe to jump now.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            TextButton(onClick = onResetTimer) {
                                Text("Dismiss", color = palette.fast)
                            }
                        }
                    }
                }
            }

            // Main Precision Dial Card
            item(key = "timer_dial") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (timer.isFinished) palette.fast.copy(alpha = 0.5f) else palette.hairline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StatusPillBadge(
                            text = when {
                                timer.isRunning -> "ACTIVE · ${timer.label.ifBlank { "COOLDOWN" }.uppercase()}"
                                timer.isActive -> "PAUSED · ${timer.label.ifBlank { "COOLDOWN" }.uppercase()}"
                                timer.isFinished -> "COMPLETE · ${timer.label.ifBlank { "READY" }.uppercase()}"
                                else -> "STANDBY · READY TO START"
                            },
                            accentColor = ringAccent,
                            containerColor = palette.containerFor(tier),
                            showDot = timer.isRunning || timer.isFinished
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Box(
                            modifier = Modifier.size(228.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 10.dp.toPx()
                                val inset = strokeWidth / 2f
                                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                val topLeft = Offset(inset, inset)

                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                if (progress > 0f) {
                                    drawArc(
                                        color = ringAccent,
                                        startAngle = -90f,
                                        sweepAngle = 360f * progress,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = CooldownCalculator.formatTimerClock(remaining),
                                    style = MonoStyles.timerDisplay,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("timer_clock_text")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (timer.isActive && remaining > 0) {
                                        "Ready at ${
                                            CooldownCalculator.formatReadyTime(
                                                uiState.nowEpochMillis + remaining * 1000L
                                            )
                                        }"
                                    } else {
                                        "Safe to jump"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (remaining == 0) palette.fast else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onResetTimer,
                                enabled = timer.isActive || timer.isFinished,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                border = BorderStroke(1.dp, palette.hairline),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("timer_reset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Reset timer",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            FilledTonalButton(
                                onClick = { onAdjustTimerSeconds(-60) },
                                enabled = timer.isActive && remaining > 60,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("timer_minus_one_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Subtract 1 minute",
                                    modifier = Modifier.size(15.dp)
                                )
                                Text("1m", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = {
                                    if (timer.isActive) {
                                        onTogglePauseResume()
                                    } else {
                                        val best = uiState.bestCandidate
                                        if (best != null && best.remainingCooldownSeconds > 0) {
                                            onStartTimer(best.remainingCooldownSeconds, best.point.label)
                                        } else {
                                            onStartTimer(600, "10m Cooldown")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("timer_primary_button")
                            ) {
                                Icon(
                                    imageVector = if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        timer.isRunning -> "Pause"
                                        timer.isActive -> "Resume"
                                        else -> "Start"
                                    },
                                    maxLines = 1
                                )
                            }

                            FilledTonalButton(
                                onClick = { onAdjustTimerSeconds(60) },
                                enabled = timer.isActive,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("timer_plus_one_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add 1 minute",
                                    modifier = Modifier.size(15.dp)
                                )
                                Text("1m", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Start directly from your compared coordinates
            if (candidateTimers.isNotEmpty()) {
                item(key = "from_coords") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, palette.hairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "START FROM COMPARED COORDINATES",
                                style = MonoStyles.badge,
                                color = palette.mutedText
                            )
                            candidateTimers.forEach { eval ->
                                val accent = palette.accentFor(eval.tier)
                                Surface(
                                    onClick = {
                                        onStartTimer(eval.remainingCooldownSeconds, eval.point.label)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(1.dp, palette.hairline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = eval.point.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = CooldownCalculator.formatDistance(
                                                    eval.distanceKm,
                                                    uiState.distanceUnit
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        StatusPillBadge(
                                            text = "START ${
                                                CooldownCalculator.formatCooldown(eval.remainingCooldownSeconds)
                                            }",
                                            accentColor = accent,
                                            containerColor = palette.containerFor(eval.tier),
                                            showDot = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick duration presets + Custom duration trigger
            item(key = "quick_presets") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
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
                            Text(
                                text = "QUICK PRESETS",
                                style = MonoStyles.badge,
                                color = palette.mutedText
                            )
                            TextButton(
                                onClick = { showCustomDialog = true },
                                modifier = Modifier.testTag("custom_timer_button")
                            ) {
                                Text("Custom…", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        quickPresets.chunked(3).forEach { rowPresets ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPresets.forEach { (sec, label) ->
                                    Surface(
                                        onClick = { onStartTimer(sec, "$label Cooldown") },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        border = BorderStroke(1.dp, palette.hairline),
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("preset_timer_$label")
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MonoStyles.coordinate,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomTimerDialog(
            onDismiss = { showCustomDialog = false },
            onConfirmMinutes = { minutes ->
                onStartTimer(minutes * 60, "${minutes}m Custom")
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun CustomTimerDialog(
    onDismiss: () -> Unit,
    onConfirmMinutes: (Int) -> Unit
) {
    var input by remember { mutableStateOf("19") }
    val parsed = input.toIntOrNull()
    val valid = parsed != null && parsed in 1..240

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Cooldown Timer", style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter(Char::isDigit).take(3) },
                label = { Text("Minutes (1 – 240)") },
                textStyle = MonoStyles.coordinate,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_timer_minutes_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { parsed?.let(onConfirmMinutes) },
                enabled = valid,
                modifier = Modifier.testTag("custom_timer_start_button")
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
