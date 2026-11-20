package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CoordShiftUiState
import com.example.domain.CooldownCalculator
import com.example.ui.theme.LocalCooldownPalette
import com.example.ui.theme.MonoStyles

/**
 * Shared Permanent Base & Temp/Permanent Anchor Switcher displayed on both Compare and Radar
 * so both screens stay visibly and functionally connected.
 */
@Composable
fun AnchorModeBar(
    uiState: CoordShiftUiState,
    onSwitchToPermanent: (triggerTimer: Boolean) -> Unit,
    onSwitchToTemp: () -> Unit,
    onOpenSetPermanentDialog: () -> Unit,
    onClearTempsResetToPermanent: () -> Unit,
    onClearLastLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCooldownPalette.current
    val perm = uiState.permanentCoord
    val returnInfo = uiState.returnToPermanentInfo
    val hasTemps = uiState.tempCount > 0

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (perm != null) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f) else palette.hairline
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        if (perm == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "No Permanent Base set",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Save 1 home coord or pin any temp card",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onOpenSetPermanentDialog,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("set_permanent_base_button")
                ) {
                    Text("Set Base", style = MaterialTheme.typography.labelMedium)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: Permanent Base info + Edit + Reset Temps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = "Permanent base",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PERMANENT · ",
                                    style = MonoStyles.badge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = perm.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = perm.formattedCoords(5),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenSetPermanentDialog,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("edit_permanent_base_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit permanent base",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (hasTemps || uiState.lastLocationCoord != null) {
                            Surface(
                                onClick = onClearTempsResetToPermanent,
                                shape = RoundedCornerShape(9.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.testTag("reset_to_permanent_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RestartAlt,
                                        contentDescription = "Clear temps and reset to permanent",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reset",
                                        style = MonoStyles.badge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Segmented 2-Way Anchor Switcher: Permanent <-> Temp Current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .border(1.dp, palette.hairline, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val permActive = uiState.isCurrentPermanent
                    val tempActive = !permActive && uiState.currentAnchor != null

                    Surface(
                        onClick = { onSwitchToPermanent(false) },
                        shape = RoundedCornerShape(9.dp),
                        color = if (permActive) palette.fastContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        border = if (permActive) {
                            BorderStroke(1.dp, palette.fast.copy(alpha = 0.5f))
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("switch_mode_permanent")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                tint = if (permActive) palette.fast else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Permanent",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (permActive) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (permActive) palette.fast else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        onClick = { if (hasTemps) onSwitchToTemp() },
                        enabled = hasTemps,
                        shape = RoundedCornerShape(9.dp),
                        color = if (tempActive) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        },
                        border = if (tempActive) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("switch_mode_temp")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = when {
                                    tempActive -> MaterialTheme.colorScheme.secondary
                                    hasTemps -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> palette.mutedText.copy(alpha = 0.5f)
                                },
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            val tempLabel = if (tempActive && uiState.currentAnchor != null) {
                                "Temp · ${uiState.currentAnchor!!.label}"
                            } else if (hasTemps) {
                                "Temp (${uiState.tempCount})"
                            } else {
                                "Temp (0)"
                            }
                            Text(
                                text = tempLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (tempActive) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = when {
                                    tempActive -> MaterialTheme.colorScheme.secondary
                                    hasTemps -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> palette.mutedText.copy(alpha = 0.5f)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Dynamic Return-to-Permanent OR Last Location Context Strip
                if (returnInfo != null) {
                    val accent = palette.accentFor(returnInfo.tier)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.containerFor(returnInfo.tier).copy(alpha = 0.65f))
                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COOLDOWN BACK TO PERMANENT",
                                style = MonoStyles.badge,
                                color = accent
                            )
                            Text(
                                text = "${
                                    CooldownCalculator.formatCooldown(returnInfo.remainingCooldownSeconds)
                                } · ${
                                    CooldownCalculator.formatDistance(
                                        returnInfo.distanceKm,
                                        uiState.distanceUnit
                                    )
                                }",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            onClick = { onSwitchToPermanent(true) },
                            shape = RoundedCornerShape(8.dp),
                            color = accent,
                            modifier = Modifier.testTag("return_to_permanent_jump_button")
                        ) {
                            Text(
                                text = "Jump Home",
                                style = MonoStyles.badge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else if (uiState.isCurrentPermanent && uiState.lastLocationCoord != null) {
                    val lastLoc = uiState.lastLocationCoord
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Undo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Also comparing from Last: ${lastLoc.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onClearLastLocation,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear last location reference",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
