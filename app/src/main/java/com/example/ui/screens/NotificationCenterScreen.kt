package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.CoordShiftUiState
import com.example.data.NotificationAlertStyle
import com.example.data.NotificationPreferences
import com.example.data.VibrationPattern
import com.example.domain.CooldownCalculator
import com.example.notifications.CooldownNotificationManager
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.LocalCooldownPalette
import com.example.ui.theme.MonoStyles

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationCenterScreen(
    uiState: CoordShiftUiState,
    onBack: () -> Unit,
    onUpdatePrefs: ((NotificationPreferences) -> NotificationPreferences) -> Unit,
    onSendTestAlert: () -> Boolean,
    onClearLog: () -> Unit,
    onPermissionGranted: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalCooldownPalette.current
    val prefs = uiState.notificationPrefs

    var hasPermission by remember {
        mutableStateOf(CooldownNotificationManager.hasNotificationPermission(context))
    }

    // Re-check notification permission when returning from Android System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = CooldownNotificationManager.hasNotificationPermission(context)
                if (nowGranted && !hasPermission) {
                    onPermissionGranted()
                }
                hasPermission = nowGranted
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var pendingTestAfterPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted || CooldownNotificationManager.hasNotificationPermission(context)
        if (hasPermission) {
            onPermissionGranted()
            if (pendingTestAfterPermission) {
                pendingTestAfterPermission = false
                if (onSendTestAlert()) {
                    onShowMessage("Test alert sent")
                }
            } else {
                onShowMessage("Notifications enabled")
            }
        } else {
            pendingTestAfterPermission = false
            onShowMessage("Allow notifications in Settings to receive cooldown alerts")
        }
    }

    fun openSystemNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
            }
        }
        runCatching { context.startActivity(intent) }
    }

    fun requestOrOpenSettings(sendTestOnGrant: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
            pendingTestAfterPermission = sendTestOnGrant
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openSystemNotificationSettings()
        }
    }

    val messagePresets = listOf(
        "Cooldown complete — safe to act now!",
        "Ready to jump now!",
        "Wait finished — good to go!"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Clean Top Navigation Header
            item(key = "notif_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("notif_center_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Center",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Customize cooldown alerts & live shade bar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    val active = prefs.masterEnabled && hasPermission
                    StatusPillBadge(
                        text = when {
                            !hasPermission -> "BLOCKED"
                            active -> "ARMED"
                            else -> "MUTED"
                        },
                        accentColor = when {
                            !hasPermission -> palette.long
                            active -> palette.fast
                            else -> palette.mutedText
                        },
                        containerColor = when {
                            !hasPermission -> palette.longContainer
                            active -> palette.fastContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        showDot = active
                    )
                }
            }

            // 2. System Permission Prompt (shown clearly if OS permission is off)
            if (!hasPermission) {
                item(key = "permission_banner") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.longContainer.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, palette.long.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsOff,
                                    contentDescription = null,
                                    tint = palette.long,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "System notifications are off",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Grant notification access so CoordShift can show live cooldown timers and alert you when a jump is ready.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { requestOrOpenSettings(false) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("grant_notifications_button")
                                ) {
                                    Text("Allow Notifications")
                                }
                                OutlinedButton(
                                    onClick = { openSystemNotificationSettings() },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, palette.long.copy(alpha = 0.45f))
                                ) {
                                    Text("Settings")
                                }
                            }
                        }
                    }
                }
            }

            // 3. Master Toggle Card
            item(key = "master_toggle") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (prefs.masterEnabled) palette.fast.copy(alpha = 0.45f) else palette.hairline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (prefs.masterEnabled) palette.fastContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (prefs.masterEnabled) {
                                        Icons.Filled.NotificationsActive
                                    } else {
                                        Icons.Filled.NotificationsOff
                                    },
                                    contentDescription = null,
                                    tint = if (prefs.masterEnabled) palette.fast else palette.mutedText,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Cooldown Notifications",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (prefs.masterEnabled) {
                                        "Active · ${prefs.activeAlertSummary}"
                                    } else {
                                        "All cooldown notifications paused"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = prefs.masterEnabled,
                            onCheckedChange = { enabled ->
                                onUpdatePrefs { it.copy(masterEnabled = enabled) }
                                if (enabled && !hasPermission) {
                                    requestOrOpenSettings(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = palette.fast
                            ),
                            modifier = Modifier.testTag("notif_master_switch")
                        )
                    }
                }
            }

            // 4. Live Shade Preview Card (updates live with user customizations)
            if (prefs.masterEnabled) {
                item(key = "live_preview") {
                    NotificationPreviewCard(
                        prefs = prefs,
                        spotLabel = uiState.currentAnchor?.label ?: "Yokohama, JP",
                        homeLabel = uiState.permanentCoord?.label ?: "Tokyo, JP",
                        nowMillis = uiState.nowEpochMillis
                    )
                }

                // 5. Live Shade Countdown Controls
                item(key = "live_shade_card") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, palette.hairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "LIVE STATUS BAR",
                                style = MonoStyles.badge,
                                color = palette.mutedText
                            )

                            NotifToggleRow(
                                title = "Ongoing Countdown Bar",
                                subtitle = "Live ticking timer in shade with Pause, +1m & Stop",
                                checked = prefs.liveCountdownEnabled,
                                onCheckedChange = { v ->
                                    onUpdatePrefs { it.copy(liveCountdownEnabled = v) }
                                },
                                testTag = "notif_live_bar_switch"
                            )

                            AnimatedVisibility(visible = prefs.liveCountdownEnabled) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HorizontalDivider(color = palette.hairline.copy(alpha = 0.55f))
                                    NotifToggleRow(
                                        title = "Show Ready-At Clock",
                                        subtitle = "Display exact clock time when wait ends",
                                        checked = prefs.showReadyClock,
                                        onCheckedChange = { v ->
                                            onUpdatePrefs { it.copy(showReadyClock = v) }
                                        }
                                    )
                                    HorizontalDivider(color = palette.hairline.copy(alpha = 0.55f))
                                    NotifToggleRow(
                                        title = "Show Return-to-Home Info",
                                        subtitle = "Include wait back to Permanent Home when on Temp",
                                        checked = prefs.showReturnHomeInfo,
                                        onCheckedChange = { v ->
                                            onUpdatePrefs { it.copy(showReturnHomeInfo = v) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Early Pre-Alert Reminder Card
                item(key = "pre_alert_card") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, palette.hairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EARLY HEADS-UP REMINDER",
                                    style = MonoStyles.badge,
                                    color = palette.mutedText
                                )
                                Text(
                                    text = if (prefs.preAlertMinutes == 0) "Off" else "${prefs.preAlertMinutes}m before",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (prefs.preAlertMinutes > 0) MaterialTheme.colorScheme.secondary else palette.mutedText
                                )
                            }
                            Text(
                                text = "Get a heads-up notification right before your cooldown finishes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0 to "Off", 1 to "1m", 2 to "2m", 5 to "5m", 10 to "10m").forEach { (mins, label) ->
                                    val selected = prefs.preAlertMinutes == mins
                                    Surface(
                                        onClick = { onUpdatePrefs { it.copy(preAlertMinutes = mins) } },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.secondary else palette.hairline
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("pre_alert_chip_$mins")
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.secondary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. Completion Alert & Style Customizations
                item(key = "alert_style_card") {
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
                                text = "READY ALERT & CUSTOMIZATION",
                                style = MonoStyles.badge,
                                color = palette.mutedText
                            )

                            NotifToggleRow(
                                title = "Cooldown Ready Alert",
                                subtitle = "Notify immediately when countdown reaches 00:00",
                                checked = prefs.readyAlertEnabled,
                                onCheckedChange = { v ->
                                    onUpdatePrefs { it.copy(readyAlertEnabled = v) }
                                },
                                testTag = "notif_ready_alert_switch"
                            )

                            HorizontalDivider(color = palette.hairline.copy(alpha = 0.55f))

                            // Alert delivery style
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Alert Style",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    NotificationAlertStyle.entries.forEach { style ->
                                        val selected = prefs.alertStyle == style
                                        Surface(
                                            onClick = { onUpdatePrefs { it.copy(alertStyle = style) } },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (selected) palette.fastContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) palette.fast else palette.hairline
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                                .testTag("alert_style_${style.name.lowercase()}")
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = style.label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (selected) palette.fast else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Vibration pattern (only shown when Sound+Vibe or Vibrate)
                            AnimatedVisibility(visible = prefs.alertStyle != NotificationAlertStyle.SILENT) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Vibration Pulse",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        VibrationPattern.entries.forEach { pat ->
                                            val selected = prefs.vibrationPattern == pat
                                            Surface(
                                                onClick = {
                                                    onUpdatePrefs { it.copy(vibrationPattern = pat) }
                                                    CooldownNotificationManager.previewVibrationPattern(context, pat)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (selected) MaterialTheme.colorScheme.secondary else palette.hairline
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
                                                        text = pat.label,
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = if (selected) {
                                                            MaterialTheme.colorScheme.secondary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = palette.hairline.copy(alpha = 0.55f))

                            NotifToggleRow(
                                title = "Show Details on Lock Screen",
                                subtitle = "Display spot name and timer even when locked",
                                checked = prefs.showOnLockScreen,
                                onCheckedChange = { v ->
                                    onUpdatePrefs { it.copy(showOnLockScreen = v) }
                                }
                            )

                            HorizontalDivider(color = palette.hairline.copy(alpha = 0.55f))

                            // Custom Ready Message
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Custom Ready Message",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = prefs.customReadyMessage,
                                    onValueChange = { text ->
                                        onUpdatePrefs { it.copy(customReadyMessage = text.take(80)) }
                                    },
                                    singleLine = true,
                                    placeholder = { Text("Cooldown complete — safe to act now!") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = palette.hairline
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_ready_message_input")
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    messagePresets.forEach { preset ->
                                        val isCurrent = prefs.customReadyMessage == preset
                                        Surface(
                                            onClick = {
                                                onUpdatePrefs { it.copy(customReadyMessage = preset) }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCurrent) {
                                                palette.fastContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (isCurrent) palette.fast.copy(alpha = 0.45f) else palette.hairline
                                            )
                                        ) {
                                            Text(
                                                text = preset,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCurrent) palette.fast else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. Test & System Channel Controls
                item(key = "test_and_system") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!hasPermission) {
                                    requestOrOpenSettings(sendTestOnGrant = true)
                                } else {
                                    if (onSendTestAlert()) {
                                        onShowMessage("Test notification sent")
                                    } else {
                                        onShowMessage("Enable notifications first")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("send_test_notification_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text("Send Test Alert")
                        }

                        OutlinedButton(
                            onClick = { openSystemNotificationSettings() },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, palette.hairline),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("open_os_notif_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Android notification settings",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OS Sound")
                        }
                    }
                }
            }

            // 9. Recent Alert History Log
            item(key = "recent_alerts_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, palette.hairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENT ALERTS",
                                style = MonoStyles.badge,
                                color = palette.mutedText
                            )
                            if (uiState.notificationLog.isNotEmpty()) {
                                TextButton(onClick = onClearLog) {
                                    Text("Clear", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        if (uiState.notificationLog.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = palette.mutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Triggered cooldown alerts and test notifications appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            uiState.notificationLog.forEachIndexed { idx, entry ->
                                if (idx > 0) {
                                    HorizontalDivider(color = palette.hairline.copy(alpha = 0.45f))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (entry.isPreAlert) MaterialTheme.colorScheme.secondary
                                                    else palette.fast
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(9.dp))
                                        Column {
                                            Text(
                                                text = entry.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = entry.body,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = CooldownCalculator.formatReadyTime(entry.timestampMillis),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.mutedText
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

@Composable
private fun NotificationPreviewCard(
    prefs: NotificationPreferences,
    spotLabel: String,
    homeLabel: String,
    nowMillis: Long
) {
    val palette = LocalCooldownPalette.current
    val readyTime = CooldownCalculator.formatReadyTime(nowMillis + 15 * 60_000L)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "SHADE PREVIEW",
            style = MonoStyles.badge,
            color = palette.mutedText,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, palette.hairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = palette.fast,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CoordShift · ${prefs.alertStyle.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (prefs.liveCountdownEnabled) "14:59" else "now",
                        style = MonoStyles.badge,
                        color = palette.fast
                    )
                }

                Text(
                    text = if (prefs.liveCountdownEnabled) "Waiting · $spotLabel" else "Ready · $spotLabel",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val previewBody = if (prefs.liveCountdownEnabled) {
                    buildList {
                        if (prefs.showReadyClock) add("Ready at $readyTime")
                        if (prefs.showReturnHomeInfo) add("Back to $homeLabel: 15 min")
                    }.joinToString(" · ").ifBlank { "Cooldown timer active" }
                } else {
                    prefs.customReadyMessage.ifBlank { "Cooldown complete — safe to act now!" }
                }

                Text(
                    text = previewBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (prefs.liveCountdownEnabled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        listOf("Pause", "+1m", "Stop").forEach { action ->
                            Text(
                                text = action,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotifToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    val palette = LocalCooldownPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = palette.fast
            ),
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}
