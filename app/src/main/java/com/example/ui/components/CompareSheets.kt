package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CoordPoint
import com.example.data.DistanceUnit
import com.example.ui.theme.LocalCooldownPalette
import com.example.ui.theme.MonoStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareOptionsSheet(
    distanceUnit: DistanceUnit,
    safetyBufferMinutes: Int,
    elapsedSeconds: Int,
    hasPermanent: Boolean,
    hasSummary: Boolean,
    onSelectMinutesAgo: (Int?) -> Unit,
    onSetSafetyBuffer: (Int) -> Unit,
    onToggleUnit: () -> Unit,
    onEditPermanent: () -> Unit,
    onCopySummary: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalCooldownPalette.current
    val elapsedPresets = listOf(
        0 to "Now (0m)",
        5 to "5m ago",
        15 to "15m ago",
        30 to "30m ago",
        60 to "1h ago",
        120 to "2h+ (Ready)"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Comparison Options",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 1. Time since last action
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TIME SINCE LAST ACTION",
                    style = MonoStyles.badge,
                    color = palette.mutedText
                )
                Text(
                    text = "Already waited? Subtracts elapsed time from every cooldown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                elapsedPresets.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (mins, label) ->
                            val isSelected = (mins == 0 && elapsedSeconds == 0) ||
                                (mins > 0 && kotlin.math.abs(elapsedSeconds / 60 - mins) <= 1)
                            Surface(
                                onClick = { onSelectMinutesAgo(if (mins == 0) null else mins) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) palette.fastContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) palette.fast else palette.hairline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) palette.fast else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = palette.hairline.copy(alpha = 0.6f))

            // 2. Safety Buffer & Distance Unit in one clean row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1.4f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SAFETY BUFFER",
                        style = MonoStyles.badge,
                        color = palette.mutedText
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 1, 2).forEach { mins ->
                            val selected = safetyBufferMinutes == mins
                            Surface(
                                onClick = { onSetSafetyBuffer(mins) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.secondary else palette.hairline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                            ) {
                                Text(
                                    text = "+${mins}m",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "UNIT",
                        style = MonoStyles.badge,
                        color = palette.mutedText
                    )
                    Surface(
                        onClick = onToggleUnit,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, palette.hairline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("unit_toggle_chip")
                    ) {
                        Text(
                            text = if (distanceUnit == DistanceUnit.KM) "KM" else "MI",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = palette.hairline.copy(alpha = 0.6f))

            // 3. Quick Utility Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEditPermanent()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.hairline),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasPermanent) "Edit Home" else "Set Home")
                }

                if (hasSummary) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onCopySummary()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.hairline),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("copy_summary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy List")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SetPermanentBaseDialog(
    existing: CoordPoint?,
    onPasteFromClipboard: () -> String?,
    onDismiss: () -> Unit,
    onClearPermanent: () -> Unit,
    onSave: (String, String) -> String?
) {
    var labelText by remember(existing) { mutableStateOf(existing?.label ?: "Home Base") }
    var coordsText by remember(existing) { mutableStateOf(existing?.formattedCoords(5) ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Set Permanent Home" else "Edit Permanent Home",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Saved permanently across restarts. Temporary spots clear back to this coordinate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = labelText,
                    onValueChange = {
                        labelText = it.take(32)
                        errorMsg = null
                    },
                    label = { Text("Name (auto-detected if left default)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("perm_label_input")
                )
                OutlinedTextField(
                    value = coordsText,
                    onValueChange = {
                        coordsText = it
                        errorMsg = null
                    },
                    label = { Text("Coordinates (lat, lng)") },
                    textStyle = MonoStyles.coordinate,
                    singleLine = true,
                    isError = errorMsg != null,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clip = onPasteFromClipboard()
                                if (!clip.isNullOrBlank()) {
                                    coordsText = clip
                                    errorMsg = null
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentPaste,
                                contentDescription = "Paste from clipboard",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("perm_coords_input")
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (existing != null) {
                    TextButton(
                        onClick = onClearPermanent,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = "Remove Home Base",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { errorMsg = onSave(labelText, coordsText) },
                enabled = coordsText.isNotBlank(),
                modifier = Modifier.testTag("perm_save_button")
            ) {
                Text("Save Home")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditCoordinateDialog(
    title: String,
    initialLabel: String,
    initialCoords: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> String?
) {
    var labelText by remember(initialLabel) { mutableStateOf(initialLabel) }
    var coordsText by remember(initialCoords) { mutableStateOf(initialCoords) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = labelText,
                    onValueChange = {
                        labelText = it.take(32)
                        errorMsg = null
                    },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input_field")
                )
                OutlinedTextField(
                    value = coordsText,
                    onValueChange = {
                        coordsText = it
                        errorMsg = null
                    },
                    label = { Text("Coordinates (lat, lng)") },
                    textStyle = MonoStyles.coordinate,
                    singleLine = true,
                    isError = errorMsg != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_coords_input_field")
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { errorMsg = onSave(labelText, coordsText) },
                enabled = labelText.isNotBlank() && coordsText.isNotBlank(),
                modifier = Modifier.testTag("rename_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
