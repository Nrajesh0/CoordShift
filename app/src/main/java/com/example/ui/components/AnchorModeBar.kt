package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
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
import com.example.ui.theme.LocalCooldownPalette

/**
 * Ultra-slim 1-row Permanent <-> Temp mode switcher shared across Compare and Radar.
 * Keeps the screen calm and uncluttered while allowing 1-tap switching between Home and Temp.
 */
@Composable
fun AnchorModeBar(
    uiState: CoordShiftUiState,
    onSwitchToPermanent: (triggerTimer: Boolean) -> Unit,
    onSwitchToTemp: () -> Unit,
    onOpenSetPermanentDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCooldownPalette.current
    val perm = uiState.permanentCoord
    val hasTemps = uiState.tempCount > 0

    if (perm == null) {
        Surface(
            onClick = onOpenSetPermanentDialog,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, palette.hairline),
            modifier = modifier
                .fillMaxWidth()
                .testTag("set_permanent_base_button")
        ) {
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
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set Permanent Home",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Set →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    } else {
        val permActive = uiState.isCurrentPermanent
        val tempActive = !permActive && uiState.currentAnchor != null

        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                onClick = { onSwitchToPermanent(false) },
                shape = RoundedCornerShape(10.dp),
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
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        tint = if (permActive) palette.fast else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Home · ${perm.label}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (permActive) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (permActive) palette.fast else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                onClick = { if (hasTemps) onSwitchToTemp() },
                enabled = hasTemps,
                shape = RoundedCornerShape(10.dp),
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
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = when {
                            tempActive -> MaterialTheme.colorScheme.secondary
                            hasTemps -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> palette.mutedText.copy(alpha = 0.45f)
                        },
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val tempText = when {
                        tempActive && uiState.currentAnchor != null -> uiState.currentAnchor!!.label
                        hasTemps -> "Temp (${uiState.tempCount})"
                        else -> "Temp"
                    }
                    Text(
                        text = tempText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (tempActive) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = when {
                            tempActive -> MaterialTheme.colorScheme.secondary
                            hasTemps -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> palette.mutedText.copy(alpha = 0.45f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
