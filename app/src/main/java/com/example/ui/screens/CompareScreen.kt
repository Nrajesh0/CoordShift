package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.CompareOptionsSheet
import com.example.ui.components.EditCoordinateDialog
import com.example.ui.components.SetPermanentBaseDialog
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.accentFor
import com.example.ui.components.containerFor
import com.example.ui.theme.LocalCooldownPalette
import com.example.ui.theme.MonoStyles

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
    onSetSafetyBuffer: (Int) -> Unit,
    onSetLastActionMinutesAgo: (Int?) -> Unit,
    onResetModifiers: () -> Unit,
    onOpenTimerTab: () -> Unit,
    onOpenNotificationCenter: () -> Unit,
    onToggleTimerPauseResume: () -> Unit,
    onDismissTimer: () -> Unit,
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
    var showOptionsSheet by remember { mutableStateOf(false) }
    var expandedCardId by remember { mutableStateOf<String?>(null) }

    // Smooth midpoint-based drag-and-drop state
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

    fun buildDragModifier(evaluated: EvaluatedCoord): Modifier {
        return Modifier
            .minimumInteractiveComponentSize()
            .pointerInput(evaluated.point.id) {
                detectDragGestures(
                    onDragStart = {
                        val startIdx = latestEvaluated.indexOfFirst { it.point.id == evaluated.point.id }
                        val startEval = latestEvaluated.getOrNull(startIdx)
                        draggedId = evaluated.point.id
                        dragStartIndex = startIdx
                        dragStartCooldownSeconds = startEval?.remainingCooldownSeconds ?: 0
                        dragOffsetY = 0f
                        staleOffsetBeforeSwap = null
                        expandedCardId = null
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y

                        val currentList = latestEvaluated
                        val fromIdx = currentList.indexOfFirst { it.point.id == evaluated.point.id }
                        if (fromIdx == -1) return@detectDragGestures

                        val visibleCoordInfos = listState.layoutInfo.visibleItemsInfo
                            .mapNotNull { info ->
                                val id = info.key as? String ?: return@mapNotNull null
                                val coordIdx = currentList.indexOfFirst { it.point.id == id }
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

                        val draggedMidY = currentInfo.offset + currentInfo.size / 2f + dragOffsetY
                        val targetPair = visibleCoordInfos.firstOrNull { (cIdx, info) ->
                            if (cIdx == fromIdx) return@firstOrNull false
                            val targetMidY = info.offset + info.size / 2f
                            if (cIdx < fromIdx) draggedMidY < targetMidY else draggedMidY > targetMidY
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
                        val finalIdx = latestEvaluated.indexOfFirst { it.point.id == evaluated.point.id }
                        if (finalIdx == 0 && dragStartIndex > 0) {
                            onDragPromotedToCurrent(evaluated.point, dragStartCooldownSeconds)
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
            }
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Clean 1-Row App Header
            item(key = "home_header") {
                val extraTempCandidates = uiState.coordinates.drop(1)
                    .count { it.id != uiState.permanentCoord?.id }
                val canClear = if (uiState.isCurrentPermanent) {
                    uiState.tempCount > 0 || uiState.lastLocationCoord != null
                } else {
                    extraTempCandidates > 0 || (!uiState.timerState.isActive && uiState.permanentCoord != null)
                }
                HomeTopBar(
                    hasTempsOrLast = canClear,
                    hasActiveModifiers = uiState.elapsedSinceLastActionSeconds > 0 || uiState.safetyBufferMinutes > 0,
                    notificationsArmed = uiState.notificationPrefs.masterEnabled,
                    modifierBadge = buildString {
                        if (uiState.elapsedSinceLastActionSeconds > 0) {
                            append("-${uiState.elapsedSinceLastActionSeconds / 60}m")
                        }
                        if (uiState.safetyBufferMinutes > 0) {
                            if (isNotEmpty()) append(" ")
                            append("+${uiState.safetyBufferMinutes}m")
                        }
                    },
                    onReset = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        expandedCardId = null
                        onClearTempsResetToPermanent()
                    },
                    onOpenNotifications = onOpenNotificationCenter,
                    onOpenOptions = { showOptionsSheet = true }
                )
            }

            // Active Cooldown Modifiers Banner (always visible when an elapsed offset or safety buffer is active)
            if (uiState.elapsedSinceLastActionSeconds > 0 || uiState.safetyBufferMinutes > 0) {
                item(key = "active_modifiers_banner") {
                    val parts = buildList {
                        if (uiState.elapsedSinceLastActionSeconds > 0) {
                            val mins = (uiState.elapsedSinceLastActionSeconds / 60).coerceAtLeast(1)
                            add("Already waited ${mins}m")
                        }
                        if (uiState.safetyBufferMinutes > 0) {
                            add("+${uiState.safetyBufferMinutes}m safety buffer")
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active_modifiers_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = parts.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onResetModifiers()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, palette.hairline),
                                modifier = Modifier.testTag("reset_modifiers_button")
                            ) {
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Empty State OR Unified Hero "NOW" Card + Quick Paste + Ranked Spot List
            if (uiState.evaluatedCoords.isEmpty()) {
                item(key = "paste_bar_empty") {
                    QuickPasteCapsule(
                        inputText = uiState.inputText,
                        errorMessage = uiState.inputError,
                        onInputChanged = onInputChanged,
                        onPasteClipboard = {
                            val text = readClipboardText()
                            if (text.isNullOrBlank()) {
                                onShowMessage("Clipboard is empty")
                            } else if (onSubmitInput(text) > 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onAddManual = {
                            keyboard?.hide()
                            if (onSubmitInput(uiState.inputText) > 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    )
                }

                item(key = "welcome_card") {
                    WelcomeEmptyDeck(
                        onSetHome = { showPermanentDialog = true },
                        onPasteNow = {
                            val text = readClipboardText()
                            if (text.isNullOrBlank()) {
                                onShowMessage("Clipboard is empty")
                            } else {
                                onSubmitInput(text)
                            }
                        },
                        onLoadSample = onLoadDemoTrio
                    )
                }
            } else {
                val anchorEval = uiState.evaluatedCoords.first()
                // Hide Permanent Home from Compare Next when it is already shown in HeroNowCard's Return Home footer
                val hideHomeInList = uiState.returnToPermanentInfo != null
                val candidates = uiState.evaluatedCoords.drop(1).filterNot { hideHomeInList && it.isPermanent }

                // Hero Focus Card: Combines Mode Switcher + Where You Are + Active Timer + Home Return
                item(key = anchorEval.point.id) {
                    val isDragging = draggedId == anchorEval.point.id
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.02f else 1f,
                        animationSpec = spring(stiffness = 600f),
                        label = "hero_scale"
                    )
                    HeroNowCard(
                        anchor = anchorEval,
                        uiState = uiState,
                        isDragging = isDragging,
                        onSwitchToPermanent = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSwitchToPermanent(false)
                        },
                        onSwitchToTemp = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSwitchToTemp()
                        },
                        onOpenSetHome = { showPermanentDialog = true },
                        onCopyCoords = {
                            copyToClipboard(anchorEval.point.label, anchorEval.point.formattedCoords(5))
                        },
                        onEditAnchor = { editingCoord = anchorEval.point },
                        onPinAnchorAsHome = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPinAsPermanent(anchorEval.point.id)
                        },
                        onOpenTimerTab = onOpenTimerTab,
                        onToggleTimerPauseResume = onToggleTimerPauseResume,
                        onDismissTimer = onDismissTimer,
                        onReturnHome = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSwitchToPermanent(true)
                        },
                        onClearLastLocation = onClearLastLocation,
                        modifier = Modifier
                            .then(if (!isDragging) Modifier.animateItem() else Modifier)
                            .zIndex(if (isDragging) 10f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }

                // Direct 1-Tap Paste Capsule
                item(key = "paste_bar") {
                    QuickPasteCapsule(
                        inputText = uiState.inputText,
                        errorMessage = uiState.inputError,
                        onInputChanged = onInputChanged,
                        onPasteClipboard = {
                            val text = readClipboardText()
                            if (text.isNullOrBlank()) {
                                onShowMessage("Clipboard is empty")
                            } else if (onSubmitInput(text) > 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onAddManual = {
                            keyboard?.hide()
                            if (onSubmitInput(uiState.inputText) > 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    )
                }

                // Active Drag Hint
                item(key = "drag_hint") {
                    AnimatedVisibility(
                        visible = draggedId != null && uiState.evaluatedCoords.size >= 2,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.fastContainer)
                                .border(1.dp, palette.fast.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
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
                                text = "Drop on top card to set as Current Location",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.fast
                            )
                        }
                    }
                }

                // Candidate Spots Section
                if (candidates.isEmpty()) {
                    item(key = "single_spot_prompt") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, palette.hairline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Paste coordinates above to compare wait times from ${anchorEval.point.label}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    item(key = "next_spots_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "COMPARE NEXT",
                                    style = MonoStyles.badge,
                                    color = palette.mutedText
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${candidates.size}",
                                        style = MonoStyles.badge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (candidates.size >= 2) {
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSortFastest()
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, palette.hairline),
                                    modifier = Modifier.testTag("sort_fastest_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Sort,
                                            contentDescription = "Sort by shortest wait",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Fastest",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    itemsIndexed(
                        items = candidates,
                        key = { _, item -> item.point.id }
                    ) { idx, evaluated ->
                        val slotNumber = idx + 2
                        val isDragging = draggedId == evaluated.point.id
                        val isExpanded = expandedCardId == evaluated.point.id && !isDragging
                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.025f else 1f,
                            animationSpec = spring(stiffness = 600f),
                            label = "spot_scale"
                        )

                        SpotComparisonRow(
                            evaluated = evaluated,
                            slotNumber = slotNumber,
                            distanceUnit = uiState.distanceUnit,
                            lastLocationLabel = uiState.lastLocationCoord?.label,
                            isExpanded = isExpanded,
                            isDragging = isDragging,
                            onToggleExpand = {
                                expandedCardId = if (isExpanded) null else evaluated.point.id
                            },
                            onJumpHere = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                expandedCardId = null
                                onJumpToCoordinate(evaluated.point.id)
                            },
                            onCompareFromHere = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                expandedCardId = null
                                onPreviewAsCurrent(evaluated.point.id)
                            },
                            onCopyCoords = {
                                copyToClipboard(evaluated.point.label, evaluated.point.formattedCoords(5))
                            },
                            onEdit = { editingCoord = evaluated.point },
                            onPinAsHome = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                expandedCardId = null
                                onPinAsPermanent(evaluated.point.id)
                            },
                            onDelete = {
                                expandedCardId = null
                                onDeleteCoordinate(evaluated.point.id)
                            },
                            dragHandleModifier = buildDragModifier(evaluated),
                            modifier = Modifier
                                .then(if (!isDragging) Modifier.animateItem() else Modifier)
                                .zIndex(if (isDragging) 10f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    scaleX = scale
                                    scaleY = scale
                                }
                        )
                    }
                }
            }
        }
    }

    // Dialogs & Options Bottom Sheet
    editingCoord?.let { target ->
        EditCoordinateDialog(
            title = if (target.id == uiState.permanentCoord?.id) "Edit Home Base" else "Edit Coordinate",
            initialLabel = target.label,
            initialCoords = target.formattedCoords(5),
            onDismiss = { editingCoord = null },
            onSave = { newLabel, newRawCoords ->
                val error = onUpdateCoordinate(target.id, newLabel, newRawCoords)
                if (error == null) editingCoord = null
                error
            }
        )
    }

    if (showPermanentDialog) {
        SetPermanentBaseDialog(
            existing = uiState.permanentCoord,
            onPasteFromClipboard = { readClipboardText() },
            onDismiss = { showPermanentDialog = false },
            onClearPermanent = {
                onClearPermanentBase()
                showPermanentDialog = false
            },
            onSave = { label, rawCoords ->
                val error = onSetPermanentFromRaw(label, rawCoords)
                if (error == null) showPermanentDialog = false
                error
            }
        )
    }

    if (showOptionsSheet) {
        CompareOptionsSheet(
            distanceUnit = uiState.distanceUnit,
            safetyBufferMinutes = uiState.safetyBufferMinutes,
            elapsedSeconds = uiState.elapsedSinceLastActionSeconds,
            hasPermanent = uiState.permanentCoord != null,
            hasSummary = uiState.evaluatedCoords.size >= 2,
            onSelectMinutesAgo = onSetLastActionMinutesAgo,
            onSetSafetyBuffer = onSetSafetyBuffer,
            onToggleUnit = onToggleUnit,
            onEditPermanent = { showPermanentDialog = true },
            onCopySummary = onCopySummary,
            onDismiss = { showOptionsSheet = false }
        )
    }
}

@Composable
private fun HomeTopBar(
    hasTempsOrLast: Boolean,
    hasActiveModifiers: Boolean,
    notificationsArmed: Boolean,
    modifierBadge: String,
    onReset: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenOptions: () -> Unit
) {
    val palette = LocalCooldownPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasTempsOrLast) {
                Surface(
                    onClick = onReset,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, palette.hairline),
                    modifier = Modifier.testTag("reset_to_permanent_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = "Clear temporary coordinates",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Surface(
                onClick = onOpenNotifications,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, palette.hairline),
                modifier = Modifier.testTag("open_notifications_button")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        imageVector = if (notificationsArmed) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                        contentDescription = "Notification Center",
                        tint = if (notificationsArmed) MaterialTheme.colorScheme.onSurface else palette.mutedText,
                        modifier = Modifier.size(17.dp)
                    )
                    if (notificationsArmed) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(palette.fast)
                        )
                    }
                }
            }

            Surface(
                onClick = onOpenOptions,
                shape = RoundedCornerShape(12.dp),
                color = if (hasActiveModifiers) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    if (hasActiveModifiers) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    } else {
                        palette.hairline
                    }
                ),
                modifier = Modifier.testTag("compare_options_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Comparison options",
                        tint = if (hasActiveModifiers) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    if (hasActiveModifiers && modifierBadge.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = modifierBadge,
                            style = MonoStyles.badge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unified Hero "NOW" Card:
 * Houses Mode Switcher (Home | Temp), Current Location, Active Cooldown Countdown (with live progress bar),
 * and 1-tap Return Home — eliminating multiple stacked boxes and duplicate clocks.
 */
@Composable
private fun HeroNowCard(
    anchor: EvaluatedCoord,
    uiState: CoordShiftUiState,
    isDragging: Boolean,
    onSwitchToPermanent: () -> Unit,
    onSwitchToTemp: () -> Unit,
    onOpenSetHome: () -> Unit,
    onCopyCoords: () -> Unit,
    onEditAnchor: () -> Unit,
    onPinAnchorAsHome: () -> Unit,
    onOpenTimerTab: () -> Unit,
    onToggleTimerPauseResume: () -> Unit,
    onDismissTimer: () -> Unit,
    onReturnHome: () -> Unit,
    onClearLastLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCooldownPalette.current
    val perm = uiState.permanentCoord
    val hasTemps = uiState.tempCount > 0
    val timer = uiState.timerState
    val timerRemaining = timer.remainingSeconds(uiState.nowEpochMillis)
    val returnInfo = uiState.returnToPermanentInfo

    val accent = if (anchor.isPermanent) palette.fast else MaterialTheme.colorScheme.secondary
    val glowColor = accent.copy(alpha = 0.14f)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isDragging) 14.dp else 0.dp,
        border = BorderStroke(
            width = if (isDragging) 1.5.dp else 1.dp,
            color = if (isDragging) MaterialTheme.colorScheme.primary else accent.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("current_anchor_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = Offset(size.width * 0.15f, 0f),
                            radius = size.width * 0.95f
                        )
                    )
                }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Status indicator + Compact Inline Home|Temp Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = if (anchor.isPermanent) "AT HOME" else "CURRENT SPOT",
                        style = MonoStyles.badge,
                        color = accent
                    )
                }

                if (perm != null) {
                    // Compact inline Home | Temp segmented pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                            .border(1.dp, palette.hairline, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val permActive = uiState.isCurrentPermanent
                        val tempActive = !permActive
                        Surface(
                            onClick = onSwitchToPermanent,
                            shape = RoundedCornerShape(7.dp),
                            color = if (permActive) palette.fastContainer else Color.Transparent,
                            modifier = Modifier.testTag("switch_mode_permanent")
                        ) {
                            Text(
                                text = "Home",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (permActive) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (permActive) palette.fast else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            onClick = { if (hasTemps) onSwitchToTemp() },
                            enabled = hasTemps,
                            shape = RoundedCornerShape(7.dp),
                            color = if (tempActive) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            modifier = Modifier.testTag("switch_mode_temp")
                        ) {
                            Text(
                                text = "Temp",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (tempActive) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = when {
                                    tempActive -> MaterialTheme.colorScheme.secondary
                                    hasTemps -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> palette.mutedText.copy(alpha = 0.45f)
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = onOpenSetHome,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("set_permanent_base_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Set Home",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Hero Identity: Large Place Name + Tappable Copy Coords + Quick Edit/Pin
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = anchor.point.label,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!anchor.isPermanent && perm == null) {
                            IconButton(
                                onClick = onPinAnchorAsHome,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = "Save as Permanent Home",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onEditAnchor,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit current location",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Tappable coordinate chip for 1-tap copy
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCopyCoords)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = anchor.point.formattedCoords(5),
                        style = MonoStyles.coordinate,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy coordinates",
                        tint = palette.mutedText,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Contextual Active Cooldown Strip (with live drain progress line)
            if (timer.isActive) {
                val tTier = CooldownCalculator.tierForCooldownSeconds(timerRemaining)
                val tAccent = palette.accentFor(tTier)
                val fraction = (timerRemaining.toFloat() / timer.totalDurationSeconds.coerceAtLeast(1).toFloat())
                    .coerceIn(0f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = spring(stiffness = 300f),
                    label = "timer_progress"
                )

                Surface(
                    onClick = onOpenTimerTab,
                    shape = RoundedCornerShape(14.dp),
                    color = palette.containerFor(tTier).copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, tAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_timer_mini_bar")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                    tint = tAccent,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(9.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val timerSpot = timer.label.trim().uppercase()
                                    Text(
                                        text = when {
                                            timer.isRunning && timerSpot.isNotEmpty() -> "WAITING · $timerSpot"
                                            timer.isRunning -> "WAIT BEFORE ACTING"
                                            timerSpot.isNotEmpty() -> "PAUSED · $timerSpot"
                                            else -> "COOLDOWN PAUSED"
                                        },
                                        style = MonoStyles.badge,
                                        color = tAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Ready at ${
                                            CooldownCalculator.formatReadyTime(
                                                uiState.nowEpochMillis + timerRemaining * 1000L
                                            )
                                        }",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = CooldownCalculator.formatTimerClock(timerRemaining),
                                    style = MonoStyles.metricLarge,
                                    color = tAccent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(tAccent.copy(alpha = 0.16f))
                                        .clickable(onClick = onToggleTimerPauseResume),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (timer.isRunning) "Pause timer" else "Resume timer",
                                        tint = tAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Sleek live draining progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(tAccent.copy(alpha = 0.18f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedFraction)
                                    .height(3.dp)
                                    .background(tAccent)
                            )
                        }
                    }
                }
            } else if (timer.isFinished) {
                Surface(
                    onClick = onDismissTimer,
                    shape = RoundedCornerShape(14.dp),
                    color = palette.fastContainer.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, palette.fast.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("completed_timer_mini_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                            Column {
                                Text(
                                    text = "COOLDOWN COMPLETE",
                                    style = MonoStyles.badge,
                                    color = palette.fast
                                )
                                Text(
                                    text = "${timer.label.ifBlank { "Spot" }} is safe to act now",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.fast
                        )
                    }
                }
            }

            // Contextual Footer: Return Home shortcut (when on Temp) OR Last Visited (when at Home)
            if (returnInfo != null && perm != null) {
                val rAccent = palette.accentFor(returnInfo.tier)
                HorizontalDivider(color = palette.hairline.copy(alpha = 0.55f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onSwitchToPermanent)
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = null,
                            tint = rAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Column {
                            Text(
                                text = "Return to ${perm.label}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${
                                    if (returnInfo.remainingCooldownSeconds == 0) "Ready now"
                                    else CooldownCalculator.formatCooldown(returnInfo.remainingCooldownSeconds)
                                } · ${
                                    CooldownCalculator.formatDistance(
                                        returnInfo.distanceKm,
                                        uiState.distanceUnit
                                    )
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = rAccent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = onReturnHome,
                        shape = RoundedCornerShape(10.dp),
                        color = palette.containerFor(returnInfo.tier),
                        border = BorderStroke(1.dp, rAccent.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("return_to_permanent_jump_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Return",
                                style = MonoStyles.badge,
                                color = rAccent
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = rAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            } else if (uiState.isCurrentPermanent && uiState.lastLocationCoord != null) {
                HorizontalDivider(color = palette.hairline.copy(alpha = 0.55f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Also comparing from last spot: ${uiState.lastLocationCoord.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearLastLocation,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear last spot",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPasteCapsule(
    inputText: String,
    errorMessage: String?,
    onInputChanged: (String) -> Unit,
    onPasteClipboard: () -> Unit,
    onAddManual: () -> Unit
) {
    val palette = LocalCooldownPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
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
                        text = "Paste coordinates or Maps link…",
                        style = MonoStyles.coordinate,
                        color = palette.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                textStyle = MonoStyles.coordinate.copy(color = MaterialTheme.colorScheme.onSurface),
                singleLine = false,
                maxLines = 3,
                isError = errorMessage != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddManual() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = palette.hairline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("coord_input_field")
            )

            if (inputText.isNotBlank()) {
                Button(
                    onClick = onAddManual,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_coord_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.action_add),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Button(
                    onClick = onPasteClipboard,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
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
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/**
 * Swiss-Minimal Spot Card with Progressive Disclosure:
 * - Collapsed (default): Thin severity bar, Place Name + optional single badge, Distance & Ready clock,
 *   big tabular Wait Time, and 1-tap Jump pill.
 * - Tap card body: Smoothly reveals full coordinates, compass bearing, and labeled action chips
 *   (Compare, Copy, Edit, Set Home, Remove).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpotComparisonRow(
    evaluated: EvaluatedCoord,
    slotNumber: Int,
    distanceUnit: DistanceUnit,
    lastLocationLabel: String?,
    isExpanded: Boolean,
    isDragging: Boolean,
    onToggleExpand: () -> Unit,
    onJumpHere: () -> Unit,
    onCompareFromHere: () -> Unit,
    onCopyCoords: () -> Unit,
    onEdit: () -> Unit,
    onPinAsHome: () -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier
) {
    val palette = LocalCooldownPalette.current
    val tierAccent = palette.accentFor(evaluated.tier)
    val tierContainer = palette.containerFor(evaluated.tier)

    val borderColor = when {
        isDragging -> MaterialTheme.colorScheme.primary
        isExpanded -> tierAccent.copy(alpha = 0.55f)
        evaluated.isBestPick -> palette.fast.copy(alpha = 0.5f)
        evaluated.isPermanent -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        else -> palette.hairline
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isDragging) 12.dp else 0.dp,
        border = BorderStroke(if (isDragging || evaluated.isBestPick || isExpanded) 1.25.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = 500f))
            .testTag("candidate_card_$slotNumber")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Scannable Row (tap body to expand/collapse details)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(start = 2.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag Grip
                Box(
                    modifier = dragHandleModifier.testTag("drag_handle_${evaluated.point.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = stringResource(R.string.action_drag_reorder),
                        tint = if (isDragging) MaterialTheme.colorScheme.primary else palette.mutedText.copy(alpha = 0.65f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Semantic Severity Accent Bar
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(tierAccent)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Left Info Column: Name (+ max 1 pill) + Distance/Ready clock + optional dual ref
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = evaluated.point.label,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        when {
                            evaluated.isPermanent -> StatusPillBadge(
                                text = "HOME",
                                accentColor = MaterialTheme.colorScheme.secondary,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                showDot = false
                            )
                            evaluated.isBestPick -> StatusPillBadge(
                                text = "BEST",
                                accentColor = palette.fast,
                                containerColor = palette.fastContainer,
                                showDot = true
                            )
                            evaluated.isLastLocation -> StatusPillBadge(
                                text = "LAST",
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                showDot = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    val readySub = if (evaluated.remainingCooldownSeconds == 0) {
                        "Ready now"
                    } else {
                        "Ready ${CooldownCalculator.formatReadyTime(evaluated.readyAtEpochMillis)}"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${CooldownCalculator.formatDistance(evaluated.distanceKm, distanceUnit)} · $readySub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Hide actions" else "Show actions",
                            tint = palette.mutedText,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Compact 1-line secondary reference ONLY when applicable
                    val secondaryHint = when {
                        evaluated.cooldownFromLastLocationSeconds != null -> {
                            val cd = evaluated.cooldownFromLastLocationSeconds
                            val label = lastLocationLabel ?: "Last"
                            "From $label: ${if (cd == 0) "Ready" else CooldownCalculator.formatCooldown(cd)}"
                        }
                        evaluated.cooldownToPermanentSeconds != null -> {
                            val cd = evaluated.cooldownToPermanentSeconds
                            "Home return: ${if (cd == 0) "Ready" else CooldownCalculator.formatCooldown(cd)}"
                        }
                        else -> null
                    }
                    if (secondaryHint != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = secondaryHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Column: Big unmistakable wait time + 1-tap Jump button
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (evaluated.remainingCooldownSeconds == 0) {
                            "Ready"
                        } else {
                            CooldownCalculator.formatCooldown(evaluated.remainingCooldownSeconds)
                        },
                        style = MonoStyles.metricLarge,
                        color = tierAccent
                    )

                    Surface(
                        onClick = onJumpHere,
                        shape = RoundedCornerShape(10.dp),
                        color = tierContainer,
                        border = BorderStroke(1.dp, tierAccent.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("jump_coord_${evaluated.point.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "Jump and start cooldown",
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
                }
            }

            // Expandable Progressive-Disclosure Action Drawer
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = evaluated.point.formattedCoords(5),
                            style = MonoStyles.coordinate,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${evaluated.cardinalDirection} ${evaluated.bearingDegrees.toInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        InlineActionChip(
                            label = "Compare here",
                            icon = Icons.Filled.SwapVert,
                            onClick = onCompareFromHere,
                            testTag = "set_current_${evaluated.point.id}"
                        )
                        InlineActionChip(
                            label = "Copy",
                            icon = Icons.Filled.ContentCopy,
                            onClick = onCopyCoords,
                            testTag = "copy_coord_${evaluated.point.id}"
                        )
                        InlineActionChip(
                            label = "Edit",
                            icon = Icons.Filled.Edit,
                            onClick = onEdit
                        )
                        if (!evaluated.isPermanent) {
                            InlineActionChip(
                                label = "Set Home",
                                icon = Icons.Filled.PushPin,
                                tint = MaterialTheme.colorScheme.secondary,
                                onClick = onPinAsHome,
                                testTag = "pin_permanent_${evaluated.point.id}"
                            )
                            InlineActionChip(
                                label = "Remove",
                                icon = Icons.Filled.DeleteOutline,
                                tint = MaterialTheme.colorScheme.error,
                                onClick = onDelete,
                                testTag = "delete_coord_${evaluated.point.id}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String? = null
) {
    val palette = LocalCooldownPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, palette.hairline),
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tint
            )
        }
    }
}

@Composable
private fun WelcomeEmptyDeck(
    onSetHome: () -> Unit,
    onPasteNow: () -> Unit,
    onLoadSample: () -> Unit
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
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.fastContainer)
                    .border(1.dp, palette.fast.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = palette.fast,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Compare wait times instantly",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Save your Permanent Home, paste temporary spots, and tap Jump on the fastest option.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSetHome,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("set_permanent_base_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Permanent Home")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPasteNow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.hairline),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("empty_paste_button")
                ) {
                    Text("Paste Coords")
                }

                OutlinedButton(
                    onClick = onLoadSample,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.hairline),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("load_demo_button")
                ) {
                    Text("Sample Trio")
                }
            }
        }
    }
}
