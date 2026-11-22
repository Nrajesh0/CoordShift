package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CoordShiftUiState
import com.example.data.DistanceUnit
import com.example.data.EvaluatedCoord
import com.example.data.RouteLeg
import com.example.domain.CooldownCalculator
import com.example.ui.components.AnchorModeBar
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.accentFor
import com.example.ui.components.containerFor
import com.example.ui.theme.LocalCooldownPalette
import com.example.ui.theme.MonoStyles
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

@Composable
fun RouteRadarScreen(
    uiState: CoordShiftUiState,
    onApplyOptimalOrder: () -> Unit,
    onPromoteCoordinate: (String) -> Unit,
    onSwitchToPermanent: (Boolean) -> Unit,
    onSwitchToTemp: () -> Unit,
    onGoToCompare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCooldownPalette.current
    val route = uiState.routeSummary

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
            item(key = "radar_header") {
                Column {
                    Text(
                        text = "Radar",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Synced with Compare — tap any spot to switch",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.permanentCoord != null) {
                item(key = "radar_anchor_bar") {
                    AnchorModeBar(
                        uiState = uiState,
                        onSwitchToPermanent = onSwitchToPermanent,
                        onSwitchToTemp = onSwitchToTemp,
                        onOpenSetPermanentDialog = onGoToCompare
                    )
                }
            }

            if (uiState.evaluatedCoords.size < 2 || route == null) {
                item(key = "radar_empty") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, palette.hairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Radar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Add at least 2 coordinates",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Once you have 2+ coordinates in Compare, Radar plots your Permanent Base, Current Anchor, Last Location, and all Temp coordinates in real time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onGoToCompare,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("radar_goto_compare_button")
                            ) {
                                Text("Go to Compare")
                            }
                        }
                    }
                }
            } else {
                item(key = "spatial_radar") {
                    SpatialRadarCard(
                        evaluatedCoords = uiState.evaluatedCoords,
                        distanceUnit = uiState.distanceUnit,
                        onPromoteCoordinate = onPromoteCoordinate
                    )
                }

                item(key = "route_optimizer") {
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
                                Text(
                                    text = "MULTI-HOP SEQUENCE",
                                    style = MonoStyles.badge,
                                    color = palette.mutedText
                                )
                                if (route.isAlreadyOptimal) {
                                    StatusPillBadge(
                                        text = "OPTIMAL ORDER",
                                        accentColor = palette.fast,
                                        containerColor = palette.fastContainer,
                                        showDot = false
                                    )
                                } else {
                                    StatusPillBadge(
                                        text = "SAVE ${
                                            CooldownCalculator.formatCooldown(route.savedCooldownSeconds)
                                        }",
                                        accentColor = palette.fast,
                                        containerColor = palette.fastContainer,
                                        showDot = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RouteStatTile(
                                    title = "CURRENT ORDER",
                                    cooldownText = if (route.currentTotalCooldownSeconds == 0) {
                                        "Ready"
                                    } else {
                                        CooldownCalculator.formatCooldown(route.currentTotalCooldownSeconds)
                                    },
                                    distanceText = CooldownCalculator.formatDistance(
                                        route.currentTotalDistanceKm,
                                        uiState.distanceUnit
                                    ),
                                    accent = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                RouteStatTile(
                                    title = "OPTIMAL HOP",
                                    cooldownText = if (route.optimalTotalCooldownSeconds == 0) {
                                        "Ready"
                                    } else {
                                        CooldownCalculator.formatCooldown(route.optimalTotalCooldownSeconds)
                                    },
                                    distanceText = CooldownCalculator.formatDistance(
                                        route.optimalTotalDistanceKm,
                                        uiState.distanceUnit
                                    ),
                                    accent = palette.fast,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (!route.isAlreadyOptimal) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onApplyOptimalOrder,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("apply_optimal_route_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoFixHigh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reorder Compare List to Optimal Path")
                                }
                            } else {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = palette.fast,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Your list is already in the lowest-wait jump sequence.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "legs_title") {
                    Text(
                        text = "OPTIMAL HOP BREAKDOWN",
                        style = MonoStyles.badge,
                        color = palette.mutedText,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                itemsIndexed(route.optimalOrderLegs) { idx, leg ->
                    RouteLegRow(
                        stepNumber = idx + 1,
                        leg = leg,
                        distanceUnit = uiState.distanceUnit
                    )
                }
            }
        }
    }
}

@Composable
private fun SpatialRadarCard(
    evaluatedCoords: List<EvaluatedCoord>,
    distanceUnit: DistanceUnit,
    onPromoteCoordinate: (String) -> Unit
) {
    val palette = LocalCooldownPalette.current
    val textMeasurer = rememberTextMeasurer()
    val anchor = evaluatedCoords.first()
    val candidates = evaluatedCoords.drop(1)
    val surfaceBg = MaterialTheme.colorScheme.surface
    val gridColor = palette.hairline
    val anchorColor = if (anchor.isPermanent) palette.fast else MaterialTheme.colorScheme.secondary
    val permColor = MaterialTheme.colorScheme.secondary
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val pinBadgeStyle = MonoStyles.badge

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surfaceBg,
        border = BorderStroke(1.dp, palette.hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${if (anchor.isPermanent) "PERMANENT" else "TEMP"} CENTER · ${anchor.point.label.uppercase()}",
                        style = MonoStyles.badge,
                        color = anchorColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tap any coordinate below to set as Current Anchor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPillBadge(
                    text = "N ↑",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    showDot = false
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                    .border(1.dp, palette.hairline.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = (minOf(size.width, size.height) / 2f) * 0.88f
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    val ringFractions = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                    ringFractions.forEachIndexed { idx, frac ->
                        drawCircle(
                            color = gridColor.copy(alpha = if (idx == 3) 0.85f else 0.5f),
                            radius = maxRadius * frac,
                            center = center,
                            style = Stroke(width = 1.dp.toPx(), pathEffect = if (idx < 3) dashEffect else null)
                        )
                    }

                    drawLine(
                        color = gridColor.copy(alpha = 0.45f),
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = gridColor.copy(alpha = 0.45f),
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1.dp.toPx()
                    )

                    val maxLog = ln(1500.0 + 1.0)
                    candidates.forEachIndexed { idx, cand ->
                        val slotNum = idx + 2
                        val normDist = (ln(cand.distanceKm.coerceAtLeast(0.2) + 1.0) / maxLog)
                            .toFloat()
                            .coerceIn(0.16f, 0.94f)
                        val rad = Math.toRadians(cand.bearingDegrees - 90.0)
                        val ptX = center.x + (cos(rad) * maxRadius * normDist).toFloat()
                        val ptY = center.y + (sin(rad) * maxRadius * normDist).toFloat()
                        val dotColor = if (cand.isPermanent) permColor else palette.accentFor(cand.tier)

                        drawLine(
                            color = dotColor.copy(alpha = if (cand.isBestPick || cand.isPermanent) 0.75f else 0.35f),
                            start = center,
                            end = Offset(ptX, ptY),
                            strokeWidth = if (cand.isBestPick || cand.isPermanent) 2.2.dp.toPx() else 1.2.dp.toPx(),
                            pathEffect = if (cand.isPermanent) dashEffect else null
                        )

                        if (cand.isPermanent) {
                            // Distinct glowing diamond marker for Permanent Base
                            val r = 8.dp.toPx()
                            val diamond = Path().apply {
                                moveTo(ptX, ptY - r)
                                lineTo(ptX + r, ptY)
                                lineTo(ptX, ptY + r)
                                lineTo(ptX - r, ptY)
                                close()
                            }
                            drawPath(path = diamond, color = permColor)
                        } else {
                            if (cand.isBestPick) {
                                drawCircle(
                                    color = dotColor.copy(alpha = 0.25f),
                                    radius = 12.dp.toPx(),
                                    center = Offset(ptX, ptY)
                                )
                            }
                            drawCircle(
                                color = dotColor,
                                radius = if (cand.isBestPick) 6.dp.toPx() else 4.5.dp.toPx(),
                                center = Offset(ptX, ptY)
                            )
                        }

                        val tagText = when {
                            cand.isPermanent -> "PERM"
                            cand.isLastLocation -> "LAST"
                            else -> "#$slotNum"
                        }
                        val layoutResult = textMeasurer.measure(
                            text = tagText,
                            style = pinBadgeStyle.copy(color = dotColor)
                        )
                        val textX = (ptX + 9.dp.toPx())
                            .coerceAtMost(size.width - layoutResult.size.width)
                        val textY = (ptY - layoutResult.size.height / 2f)
                            .coerceIn(0f, (size.height - layoutResult.size.height).coerceAtLeast(0f))
                        drawText(
                            textLayoutResult = layoutResult,
                            topLeft = Offset(textX, textY)
                        )
                    }

                    // Center Anchor Pulse
                    drawCircle(
                        color = anchorColor.copy(alpha = 0.22f),
                        radius = 14.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = anchorColor,
                        radius = 6.dp.toPx(),
                        center = center
                    )
                }

                Text(
                    text = "N",
                    style = MonoStyles.badge,
                    color = labelTextColor,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                )
                Text(
                    text = "S",
                    style = MonoStyles.badge,
                    color = labelTextColor,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                )
                Text(
                    text = "W",
                    style = MonoStyles.badge,
                    color = labelTextColor,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                )
                Text(
                    text = "E",
                    style = MonoStyles.badge,
                    color = labelTextColor,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                candidates.forEachIndexed { idx, cand ->
                    val slotNum = idx + 2
                    val accent = if (cand.isPermanent) permColor else palette.accentFor(cand.tier)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPromoteCoordinate(cand.point.id) }
                            .padding(horizontal = 6.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (cand.isPermanent) {
                                Box(
                                    modifier = Modifier.width(34.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = "Permanent Base",
                                        tint = permColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(permColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Text(
                                    text = if (cand.isLastLocation) "LAST" else "#$slotNum",
                                    style = MonoStyles.badge,
                                    color = accent,
                                    modifier = Modifier.width(34.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = cand.point.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val waitText = if (cand.remainingCooldownSeconds == 0) {
                            "Ready"
                        } else {
                            CooldownCalculator.formatCooldown(cand.remainingCooldownSeconds)
                        }
                        Text(
                            text = "${cand.cardinalDirection} · ${
                                CooldownCalculator.formatDistance(cand.distanceKm, distanceUnit)
                            } · $waitText",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteStatTile(
    title: String,
    cooldownText: String,
    distanceText: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalCooldownPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            style = MonoStyles.badge,
            color = palette.mutedText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = cooldownText,
            style = MonoStyles.metricLarge,
            color = accent
        )
        Text(
            text = distanceText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RouteLegRow(
    stepNumber: Int,
    leg: RouteLeg,
    distanceUnit: DistanceUnit
) {
    val palette = LocalCooldownPalette.current
    val accent = palette.accentFor(leg.tier)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, palette.hairline),
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
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(palette.containerFor(leg.tier)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$stepNumber",
                        style = MonoStyles.badge,
                        color = accent
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = leg.from.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = palette.mutedText,
                            modifier = Modifier
                                .padding(horizontal = 5.dp)
                                .size(13.dp)
                        )
                        Text(
                            text = leg.to.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = CooldownCalculator.formatDistance(leg.distanceKm, distanceUnit),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusPillBadge(
                text = if (leg.cooldownSeconds == 0) {
                    "Ready"
                } else {
                    CooldownCalculator.formatCooldown(leg.cooldownSeconds)
                },
                accentColor = accent,
                containerColor = palette.containerFor(leg.tier),
                showDot = false
            )
        }
    }
}
