package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppTab
import com.example.data.CoordPoint
import com.example.data.CoordPreferencesRepository
import com.example.data.CoordShiftUiState
import com.example.data.CooldownTier
import com.example.data.DistanceUnit
import com.example.data.EvaluatedCoord
import com.example.data.JumpOutcome
import com.example.data.ThemeMode
import com.example.data.TimerState
import com.example.domain.CooldownCalculator
import com.example.domain.CoordinateParser
import com.example.domain.PlaceNameResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

class CoordShiftViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoordPreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(CoordShiftUiState())
    val uiState: StateFlow<CoordShiftUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = repository.sessionFlow.first()
            val now = System.currentTimeMillis()
            val sanitizedTimer = if (session.timerState.isRunning &&
                session.timerState.remainingSeconds(now) <= 0
            ) {
                session.timerState.copy(isRunning = false, pausedRemainingSeconds = 0)
            } else {
                session.timerState
            }
            val initialList = session.permanentCoord?.let { listOf(it) } ?: emptyList()
            _uiState.update { current ->
                recomputeState(
                    current.copy(
                        permanentCoord = session.permanentCoord,
                        coordinates = initialList,
                        distanceUnit = session.distanceUnit,
                        themeMode = session.themeMode,
                        safetyBufferMinutes = session.safetyBufferMinutes,
                        lastActionEpochMillis = session.lastActionEpochMillis,
                        timerState = sanitizedTimer,
                        nowEpochMillis = now
                    )
                )
            }
        }

        // 1-second live clock ticker for countdown timer & elapsed-time remaining wait updates
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val now = System.currentTimeMillis()
                _uiState.update { current ->
                    val updatedTimer = if (current.timerState.isRunning &&
                        current.timerState.remainingSeconds(now) <= 0
                    ) {
                        val finished = current.timerState.copy(
                            isRunning = false,
                            pausedRemainingSeconds = 0
                        )
                        viewModelScope.launch { repository.saveTimerState(finished) }
                        finished
                    } else {
                        current.timerState
                    }
                    recomputeState(
                        current.copy(
                            nowEpochMillis = now,
                            timerState = updatedTimer
                        )
                    )
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text, inputError = null) }
    }

    /**
     * Parses one or multiple temporary GPS coordinates from raw text (typed or pasted).
     * Returns number of newly added coordinates.
     */
    fun addFromRawText(rawText: String, clearInputOnSuccess: Boolean = true): Int {
        val parsed = CoordinateParser.parseAll(rawText)
        if (parsed.isEmpty()) {
            _uiState.update {
                it.copy(inputError = "No valid coordinates found. Try: 35.6762, 139.6503")
            }
            return 0
        }

        val currentCoords = _uiState.value.coordinates.toMutableList()
        var addedCount = 0
        for (item in parsed) {
            val exists = currentCoords.any {
                abs(it.latitude - item.latitude) < 1e-5 &&
                    abs(it.longitude - item.longitude) < 1e-5
            }
            if (!exists) {
                val id = UUID.randomUUID().toString()
                val offlineGuess = if (item.suggestedLabel == null) {
                    PlaceNameResolver.lookupOfflineCity(item.latitude, item.longitude)
                } else null
                val label = item.suggestedLabel ?: offlineGuess ?: nextAvailableLabel(currentCoords)
                currentCoords.add(
                    CoordPoint(
                        id = id,
                        latitude = item.latitude,
                        longitude = item.longitude,
                        label = label
                    )
                )
                addedCount++
            }
        }

        if (addedCount == 0) {
            _uiState.update {
                it.copy(
                    inputError = "Coordinate already in your list",
                    inputText = if (clearInputOnSuccess) "" else it.inputText
                )
            }
            return 0
        }

        _uiState.update { current ->
            recomputeState(
                current.copy(
                    coordinates = currentCoords,
                    inputText = if (clearInputOnSuccess) "" else current.inputText,
                    inputError = null
                )
            )
        }
        return addedCount
    }

    /**
     * Sets or updates the single Permanent Base coordinate from raw text + optional label.
     * Saved persistently in DataStore and kept in the active comparison list.
     */
    fun setPermanentFromRaw(label: String, rawCoords: String): String? {
        val parsed = CoordinateParser.parseAll(rawCoords).firstOrNull()
            ?: return "Invalid coordinates. Use format: 35.6762, 139.6503"
        val userTypedCustom = label.trim().isNotEmpty() && !label.trim().equals("Home Base", ignoreCase = true)
        val offlineGuess = if (!userTypedCustom) {
            PlaceNameResolver.lookupOfflineCity(parsed.latitude, parsed.longitude)
        } else null
        val cleanLabel = when {
            userTypedCustom -> label.trim().take(32)
            parsed.suggestedLabel != null -> parsed.suggestedLabel.take(32)
            offlineGuess != null -> offlineGuess
            else -> "Home Base"
        }

        val state = _uiState.value
        val existingPerm = state.permanentCoord
        val newPerm = CoordPoint(
            id = existingPerm?.id ?: UUID.randomUUID().toString(),
            latitude = parsed.latitude,
            longitude = parsed.longitude,
            label = cleanLabel
        )

        val updatedList = state.coordinates.toMutableList()
        val existingIdx = updatedList.indexOfFirst { it.id == newPerm.id }
        if (existingIdx >= 0) {
            updatedList[existingIdx] = newPerm
        } else {
            updatedList.add(0, newPerm)
        }

        _uiState.update { current ->
            recomputeState(
                current.copy(
                    permanentCoord = newPerm,
                    coordinates = updatedList,
                    inputError = null
                )
            )
        }
        viewModelScope.launch { repository.savePermanentCoord(newPerm) }
        return null
    }

    /**
     * Pins an existing coordinate in the list as the single Permanent Base.
     */
    fun pinAsPermanent(coordId: String): CoordPoint? {
        val target = _uiState.value.coordinates.firstOrNull { it.id == coordId } ?: return null
        _uiState.update { current ->
            val updatedLast = if (current.lastLocationCoord?.id == target.id) {
                null
            } else {
                current.lastLocationCoord
            }
            recomputeState(
                current.copy(
                    permanentCoord = target,
                    lastLocationCoord = updatedLast
                )
            )
        }
        viewModelScope.launch { repository.savePermanentCoord(target) }
        return target
    }

    fun clearPermanentBase() {
        val perm = _uiState.value.permanentCoord ?: return
        val remaining = _uiState.value.coordinates.filterNot { it.id == perm.id }
        _uiState.update { current ->
            recomputeState(
                current.copy(
                    permanentCoord = null,
                    coordinates = remaining
                )
            )
        }
        viewModelScope.launch { repository.savePermanentCoord(null) }
    }

    /**
     * Switches Active Current Anchor back to the Permanent Base.
     * Remembers the previous Temp Current as `lastLocationCoord` so newly pasted coordinates
     * compare against BOTH Permanent AND Last Location!
     * Optionally triggers the return-to-permanent cooldown timer.
     */
    fun switchAnchorToPermanent(triggerTimer: Boolean = false): JumpOutcome? {
        val state = _uiState.value
        val perm = state.permanentCoord ?: return null
        if (state.isCurrentPermanent) return null

        val prevCurrent = state.currentAnchor
        val permEval = state.evaluatedCoords.firstOrNull { it.isPermanent }
        val returnCooldownSec = permEval?.remainingCooldownSeconds ?: 0

        val list = state.coordinates.toMutableList()
        val permIdx = list.indexOfFirst { it.id == perm.id }
        if (permIdx >= 0) {
            list.removeAt(permIdx)
        }
        list.add(0, perm)

        val rememberedLast = if (prevCurrent != null && prevCurrent.id != perm.id) {
            prevCurrent
        } else {
            state.lastLocationCoord
        }

        _uiState.update { current ->
            recomputeState(
                current.copy(
                    coordinates = list,
                    lastLocationCoord = rememberedLast,
                    lastActionEpochMillis = if (triggerTimer) null else current.lastActionEpochMillis
                )
            )
        }

        if (triggerTimer) {
            viewModelScope.launch { repository.saveLastActionEpoch(null) }
            if (returnCooldownSec > 0) {
                startTimer(returnCooldownSec, perm.label, switchToTimerTab = false)
            }
        }

        return JumpOutcome(
            destination = perm,
            jumpCooldownSeconds = returnCooldownSec,
            returnToPermanentSeconds = 0
        )
    }

    /**
     * Switches Active Current Anchor to a Temp coordinate (either a specific ID,
     * the remembered `lastLocationCoord`, or the first available Temp coordinate).
     * Used for quick "Temp ⇄ Permanent" toggling or preview comparisons.
     */
    fun switchAnchorToTemp(coordId: String? = null): Pair<CoordPoint, Int>? {
        val state = _uiState.value
        val permId = state.permanentCoord?.id
        val targetId = coordId
            ?: state.lastLocationCoord?.id?.takeIf { id -> state.coordinates.any { it.id == id } }
            ?: state.coordinates.firstOrNull { it.id != permId }?.id
            ?: return null

        val eval = state.evaluatedCoords.firstOrNull { it.point.id == targetId } ?: return null
        if (eval.isCurrent) return null

        val list = state.coordinates.toMutableList()
        val idx = list.indexOfFirst { it.id == targetId }
        if (idx <= 0) return null

        val prevCurrent = list.firstOrNull()
        val promoted = list.removeAt(idx)
        list.add(0, promoted)

        val nextLast = when {
            prevCurrent != null && prevCurrent.id != permId -> prevCurrent
            else -> state.lastLocationCoord
        }

        _uiState.update { current ->
            recomputeState(
                current.copy(
                    coordinates = list,
                    lastLocationCoord = nextLast
                )
            )
        }
        return promoted to eval.remainingCooldownSeconds
    }

    /**
     * Commits a real "Jump / Switch" to a chosen better coordinate:
     * 1. Sets it as Active Current Anchor (index 0) and updates `lastLocationCoord`
     * 2. Automatically triggers the live Cooldown Timer for the jump wait
     * 3. Computes and returns the Return-to-Permanent cooldown so you always know how long back to base!
     */
    fun jumpToCoordinate(coordId: String): JumpOutcome? {
        val state = _uiState.value
        val eval = state.evaluatedCoords.firstOrNull { it.point.id == coordId } ?: return null
        if (eval.isCurrent) return null

        if (eval.isPermanent) {
            return switchAnchorToPermanent(triggerTimer = true)
        }

        val list = state.coordinates.toMutableList()
        val idx = list.indexOfFirst { it.id == coordId }
        if (idx <= 0) return null

        val prevCurrent = list.firstOrNull()
        val destination = list.removeAt(idx)
        list.add(0, destination)

        val permId = state.permanentCoord?.id
        val nextLast = if (prevCurrent != null && prevCurrent.id != permId) {
            prevCurrent
        } else {
            destination
        }

        val jumpWaitSec = eval.remainingCooldownSeconds
        val returnToPermSec = state.permanentCoord?.let { perm ->
            val d = CooldownCalculator.distanceKm(
                destination.latitude,
                destination.longitude,
                perm.latitude,
                perm.longitude
            )
            val raw = CooldownCalculator.calculateCooldownSeconds(d)
            val buf = state.safetyBufferMinutes * 60
            if (raw > 0) (raw + buf).coerceAtMost(7200 + buf) else 0
        }

        _uiState.update { current ->
            recomputeState(
                current.copy(
                    coordinates = list,
                    lastLocationCoord = nextLast,
                    lastActionEpochMillis = null
                )
            )
        }
        viewModelScope.launch { repository.saveLastActionEpoch(null) }

        if (jumpWaitSec > 0) {
            startTimer(jumpWaitSec, destination.label, switchToTimerTab = false)
        }

        return JumpOutcome(
            destination = destination,
            jumpCooldownSeconds = jumpWaitSec,
            returnToPermanentSeconds = returnToPermSec
        )
    }

    fun clearLastLocationReference() {
        _uiState.update { current ->
            recomputeState(current.copy(lastLocationCoord = null))
        }
    }

    /**
     * Moves a coordinate from [fromIndex] to [toIndex] during drag-and-drop.
     * Index 0 is always the Active Current Anchor — dropping any card into index 0
     * promotes it as Current and recalculates all cooldowns!
     */
    fun moveCoordinate(fromIndex: Int, toIndex: Int) {
        val state = _uiState.value
        val list = state.coordinates.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return

        val prevAnchor = list.firstOrNull()
        val moved = list.removeAt(fromIndex)
        list.add(toIndex, moved)

        val permId = state.permanentCoord?.id
        val newAnchor = list.firstOrNull()
        val nextLast = if (newAnchor?.id == permId && prevAnchor != null && prevAnchor.id != permId) {
            prevAnchor
        } else {
            state.lastLocationCoord
        }

        _uiState.update { current ->
            recomputeState(
                current.copy(
                    coordinates = list,
                    lastLocationCoord = nextLast
                )
            )
        }
    }

    /**
     * Sorts all candidate coordinates (index 1..N) by shortest cooldown from Current Anchor.
     */
    fun sortCandidatesByFastest() {
        val evaluated = _uiState.value.evaluatedCoords
        if (evaluated.size < 3) return
        val anchor = evaluated.first().point
        val sortedCandidates = evaluated.drop(1)
            .sortedBy { it.distanceKm }
            .map { it.point }
        _uiState.update { current ->
            recomputeState(current.copy(coordinates = listOf(anchor) + sortedCandidates))
        }
    }

    /**
     * Applies the greedy shortest-cooldown multi-hop route order starting from Current Anchor.
     */
    fun applyOptimalRouteOrder() {
        val optimal = _uiState.value.routeSummary?.optimalOrderPoints ?: return
        if (optimal.size < 2) return
        _uiState.update { current ->
            recomputeState(current.copy(coordinates = optimal))
        }
    }

    fun updateCoordinate(coordId: String, newLabel: String, newRawCoords: String): String? {
        val trimmedLabel = newLabel.trim().take(32)
        if (trimmedLabel.isEmpty()) return "Label cannot be empty"
        val parsed = CoordinateParser.parseAll(newRawCoords).firstOrNull()
            ?: return "Invalid coordinates. Use format: 35.6762, 139.6503"

        val state = _uiState.value
        var updatedPerm = state.permanentCoord
        val updatedList = state.coordinates.map { pt ->
            if (pt.id == coordId) {
                val edited = pt.copy(
                    label = trimmedLabel,
                    latitude = parsed.latitude,
                    longitude = parsed.longitude
                )
                if (updatedPerm?.id == coordId) {
                    updatedPerm = edited
                }
                edited
            } else {
                pt
            }
        }
        val updatedLast = if (state.lastLocationCoord?.id == coordId) {
            state.lastLocationCoord.copy(
                label = trimmedLabel,
                latitude = parsed.latitude,
                longitude = parsed.longitude
            )
        } else {
            state.lastLocationCoord
        }

        _uiState.update { current ->
            recomputeState(
                current.copy(
                    permanentCoord = updatedPerm,
                    coordinates = updatedList,
                    lastLocationCoord = updatedLast
                )
            )
        }
        if (updatedPerm?.id == coordId) {
            viewModelScope.launch { repository.savePermanentCoord(updatedPerm) }
        }
        return null
    }

    fun deleteCoordinate(coordId: String) {
        val state = _uiState.value
        if (state.permanentCoord?.id == coordId) {
            // Deleting the Permanent Base requires explicit clearPermanentBase(); keep safe!
            clearPermanentBase()
            return
        }
        val list = state.coordinates.toMutableList()
        val idx = list.indexOfFirst { it.id == coordId }
        if (idx == -1) return
        val removed = list.removeAt(idx)
        val updatedLast = if (state.lastLocationCoord?.id == coordId) null else state.lastLocationCoord
        _uiState.update { current ->
            recomputeState(
                current.copy(
                    coordinates = list,
                    lastLocationCoord = updatedLast,
                    lastDeletedCoord = idx to removed
                )
            )
        }
    }

    fun undoDelete() {
        val deleted = _uiState.value.lastDeletedCoord ?: return
        val list = _uiState.value.coordinates.toMutableList()
        val insertIdx = deleted.first.coerceIn(0, list.size)
        list.add(insertIdx, deleted.second)
        _uiState.update { current ->
            recomputeState(
                current.copy(
                    coordinates = list,
                    lastDeletedCoord = null
                )
            )
        }
    }

    /**
     * Explicit user Clear Temps / Reset to Permanent action:
     * Wipes all temporary pasted coordinates and resets Active Anchor cleanly to the Permanent Base
     * (keeping the Permanent Base intact!).
     */
    fun clearTempsAndResetToPermanent() {
        val state = _uiState.value
        if (state.tempCount == 0 && state.lastLocationCoord == null) return
        val snapshot = Triple(state.coordinates, state.lastLocationCoord, state.permanentCoord)
        val resetList = state.permanentCoord?.let { listOf(it) } ?: emptyList()

        _uiState.update { current ->
            recomputeState(
                current.copy(
                    coordinates = resetList,
                    lastLocationCoord = null,
                    lastClearedSnapshot = snapshot
                )
            )
        }
    }

    fun undoClearTemps() {
        val snapshot = _uiState.value.lastClearedSnapshot ?: return
        _uiState.update { current ->
            recomputeState(
                current.copy(
                    coordinates = snapshot.first,
                    lastLocationCoord = snapshot.second,
                    lastClearedSnapshot = null
                )
            )
        }
    }

    fun buildShareSummary(): String {
        val state = _uiState.value
        return CooldownCalculator.formatComparisonSummary(
            evaluated = state.evaluatedCoords,
            unit = state.distanceUnit
        )
    }

    /**
     * Triggered only on explicit user click ("Try sample coords") to demonstrate
     * Permanent + Temp comparison when clipboard is empty.
     */
    fun loadDemoTrio() {
        val existingPerm = _uiState.value.permanentCoord
        val samples = buildList {
            if (existingPerm != null) {
                add(existingPerm)
            } else {
                add(
                    CoordPoint(
                        id = UUID.randomUUID().toString(),
                        latitude = 35.68950,
                        longitude = 139.69170,
                        label = "Tokyo Shinjuku"
                    )
                )
            }
            add(
                CoordPoint(
                    id = UUID.randomUUID().toString(),
                    latitude = 35.44370,
                    longitude = 139.63800,
                    label = "Yokohama Bay"
                )
            )
            add(
                CoordPoint(
                    id = UUID.randomUUID().toString(),
                    latitude = 34.69370,
                    longitude = 135.50230,
                    label = "Osaka Umeda"
                )
            )
            add(
                CoordPoint(
                    id = UUID.randomUUID().toString(),
                    latitude = 37.56650,
                    longitude = 126.97800,
                    label = "Seoul Myeongdong"
                )
            )
        }
        val permToKeep = existingPerm ?: samples.first()
        _uiState.update { current ->
            recomputeState(
                current.copy(
                    permanentCoord = permToKeep,
                    coordinates = samples
                )
            )
        }
        if (existingPerm == null) {
            viewModelScope.launch { repository.savePermanentCoord(permToKeep) }
        }
    }

    fun toggleDistanceUnit() {
        val next = if (_uiState.value.distanceUnit == DistanceUnit.KM) {
            DistanceUnit.MI
        } else {
            DistanceUnit.KM
        }
        _uiState.update { it.copy(distanceUnit = next) }
        viewModelScope.launch { repository.saveDistanceUnit(next) }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        _uiState.update { it.copy(distanceUnit = unit) }
        viewModelScope.launch { repository.saveDistanceUnit(unit) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch { repository.saveThemeMode(mode) }
    }

    fun cycleSafetyBuffer() {
        val next = when (_uiState.value.safetyBufferMinutes) {
            0 -> 1
            1 -> 2
            else -> 0
        }
        setSafetyBuffer(next)
    }

    fun setSafetyBuffer(minutes: Int) {
        val safe = minutes.coerceIn(0, 5)
        _uiState.update { current ->
            recomputeState(current.copy(safetyBufferMinutes = safe))
        }
        viewModelScope.launch { repository.saveSafetyBuffer(safe) }
    }

    fun setLastActionMinutesAgo(minutesAgo: Int?) {
        val now = System.currentTimeMillis()
        val epoch = if (minutesAgo == null || minutesAgo <= 0) {
            null
        } else {
            now - (minutesAgo * 60_000L)
        }
        _uiState.update { current ->
            recomputeState(
                current.copy(
                    lastActionEpochMillis = epoch,
                    nowEpochMillis = now
                )
            )
        }
        viewModelScope.launch { repository.saveLastActionEpoch(epoch) }
    }

    fun startTimer(durationSeconds: Int, label: String, switchToTimerTab: Boolean = false) {
        val safeSec = durationSeconds.coerceAtLeast(10)
        val now = System.currentTimeMillis()
        val nextTimer = TimerState(
            label = label,
            totalDurationSeconds = safeSec,
            targetEndEpochMillis = now + safeSec * 1000L,
            pausedRemainingSeconds = safeSec,
            isRunning = true
        )
        _uiState.update { current ->
            current.copy(
                timerState = nextTimer,
                nowEpochMillis = now,
                selectedTab = if (switchToTimerTab) AppTab.TIMER else current.selectedTab
            )
        }
        viewModelScope.launch { repository.saveTimerState(nextTimer) }
    }

    fun toggleTimerPauseResume() {
        val now = System.currentTimeMillis()
        val t = _uiState.value.timerState
        val updated = if (t.isRunning) {
            val remaining = t.remainingSeconds(now)
            t.copy(isRunning = false, pausedRemainingSeconds = remaining)
        } else {
            val remaining = t.pausedRemainingSeconds
            if (remaining <= 0) return
            t.copy(
                isRunning = true,
                targetEndEpochMillis = now + remaining * 1000L
            )
        }
        _uiState.update { it.copy(timerState = updated, nowEpochMillis = now) }
        viewModelScope.launch { repository.saveTimerState(updated) }
    }

    fun adjustTimerSeconds(deltaSeconds: Int) {
        val now = System.currentTimeMillis()
        val t = _uiState.value.timerState
        if (!t.isActive) return
        val updated = if (t.isRunning) {
            val remaining = (t.remainingSeconds(now) + deltaSeconds).coerceAtLeast(10)
            t.copy(
                totalDurationSeconds = maxOf(t.totalDurationSeconds, remaining),
                targetEndEpochMillis = now + remaining * 1000L
            )
        } else {
            val remaining = (t.pausedRemainingSeconds + deltaSeconds).coerceAtLeast(10)
            t.copy(
                totalDurationSeconds = maxOf(t.totalDurationSeconds, remaining),
                pausedRemainingSeconds = remaining
            )
        }
        _uiState.update { it.copy(timerState = updated, nowEpochMillis = now) }
        viewModelScope.launch { repository.saveTimerState(updated) }
    }

    fun resetTimer() {
        val empty = TimerState()
        _uiState.update { it.copy(timerState = empty) }
        viewModelScope.launch { repository.saveTimerState(empty) }
    }

    private fun nextAvailableLabel(existing: List<CoordPoint>): String {
        val used = existing.map { it.label }.toSet()
        var n = 1
        while ("Temp #$n" in used) {
            n++
        }
        return "Temp #$n"
    }

    private fun recomputeState(state: CoordShiftUiState): CoordShiftUiState {
        val coords = state.coordinates
        if (coords.isEmpty()) {
            return state.copy(evaluatedCoords = emptyList(), routeSummary = null)
        }

        val anchor = coords.first()
        val perm = state.permanentCoord
        val lastLoc = state.lastLocationCoord
        val isAnchorPermanent = perm != null && anchor.id == perm.id
        val bufferSec = state.safetyBufferMinutes * 60
        val elapsedSec = state.elapsedSinceLastActionSeconds
        val now = state.nowEpochMillis

        data class RawEval(
            val point: CoordPoint,
            val isCurrent: Boolean,
            val isPermanent: Boolean,
            val isLastLocation: Boolean,
            val distanceKm: Double,
            val bearing: Double,
            val rawCdSec: Int,
            val bufferedCdSec: Int,
            val remainingCdSec: Int,
            val distToPermKm: Double?,
            val cdToPermSec: Int?,
            val distFromLastKm: Double?,
            val cdFromLastSec: Int?
        )

        val rawEvals = coords.mapIndexed { idx, pt ->
            val isCurr = idx == 0
            val isPerm = perm != null && pt.id == perm.id
            val isLast = lastLoc != null && pt.id == lastLoc.id

            // Dual comparison 1: Cooldown to Permanent Base (shown on any non-permanent card when Permanent exists & anchor is Temp)
            val distToPerm: Double?
            val cdToPerm: Int?
            if (perm != null && !isPerm && !isAnchorPermanent) {
                val d = CooldownCalculator.distanceKm(pt.latitude, pt.longitude, perm.latitude, perm.longitude)
                val raw = CooldownCalculator.calculateCooldownSeconds(d)
                distToPerm = d
                cdToPerm = if (raw > 0) (raw + bufferSec).coerceAtMost(7200 + bufferSec) else 0
            } else {
                distToPerm = null
                cdToPerm = null
            }

            // Dual comparison 2: Cooldown from Last Temp Location (shown when Current is Permanent and user had a Last Location)
            val distFromLast: Double?
            val cdFromLast: Int?
            if (isAnchorPermanent && lastLoc != null && !isCurr && !isLast) {
                val d = CooldownCalculator.distanceKm(lastLoc.latitude, lastLoc.longitude, pt.latitude, pt.longitude)
                val raw = CooldownCalculator.calculateCooldownSeconds(d)
                val buffered = if (raw > 0) (raw + bufferSec).coerceAtMost(7200 + bufferSec) else 0
                distFromLast = d
                cdFromLast = (buffered - elapsedSec).coerceAtLeast(0)
            } else {
                distFromLast = null
                cdFromLast = null
            }

            if (isCurr) {
                RawEval(
                    point = pt,
                    isCurrent = true,
                    isPermanent = isPerm,
                    isLastLocation = isLast,
                    distanceKm = 0.0,
                    bearing = 0.0,
                    rawCdSec = 0,
                    bufferedCdSec = 0,
                    remainingCdSec = 0,
                    distToPermKm = distToPerm,
                    cdToPermSec = cdToPerm,
                    distFromLastKm = null,
                    cdFromLastSec = null
                )
            } else {
                val dist = CooldownCalculator.distanceKm(
                    anchor.latitude,
                    anchor.longitude,
                    pt.latitude,
                    pt.longitude
                )
                val bearing = CooldownCalculator.bearingDegrees(
                    anchor.latitude,
                    anchor.longitude,
                    pt.latitude,
                    pt.longitude
                )
                val rawCd = CooldownCalculator.calculateCooldownSeconds(dist)
                val buffered = if (rawCd > 0) (rawCd + bufferSec).coerceAtMost(7200 + bufferSec) else 0
                val remaining = (buffered - elapsedSec).coerceAtLeast(0)
                RawEval(
                    point = pt,
                    isCurrent = false,
                    isPermanent = isPerm,
                    isLastLocation = isLast,
                    distanceKm = dist,
                    bearing = bearing,
                    rawCdSec = rawCd,
                    bufferedCdSec = buffered,
                    remainingCdSec = remaining,
                    distToPermKm = distToPerm,
                    cdToPermSec = cdToPerm,
                    distFromLastKm = distFromLast,
                    cdFromLastSec = cdFromLast
                )
            }
        }

        // Best pick is the shortest-cooldown candidate (preferring temp candidates if any exist alongside Permanent)
        val candidates = rawEvals.drop(1)
        val tempCandidates = candidates.filterNot { it.isPermanent }
        val bestPool = if (tempCandidates.isNotEmpty()) tempCandidates else candidates
        val bestCandidateId = bestPool.minByOrNull { it.distanceKm }?.point?.id
        val maxCooldownSec = candidates.maxOfOrNull { it.bufferedCdSec } ?: 0

        val evaluated = rawEvals.map { raw ->
            val nearestRow = CooldownCalculator.referenceChart[
                CooldownCalculator.nearestChartRowIndex(raw.distanceKm)
            ]
            val tier = if (raw.isCurrent) {
                CooldownTier.READY
            } else {
                CooldownCalculator.tierForCooldownSeconds(raw.remainingCdSec)
            }
            EvaluatedCoord(
                point = raw.point,
                isCurrent = raw.isCurrent,
                isPermanent = raw.isPermanent,
                isLastLocation = raw.isLastLocation,
                isBestPick = !raw.isCurrent && bestPool.isNotEmpty() && raw.point.id == bestCandidateId,
                distanceKm = raw.distanceKm,
                bearingDegrees = raw.bearing,
                cardinalDirection = CooldownCalculator.cardinalDirection(raw.bearing),
                rawCooldownSeconds = raw.rawCdSec,
                bufferedCooldownSeconds = raw.bufferedCdSec,
                remainingCooldownSeconds = raw.remainingCdSec,
                readyAtEpochMillis = now + raw.remainingCdSec * 1000L,
                chartLabel = nearestRow.displayCooldown,
                tier = tier,
                savingsVsFarthestSeconds = if (raw.isCurrent) {
                    0
                } else {
                    (maxCooldownSec - raw.bufferedCdSec).coerceAtLeast(0)
                },
                distanceToPermanentKm = raw.distToPermKm,
                cooldownToPermanentSeconds = raw.cdToPermSec,
                distanceFromLastLocationKm = raw.distFromLastKm,
                cooldownFromLastLocationSeconds = raw.cdFromLastSec
            )
        }

        val routeSummary = CooldownCalculator.computeRouteSummary(
            points = coords,
            safetyBufferMinutes = state.safetyBufferMinutes
        )

        return state.copy(
            evaluatedCoords = evaluated,
            routeSummary = routeSummary
        )
    }
}
