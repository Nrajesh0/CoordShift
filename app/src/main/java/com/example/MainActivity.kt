package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppTab
import com.example.data.CoordPoint
import com.example.data.ThemeMode
import com.example.domain.CooldownCalculator
import com.example.ui.components.CoordShiftFooterBar
import com.example.ui.components.CoordShiftNavigationRail
import com.example.ui.screens.ChartScreen
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.RouteRadarScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.CoordShiftTheme
import com.example.viewmodel.CoordShiftViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            CoordShiftApp(
                onUpdateSystemBars = { isDark ->
                    val style = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
            )
        }
    }
}

@Composable
fun CoordShiftApp(
    onUpdateSystemBars: (Boolean) -> Unit = {},
    viewModel: CoordShiftViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isDark = when (uiState.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    DisposableEffect(isDark) {
        onUpdateSystemBars(isDark)
        onDispose {}
    }

    fun showMessage(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onAction?.invoke()
            }
        }
    }

    fun announcePreviewSwitch(promoted: CoordPoint, waitSec: Int) {
        if (waitSec > 0) {
            val waitStr = CooldownCalculator.formatCooldown(waitSec)
            showMessage(
                message = "Comparing from ${promoted.label} ($waitStr wait)",
                actionLabel = "Start Timer",
                onAction = {
                    viewModel.startTimer(
                        durationSeconds = waitSec,
                        label = promoted.label,
                        switchToTimerTab = false
                    )
                }
            )
        } else {
            showMessage("Comparing from ${promoted.label}")
        }
    }

    fun handleClearTemps() {
        val hadPermanent = uiState.permanentCoord != null
        viewModel.clearTempsAndResetToPermanent()
        showMessage(
            message = if (hadPermanent) "Cleared temps · Reset to Permanent Base" else "Cleared temporary coordinates",
            actionLabel = "Undo",
            onAction = { viewModel.undoClearTemps() }
        )
    }

    fun handleSwitchToPermanent(triggerTimer: Boolean) {
        val outcome = viewModel.switchAnchorToPermanent(triggerTimer = triggerTimer)
        if (outcome != null) {
            if (triggerTimer && outcome.jumpCooldownSeconds > 0) {
                showMessage(
                    message = "Jumped Home · ${
                        CooldownCalculator.formatCooldown(outcome.jumpCooldownSeconds)
                    } timer started",
                    actionLabel = "Timer",
                    onAction = { viewModel.selectTab(AppTab.TIMER) }
                )
            } else {
                showMessage("Switched to Permanent Base (comparing vs Last location)")
            }
        }
    }

    fun handleSwitchToTemp(coordId: String? = null) {
        val res = viewModel.switchAnchorToTemp(coordId)
        if (res != null) {
            announcePreviewSwitch(res.first, res.second)
        }
    }

    // Back press on secondary tabs returns cleanly to Compare home tab
    BackHandler(enabled = uiState.selectedTab != AppTab.COMPARE) {
        viewModel.selectTab(AppTab.COMPARE)
    }

    CoordShiftTheme(themeMode = uiState.themeMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 720.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.statusBars,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) {
                        CoordShiftFooterBar(
                            selectedTab = uiState.selectedTab,
                            isTimerRunning = uiState.timerState.isRunning,
                            onSelectTab = viewModel::selectTab
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        CoordShiftNavigationRail(
                            selectedTab = uiState.selectedTab,
                            isTimerRunning = uiState.timerState.isRunning,
                            onSelectTab = viewModel::selectTab
                        )
                    }

                    Crossfade(
                        targetState = uiState.selectedTab,
                        modifier = Modifier.weight(1f),
                        label = "tab_crossfade"
                    ) { tab ->
                        when (tab) {
                            AppTab.COMPARE -> CompareScreen(
                                uiState = uiState,
                                onInputChanged = viewModel::updateInputText,
                                onSubmitInput = { raw -> viewModel.addFromRawText(raw) },
                                onMoveCoordinate = viewModel::moveCoordinate,
                                onPreviewAsCurrent = { id ->
                                    if (id == uiState.permanentCoord?.id) {
                                        handleSwitchToPermanent(triggerTimer = false)
                                    } else {
                                        handleSwitchToTemp(id)
                                    }
                                },
                                onJumpToCoordinate = { id ->
                                    val outcome = viewModel.jumpToCoordinate(id)
                                    if (outcome != null) {
                                        val jumpText = if (outcome.jumpCooldownSeconds > 0) {
                                            "${CooldownCalculator.formatCooldown(outcome.jumpCooldownSeconds)} timer started"
                                        } else {
                                            "Ready now"
                                        }
                                        val permReturnText = outcome.returnToPermanentSeconds
                                            ?.takeIf { it > 0 }
                                            ?.let { " · Perm return ${CooldownCalculator.formatCooldown(it)}" }
                                            .orEmpty()
                                        showMessage(
                                            message = "Jumped → ${outcome.destination.label} ($jumpText$permReturnText)",
                                            actionLabel = if (outcome.jumpCooldownSeconds > 0) "Timer" else null,
                                            onAction = { viewModel.selectTab(AppTab.TIMER) }
                                        )
                                    }
                                },
                                onPinAsPermanent = { id ->
                                    val pinned = viewModel.pinAsPermanent(id)
                                    if (pinned != null) {
                                        showMessage("Pinned ${pinned.label} as Permanent Base")
                                    }
                                },
                                onSetPermanentFromRaw = viewModel::setPermanentFromRaw,
                                onClearPermanentBase = viewModel::clearPermanentBase,
                                onSwitchToPermanent = { triggerTimer ->
                                    handleSwitchToPermanent(triggerTimer)
                                },
                                onSwitchToTemp = { handleSwitchToTemp(null) },
                                onClearTempsResetToPermanent = { handleClearTemps() },
                                onClearLastLocation = {
                                    viewModel.clearLastLocationReference()
                                    showMessage("Cleared Last Location reference")
                                },
                                onSortFastest = viewModel::sortCandidatesByFastest,
                                onUpdateCoordinate = viewModel::updateCoordinate,
                                onDeleteCoordinate = { id ->
                                    viewModel.deleteCoordinate(id)
                                    showMessage(
                                        message = "Coordinate removed",
                                        actionLabel = "Undo",
                                        onAction = { viewModel.undoDelete() }
                                    )
                                },
                                onCopySummary = {
                                    val summary = viewModel.buildShareSummary()
                                    if (summary.isNotBlank()) {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(ClipData.newPlainText("CoordShift Summary", summary))
                                        showMessage("Copied comparison summary")
                                    }
                                },
                                onDragPromotedToCurrent = { promoted, waitSec ->
                                    announcePreviewSwitch(promoted, waitSec)
                                },
                                onLoadDemoTrio = viewModel::loadDemoTrio,
                                onToggleUnit = viewModel::toggleDistanceUnit,
                                onCycleBuffer = viewModel::cycleSafetyBuffer,
                                onSetLastActionMinutesAgo = viewModel::setLastActionMinutesAgo,
                                onOpenTimerTab = { viewModel.selectTab(AppTab.TIMER) },
                                onToggleTimerPauseResume = viewModel::toggleTimerPauseResume,
                                onShowMessage = { msg -> showMessage(msg) }
                            )

                            AppTab.RADAR -> RouteRadarScreen(
                                uiState = uiState,
                                onApplyOptimalOrder = {
                                    viewModel.applyOptimalRouteOrder()
                                    showMessage("Reordered list to optimal low-cooldown path")
                                },
                                onPromoteCoordinate = { id ->
                                    if (id == uiState.permanentCoord?.id) {
                                        handleSwitchToPermanent(triggerTimer = false)
                                    } else {
                                        handleSwitchToTemp(id)
                                    }
                                },
                                onSwitchToPermanent = { triggerTimer ->
                                    handleSwitchToPermanent(triggerTimer)
                                },
                                onSwitchToTemp = { handleSwitchToTemp(null) },
                                onClearTempsResetToPermanent = { handleClearTemps() },
                                onClearLastLocation = {
                                    viewModel.clearLastLocationReference()
                                    showMessage("Cleared Last Location reference")
                                },
                                onGoToCompare = { viewModel.selectTab(AppTab.COMPARE) }
                            )

                            AppTab.TIMER -> TimerScreen(
                                uiState = uiState,
                                onStartTimer = { sec, label ->
                                    viewModel.startTimer(sec, label, switchToTimerTab = false)
                                },
                                onTogglePauseResume = viewModel::toggleTimerPauseResume,
                                onAdjustTimerSeconds = viewModel::adjustTimerSeconds,
                                onResetTimer = viewModel::resetTimer
                            )

                            AppTab.CHART -> ChartScreen(
                                uiState = uiState,
                                onSetDistanceUnit = viewModel::setDistanceUnit,
                                onSetSafetyBuffer = viewModel::setSafetyBuffer,
                                onSetThemeMode = viewModel::setThemeMode,
                                onStartTimerFromChart = { sec, label ->
                                    viewModel.startTimer(sec, label, switchToTimerTab = true)
                                    showMessage("Started timer for $label")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
