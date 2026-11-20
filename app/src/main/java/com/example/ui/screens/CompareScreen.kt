package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.CoordPoint
import com.example.data.CoordShiftUiState
import com.example.data.DistanceUnit
import com.example.data.EvaluatedCoord
import com.example.domain.CooldownCalculator
import com.example.ui.components.AnchorModeBar
import com.example.ui.components.CooldownSeverityBar
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.accentFor
import com.example.ui.components.containerFor
import com.example.ui.theme.LocalCooldownPalette
import com.example.ui.theme.MonoStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    uiState: CoordShiftUiState,
    onInputChanged: (String) -> Unit,
    onSubmitInput: (String) -> Int,
    onMoveCoordinate: (Int, Int) -> Unit,
    onPreviewAsCurrent: (String) -> Unit,
    onJumpToCoordinate: (String) -> Unit,
    onPinAsPermanent: (String) -> Unit,
    onSetPermanentFromRaw: (String, String) -> String?,
    onClearPermanentBase: () -> Unit,
    onSwitchToPermanent: (Boolean) -> Unit,
    onSwitchToTemp: () -> Unit,
    onClearTempsResetToPermanent: () -> Unit,
    onClearLastLocation: () -> Unit,
    onSortFastest: () -> Unit,
    onUpdateCoordinate: (String, String, String) -> String?,
    onDeleteCoordinate: (String) -> Unit,
    onCopySummary: () -> Unit,
    onDragPromotedToCurrent: (CoordPoint, Int) -> Unit,
    onLoadDemoTrio: () -> Unit,
    onToggleUnit: () -> Unit,
    onCycleBuffer: () -> Unit,
    onSetLastActionMinutesAgo: (Int?) -> Unit,
    onOpenTimerTab: () -> Unit,
    onToggleTimerPauseResume: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val palette = LocalCooldownPalette.current
    val listState = rememberLazyListState()

    var editingCoord by remember { mutableStateOf<CoordPoint?>(null) }
    var showPermanentDialog by remember { mutableStateOf(false) }
    var showLastActionSheet by remember { mutableStateOf(false) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableStateOf(-1) }
    var dragStartCooldownSeconds by remember { mutableStateOf(0) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var staleOffsetBeforeSwap by remember { mutableStateOf<Int?>(null) }

    val latestEvaluated by rememberUpdatedState(uiState.evaluatedCoords)

    fun readClipboardText(): String? {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = cm?.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()
    }

    fun copyToClipboard(label: String, coords: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, coords))
        onShowMessage("Copied $coords")
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Minimal 2026 Top Header & Quick Controls
            item(key = "header") {
                CompareHeader(
                    tempCount = uiState.tempCount,
                    distanceUnit = uiState.distanceUnit,
                    safetyBufferMinutes = uiState.safetyBufferMinutes,
                    elapsedSeconds = uiState.elapsedSinceLastActionSeconds,
                    onToggleUnit = onToggleUnit,
                    onCycleBuffer = onCycleBuffer,
                    onOpenLastAction = { showLastActionSheet = true }
                )
            }

            // 2. Live Mini Cooldown Timer Strip (visible whenever a timer is active)
            if (uiState.timerState.isActive) {
                item(key = "active_timer_strip") {
                    ActiveTimerMiniBar(
                        uiState = uiState,
                        onTogglePauseResume = onToggleTimerPauseResume,
                        onOpenTimerTab = onOpenTimerTab
                    )
                }
            }

            // 3. Permanent Base & Permanent <-> Temp Anchor Switcher
            item(key = "anchor_mode_bar") {
                AnchorModeBar(
                    uiState = uiState,
                    onSwitchToPermanent = { triggerTimer ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSwitchToPermanent(triggerTimer)
                    },
                    onSwitchToTemp = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSwitchToTemp()
                    },
                    onOpenSetPermanentDialog = { showPermanentDialog = true },
                    onClearTempsResetToPermanent = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClearTempsResetToPermanent()
                    },
                    onClearLastLocation = onClearLastLocation
                )
            }

            // 4. Smart Paste & Multi-Line Input Dock for Temporary Coordinates
            item(key = "paste_dock") {
                SmartPasteDock(
                    inputText = uiState.inputText,
                    errorMessage = uiState.inputError,
                    onInputChanged = onInputChanged,
                    onPasteClipboard = {
                        val text = readClipboardText()
                        if (text.isNullOrBlank()) {
                            onShowMessage("Clipboard is empty")
                        } else {
                            val added = onSubmitInput(text)
                            if (added > 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onShowMessage(
                                    if (added == 1) "Added 1 temp coordinate"
                                    else "Added $added temp coordinates"
                                )
                            }
                        }
                    },
                    onAddManual = {
                        keyboard?.hide()
                        val added = onSubmitInput(uiState.inputText)
                        if (added > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShowMessage(
                                if (added == 1) "Temp coordinate added"
                                else "Added $added temp coordinates"
                            )
                        }
                    }
                )
            }

            // 5. Telemetry Strip & Quick Sort Actions (when >= 2 coordinates)
            if (uiState.evaluatedCoords.size >= 2) {
                item(key = "telemetry_strip") {
                    ComparisonTelemetryStrip(
                        bestCandidate = uiState.bestCandidate,
                        farthestCandidate = uiState.farthestCandidate,
                        distanceUnit = uiState.distanceUnit,
                        candidateCount = uiState.evaluatedCoords.size - 1,
                        hasPermanent = uiState.permanentCoord != null,
                        hasTemps = uiState.tempCount > 0,
                        onSortFastest = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSortFastest()
                            onShowMessage("Sorted by shortest cooldown")
                        },
                        onCopySummary = onCopySummary,
                        onClearTemps = onClearTempsResetToPermanent
                    )
                }
            }

            // Drag hint banner while dragging a card
            item(key = "drag_hint") {
                AnimatedVisibility(
                    visible = draggedId != null && uiState.evaluatedCoords.size >= 2,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.fastContainer)
                            .border(1.dp, palette.fast.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapVert,
                            contentDescription = null,
                            tint = palette.fast,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Drop in top slot to compare from this coordinate",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.fast
                        )
                    }
                }
            }

            // 6. Empty State OR Interactive Draggable Coordinate Cards
            if (uiState.evaluatedCoords.isEmpty()) {
                item(key = "empty_state") {
                    EmptyCompareState(
                        onPasteClipboard = {
                            val text = readClipboardText()
                            if (text.isNullOrBlank()) {
                                onShowMessage("Clipboard is empty — paste or set Permanent Base")
                            } else {
                                val added = onSubmitInput(text)
                                if (added > 0) {
                                    onShowMessage("Added $added coordinate${if (added > 1) "s" else ""}")
                                }
                            }
                        },
                        onLoadDemo = {
                            onLoadDemoTrio()
                            onShowMessage("Loaded Permanent Base + 3 temp spots")
                        }
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.evaluatedCoords,
                    key = { _, item -> item.point.id }
                ) { index, evaluated ->
                    val isDragging = draggedId == evaluated.point.id
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.025f else 1f,
                        animationSpec = spring(stiffness = 600f),
                        label = "card_scale"
                    )

                    CoordinateCompareCard(
                        evaluated = evaluated,
                        slotNumber = index + 1,
                        distanceUnit = uiState.distanceUnit,
                        permanentLabel = uiState.permanentCoord?.label,
                        lastLocationLabel = uiState.lastLocationCoord?.label,
                        hasElapsedCredit = uiState.elapsedSinceLastActionSeconds > 0,
                        isDragging = isDragging,
                        onPreviewAsCurrent = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPreviewAsCurrent(evaluated.point.id)
                        },
                        onJumpHere = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onJumpToCoordinate(evaluated.point.id)
                        },
                        onPinAsPermanent = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPinAsPermanent(evaluated.point.id)
                        },
                        onCopy = {
                            copyToClipboard(
                                evaluated.point.label,
                                evaluated.point.formattedCoords(5)
                            )
                        },
                        onEditLabel = { editingCoord = evaluated.point },
                        onDelete = { onDeleteCoordinate(evaluated.point.id) },
                        dragHandleModifier = Modifier
                            .minimumInteractiveComponentSize()
                            .pointerInput(evaluated.point.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        val startIdx = latestEvaluated.indexOfFirst {
                                            it.point.id == evaluated.point.id
                                        }
                                        val startEval = latestEvaluated.getOrNull(startIdx)
                                        draggedId = evaluated.point.id
                                        dragStartIndex = startIdx
                                        dragStartCooldownSeconds = startEval?.remainingCooldownSeconds ?: 0
                                        dragOffsetY = 0f
                                        staleOffsetBeforeSwap = null
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y

                                        val currentList = latestEvaluated
                                        val fromIdx = currentList.indexOfFirst {
                                            it.point.id == evaluated.point.id
                                        }
                                        if (fromIdx == -1) return@detectDragGestures

                                        val visibleCoordInfos = listState.layoutInfo.visibleItemsInfo
                                            .mapNotNull { info ->
                                                val id = info.key as? String ?: return@mapNotNull null
                                                val coordIdx = currentList.indexOfFirst {
                                                    it.point.id == id
                                                }
                                                if (coordIdx >= 0) coordIdx to info else null
                                            }

                                        val currentInfo = visibleCoordInfos
                                            .firstOrNull { it.first == fromIdx }?.second
                                            ?: return@detectDragGestures

                                        if (staleOffsetBeforeSwap != null) {
                                            if (currentInfo.offset == staleOffsetBeforeSwap) {
                                                return@detectDragGestures
                                            } else {
                                                staleOffsetBeforeSwap = null
                                            }
                                        }

                                        val draggedMidY = currentInfo.offset +
                                            currentInfo.size / 2f + dragOffsetY

                                        val targetPair = visibleCoordInfos.firstOrNull { (cIdx, info) ->
                                            if (cIdx == fromIdx) return@firstOrNull false
                                            val targetMidY = info.offset + info.size / 2f
                                            if (cIdx < fromIdx) {
                                                draggedMidY < targetMidY
                                            } else {
                                                draggedMidY > targetMidY
                                            }
                                        }

                                        if (targetPair != null) {
                                            val (toIdx, targetInfo) = targetPair
                                            val stepIdx = if (toIdx < fromIdx) fromIdx - 1 else fromIdx + 1
                                            val stepInfo = visibleCoordInfos
                                                .firstOrNull { it.first == stepIdx }?.second
                                                ?: targetInfo
                                            staleOffsetBeforeSwap = currentInfo.offset
                                            onMoveCoordinate(fromIdx, stepIdx)
                                            dragOffsetY -= (stepInfo.offset - currentInfo.offset)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                    onDragEnd = {
                                        val finalIdx = latestEvaluated.indexOfFirst {
                                            it.point.id == evaluated.point.id
                                        }
                                        if (finalIdx == 0 && dragStartIndex > 0) {
                                            onDragPromotedToCurrent(
                                                evaluated.point,
                                                dragStartCooldownSeconds
                                            )
                                        }
                                        draggedId = null
                                        dragStartIndex = -1
                                        dragStartCooldownSeconds = 0
                                        dragOffsetY = 0f
                                        staleOffsetBeforeSwap = null
                                    },
                                    onDragCancel = {
                                        draggedId = null
                                        dragStartIndex = -1
                                        dragStartCooldownSeconds = 0
                                        dragOffsetY = 0f
                                        staleOffsetBeforeSwap = null
                                    }
                                )
                            },
                        modifier = Modifier
                            .then(
                                if (!isDragging) Modifier.animateItem() else Modifier
                            )
                            .zIndex(if (isDragging) 10f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }

                // Helpful 1-coordinate prompt to paste temp coords
                if (uiState.evaluatedCoords.size == 1) {
                    item(key = "add_more_hint") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, palette.hairline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Paste temporary coordinates above one by one (or bulk lines) to compare cooldowns & pick the fastest jump.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Label & Coordinates Dialog
    editingCoord?.let { target ->
        EditCoordinateDialog(
            title = if (target.id == uiState.permanentCoord?.id) "Edit Permanent Base" else "Edit Coordinate",
            initialLabel = target.label,
            initialCoords = target.formattedCoords(5),
            onDismiss = { editingCoord = null },
            onSave = { newLabel, newRawCoords ->
                val error = onUpdateCoordinate(target.id, newLabel, newRawCoords)
                if (error == null) {
                    editingCoord = null
                    onShowMessage("Updated ${newLabel.trim()}")
                }
                error
            }
        )
    }

    // Set / Edit Permanent Base Dialog
    if (showPermanentDialog) {
        SetPermanentBaseDialog(
            existing = uiState.permanentCoord,
            onPasteFromClipboard = { readClipboardText() },
            onDismiss = { showPermanentDialog = false },
            onClearPermanent = {
                onClearPermanentBase()
                showPermanentDialog = false
                onShowMessage("Removed Permanent Base")
            },
            onSave = { label, rawCoords ->
                val error = onSetPermanentFromRaw(label, rawCoords)
                if (error == null) {
                    showPermanentDialog = false
                    onShowMessage("Saved Permanent Base")
                }
                error
            }
        )
    }

    // Last Action / Elapsed Credit Bottom Sheet
    if (showLastActionSheet) {
        LastActionBottomSheet(
            elapsedSeconds = uiState.elapsedSinceLastActionSeconds,
            onDismiss = { showLastActionSheet = false },
            onSelectMinutesAgo = { mins ->
                onSetLastActionMinutesAgo(mins)
                showLastActionSheet = false
            }
        )
    }
}

@Composable
private fun ActiveTimerMiniBar(
    uiState: CoordShiftUiState,
    onTogglePauseResume: () -> Unit,
    onOpenTimerTab: () -> Unit
) {
    val palette = LocalCooldownPalette.current
    val timer = uiState.timerState
    val remaining = timer.remainingSeconds(uiState.nowEpochMillis)
    val tier = CooldownCalculator.tierForCooldownSeconds(remaining)
    val accent = palette.accentFor(tier)

    Surface(
        onClick = onOpenTimerTab,
        shape = RoundedCornerShape(14.dp),
        color = palette.containerFor(tier).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_timer_mini_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (timer.isRunning) "COOLDOWN ACTIVE · ${timer.label}" else "PAUSED · ${timer.label}",
                        style = MonoStyles.badge,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ready at ${
                            CooldownCalculator.formatReadyTime(uiState.nowEpochMillis + remaining * 1000L)
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CooldownCalculator.formatTimerClock(remaining),
                    style = MonoStyles.metricLarge,
                    color = accent
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onTogglePauseResume,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (timer.isRunning) "Pause timer" else "Resume timer",
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareHeader(
    tempCount: Int,
    distanceUnit: DistanceUnit,
    safetyBufferMinutes: Int,
    elapsedSeconds: Int,
    onToggleUnit: () -> Unit,
    onCycleBuffer: () -> Unit,
    onOpenLastAction: () -> Unit
) {
    val palette = LocalCooldownPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (tempCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusPillBadge(
                            text = "$tempCount TEMP",
                            accentColor = MaterialTheme.colorScheme.secondary,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            showDot = false
                        )
                    }
                }
                Text(
                    text = "Compare temp coords · Jump triggers cooldown & base return",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Compact technical control chips: Elapsed Time, Safety Buffer, Unit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasElapsed = elapsedSeconds > 0
            Surface(
                onClick = onOpenLastAction,
                shape = RoundedCornerShape(10.dp),
                color = if (hasElapsed) palette.fastContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (hasElapsed) palette.fast.copy(alpha = 0.45f) else palette.hairline
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("last_action_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "Last action elapsed time",
                        tint = if (hasElapsed) palette.fast else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasElapsed) {
                            "Waited ${CooldownCalculator.formatCooldown(elapsedSeconds)}"
                        } else {
                            "Last action: Now"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasElapsed) palette.fast else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                onClick = onCycleBuffer,
                shape = RoundedCornerShape(10.dp),
                color = if (safetyBufferMinutes > 0) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    if (safetyBufferMinutes > 0) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                    } else {
                        palette.hairline
                    }
                ),
                modifier = Modifier.testTag("buffer_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Safety buffer",
                        tint = if (safetyBufferMinutes > 0) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+${safetyBufferMinutes}m",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (safetyBufferMinutes > 0) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            Surface(
                onClick = onToggleUnit,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, palette.hairline),
                modifier = Modifier.testTag("unit_toggle_chip")
            ) {
                Text(
                    text = distanceUnit.shortLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun SmartPasteDock(
    inputText: String,
    errorMessage: String?,
    onInputChanged: (String) -> Unit,
    onPasteClipboard: () -> Unit,
    onAddManual: () -> Unit
) {
    val palette = LocalCooldownPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (errorMessage != null) MaterialTheme.colorScheme.error else palette.hairline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChanged,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.input_placeholder),
                            style = MonoStyles.coordinate,
                            color = palette.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    textStyle = MonoStyles.coordinate.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddManual() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = palette.hairline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("coord_input_field")
                )

                if (inputText.isNotBlank()) {
                    Button(
                        onClick = onAddManual,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_coord_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_add),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onPasteClipboard,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("paste_clipboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentPaste,
                            contentDescription = stringResource(R.string.action_paste),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_paste),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ComparisonTelemetryStrip(
    bestCandidate: EvaluatedCoord?,
    farthestCandidate: EvaluatedCoord?,
    distanceUnit: DistanceUnit,
    candidateCount: Int,
    hasPermanent: Boolean,
    hasTemps: Boolean,
    onSortFastest: () -> Unit,
    onCopySummary: () -> Unit,
    onClearTemps: () -> Unit
) {
    val palette = LocalCooldownPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, palette.hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LOWEST COOLDOWN",
                    style = MonoStyles.badge,
                    color = palette.mutedText
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (bestCandidate != null) {
                    val waitLabel = if (bestCandidate.remainingCooldownSeconds == 0) {
                        "READY NOW"
                    } else {
                        CooldownCalculator.formatCooldown(bestCandidate.remainingCooldownSeconds)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = waitLabel,
                            style = MonoStyles.metricLarge,
                            color = palette.accentFor(bestCandidate.tier)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${bestCandidate.point.label} · ${
                                CooldownCalculator.formatDistance(
                                    bestCandidate.distanceKm,
                                    distanceUnit
                                )
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    if (farthestCandidate != null && bestCandidate.savingsVsFarthestSeconds >= 60) {
                        Text(
                            text = "Saves ${
                                CooldownCalculator.formatCooldown(bestCandidate.savingsVsFarthestSeconds)
                            } vs farthest",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.fast
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (candidateCount >= 2) {
                    FilledTonalButton(
                        onClick = onSortFastest,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("sort_fastest_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = "Sort by shortest cooldown",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_sort_fastest),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                IconButton(
                    onClick = onCopySummary,
                    modifier = Modifier.testTag("copy_summary_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy comparison summary",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (hasTemps) {
                    IconButton(
                        onClick = onClearTemps,
                        modifier = Modifier.testTag("clear_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = if (hasPermanent) "Clear temporary coordinates" else "Clear all",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoordinateCompareCard(
    evaluated: EvaluatedCoord,
    slotNumber: Int,
    distanceUnit: DistanceUnit,
    permanentLabel: String?,
    lastLocationLabel: String?,
    hasElapsedCredit: Boolean,
    isDragging: Boolean,
    onPreviewAsCurrent: () -> Unit,
    onJumpHere: () -> Unit,
    onPinAsPermanent: () -> Unit,
    onCopy: () -> Unit,
    onEditLabel: () -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier
) {
    val palette = LocalCooldownPalette.current
    val tierAccent = if (evaluated.isCurrent) palette.fast else palette.accentFor(evaluated.tier)
    val tierContainer = if (evaluated.isCurrent) palette.fastContainer else palette.containerFor(evaluated.tier)

    val borderColor = when {
        isDragging -> MaterialTheme.colorScheme.primary
        evaluated.isCurrent && evaluated.isPermanent -> palette.fast.copy(alpha = 0.7f)
        evaluated.isCurrent -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        evaluated.isPermanent -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
        evaluated.isBestPick -> palette.fast.copy(alpha = 0.45f)
        else -> palette.hairline
    }

    val cardBrush = when {
        evaluated.isCurrent && evaluated.isPermanent -> Brush.linearGradient(
            colors = listOf(
                palette.fastContainer.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.surface
            )
        )
        evaluated.isCurrent -> Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.surface
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface
            )
        )
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        shadowElevation = if (isDragging) 14.dp else 0.dp,
        border = BorderStroke(if (evaluated.isCurrent || isDragging) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (evaluated.isCurrent) "current_anchor_card" else "candidate_card_$slotNumber")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dedicated tactile Drag Handle
                Box(
                    modifier = dragHandleModifier
                        .testTag("drag_handle_${evaluated.point.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = stringResource(R.string.action_drag_reorder),
                        tint = if (isDragging) MaterialTheme.colorScheme.primary else palette.mutedText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center Metadata: Badges, Label, Coordinates
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (evaluated.isCurrent) {
                            StatusPillBadge(
                                text = if (evaluated.isPermanent) "CURRENT · PERM" else "CURRENT · TEMP",
                                accentColor = if (evaluated.isPermanent) palette.fast else MaterialTheme.colorScheme.secondary,
                                containerColor = if (evaluated.isPermanent) palette.fastContainer else MaterialTheme.colorScheme.secondaryContainer,
                                showDot = true
                            )
                        } else {
                            if (evaluated.isPermanent) {
                                StatusPillBadge(
                                    text = "PERMANENT",
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    showDot = false
                                )
                            } else if (evaluated.isBestPick) {
                                StatusPillBadge(
                                    text = stringResource(R.string.badge_best),
                                    accentColor = palette.fast,
                                    containerColor = palette.fastContainer,
                                    showDot = true
                                )
                            } else {
                                Text(
                                    text = "#$slotNumber",
                                    style = MonoStyles.badge,
                                    color = palette.mutedText
                                )
                            }

                            if (evaluated.isLastLocation) {
                                StatusPillBadge(
                                    text = "LAST",
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    showDot = false
                                )
                            } else if (evaluated.remainingCooldownSeconds == 0) {
                                StatusPillBadge(
                                    text = stringResource(R.string.badge_ready),
                                    accentColor = palette.fast,
                                    containerColor = palette.fastContainer,
                                    showDot = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onEditLabel)
                    ) {
                        Text(
                            text = evaluated.point.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.action_edit_label),
                            tint = palette.mutedText,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = evaluated.point.formattedCoords(5),
                        style = MonoStyles.coordinate,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Telemetry Column
                if (evaluated.isCurrent) {
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (evaluated.isPermanent) palette.fastContainer
                                    else MaterialTheme.colorScheme.secondaryContainer
                                )
                                .border(
                                    1.dp,
                                    (if (evaluated.isPermanent) palette.fast else MaterialTheme.colorScheme.secondary)
                                        .copy(alpha = 0.45f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (evaluated.isPermanent) Icons.Filled.Home else Icons.Filled.MyLocation,
                                contentDescription = "Active current anchor",
                                tint = if (evaluated.isPermanent) palette.fast else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ORIGIN",
                            style = MonoStyles.badge,
                            color = if (evaluated.isPermanent) palette.fast else MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        val cooldownDisplay = if (evaluated.remainingCooldownSeconds == 0) {
                            "0 min"
                        } else {
                            CooldownCalculator.formatCooldown(evaluated.remainingCooldownSeconds)
                        }
                        Text(
                            text = cooldownDisplay,
                            style = MonoStyles.metricLarge,
                            color = tierAccent
                        )
                        if (hasElapsedCredit && evaluated.bufferedCooldownSeconds > evaluated.remainingCooldownSeconds) {
                            Text(
                                text = "raw ${CooldownCalculator.formatCooldown(evaluated.bufferedCooldownSeconds)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.mutedText
                            )
                        } else if (evaluated.remainingCooldownSeconds > 0) {
                            Text(
                                text = "Ready ${CooldownCalculator.formatReadyTime(evaluated.readyAtEpochMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Jump anytime",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.fast
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        CooldownSeverityBar(
                            cooldownSeconds = evaluated.remainingCooldownSeconds,
                            tier = evaluated.tier,
                            modifier = Modifier
                                .width(88.dp)
                                .height(4.dp)
                        )
                    }
                }
            }

            // Dual Reference Comparison Strip:
            // 1) When on a Temp Current -> show cooldown back to Permanent Base
            // 2) When on Permanent -> show cooldown from Last Temp Location
            val hasPermDual = evaluated.cooldownToPermanentSeconds != null && evaluated.distanceToPermanentKm != null
            val hasLastDual = evaluated.cooldownFromLastLocationSeconds != null && evaluated.distanceFromLastLocationKm != null
            if (hasPermDual || hasLastDual) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasPermDual) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "To ${permanentLabel ?: "Perm"}: ${
                                    CooldownCalculator.formatCooldown(evaluated.cooldownToPermanentSeconds!!)
                                } (${
                                    CooldownCalculator.formatDistance(
                                        evaluated.distanceToPermanentKm!!,
                                        distanceUnit
                                    )
                                })",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (hasLastDual) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Undo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val lastCd = evaluated.cooldownFromLastLocationSeconds!!
                            Text(
                                text = "From Last (${lastLocationLabel ?: "Prev"}): ${
                                    if (lastCd == 0) "Ready" else CooldownCalculator.formatCooldown(lastCd)
                                } (${
                                    CooldownCalculator.formatDistance(
                                        evaluated.distanceFromLastLocationKm!!,
                                        distanceUnit
                                    )
                                })",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = palette.hairline.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 10.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Footer Telemetry + 1-Tap Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (evaluated.isCurrent) {
                    Text(
                        text = "Measuring cooldowns from here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = CooldownCalculator.formatDistance(
                                evaluated.distanceKm,
                                distanceUnit
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Explore,
                                contentDescription = null,
                                tint = palette.mutedText,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${evaluated.cardinalDirection} ${evaluated.bearingDegrees.toInt()}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!evaluated.isCurrent) {
                        // Primary action: JUMP (switches Current & triggers cooldown automatically)
                        Surface(
                            onClick = onJumpHere,
                            shape = RoundedCornerShape(8.dp),
                            color = tierContainer,
                            border = BorderStroke(1.dp, tierAccent.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("jump_coord_${evaluated.point.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = "Jump and trigger cooldown",
                                    tint = tierAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Jump",
                                    style = MonoStyles.badge,
                                    color = tierAccent
                                )
                            }
                        }

                        // Secondary quick action: Preview as Current without starting timer
                        IconButton(
                            onClick = onPreviewAsCurrent,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("set_current_${evaluated.point.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapVert,
                                contentDescription = "Compare from here without timer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Pin as Permanent Base button on non-permanent cards
                    if (!evaluated.isPermanent) {
                        IconButton(
                            onClick = onPinAsPermanent,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("pin_permanent_${evaluated.point.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Set as Permanent Base",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("copy_coord_${evaluated.point.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = stringResource(R.string.action_copy_coords),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (!evaluated.isPermanent) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("delete_coord_${evaluated.point.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.action_delete_coord),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCompareState(
    onPasteClipboard: () -> Unit,
    onLoadDemo: () -> Unit
) {
    val palette = LocalCooldownPalette.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, palette.hairline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.fastContainer)
                    .border(1.dp, palette.fast.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = palette.fast,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Set your Permanent Base above, then paste temporary coordinates one by one. Tap Jump on the best spot to trigger cooldown & track return wait.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                listOf("lat, lng", "Bulk Lines", "Maps URL", "DMS").forEach { fmt ->
                    StatusPillBadge(
                        text = fmt,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        showDot = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPasteClipboard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("empty_paste_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste Now")
                }

                OutlinedButton(
                    onClick = onLoadDemo,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.hairline),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("load_demo_button")
                ) {
                    Text(
                        text = "Try Sample Trio",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SetPermanentBaseDialog(
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
                text = if (existing == null) "Set Permanent Base" else "Edit Permanent Base",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Saved permanently across restarts. Temporary coordinates clear cleanly back to this base.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = labelText,
                    onValueChange = {
                        labelText = it.take(32)
                        errorMsg = null
                    },
                    label = { Text("Base Label") },
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
                            text = "Remove Permanent Base",
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
                Text("Save Base")
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
private fun EditCoordinateDialog(
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
                    label = { Text("Label") },
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
                onClick = {
                    errorMsg = onSave(labelText, coordsText)
                },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LastActionBottomSheet(
    elapsedSeconds: Int,
    onDismiss: () -> Unit,
    onSelectMinutesAgo: (Int?) -> Unit
) {
    val palette = LocalCooldownPalette.current
    val presets = listOf(
        0 to "Just now (0m waited)",
        5 to "5 minutes ago",
        15 to "15 minutes ago",
        30 to "30 minutes ago",
        45 to "45 minutes ago",
        60 to "1 hour ago",
        90 to "1h 30m ago",
        120 to "2+ hours ago (All Ready)"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Time Since Last Action",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Already waited since your last interaction? We subtract elapsed time from every coordinate's cooldown automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            presets.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (mins, label) ->
                        val isSelected = (mins == 0 && elapsedSeconds == 0) ||
                            (mins > 0 && kotlin.math.abs(elapsedSeconds / 60 - mins) <= 1)
                        Surface(
                            onClick = { onSelectMinutesAgo(if (mins == 0) null else mins) },
                            shape = RoundedCornerShape(12.dp),
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
