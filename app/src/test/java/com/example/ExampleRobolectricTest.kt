package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.NotificationAlertStyle
import com.example.data.VibrationPattern
import com.example.viewmodel.CoordShiftViewModel
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CoordShift", appName)
  }

  @Test
  fun `permanent base vs temp workflow jump dual comparison and reset`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = CoordShiftViewModel(app)

    // 1. Set Permanent Base (Tokyo)
    assertNull(vm.setPermanentFromRaw("Home Tokyo", "35.6895, 139.6917"))
    assertNotNull(vm.uiState.value.permanentCoord)
    assertTrue(vm.uiState.value.isCurrentPermanent)

    // 2. Paste 2 temporary coordinates (Yokohama & Osaka)
    val added = vm.addFromRawText("Yokohama: 35.4437, 139.6380\nOsaka: 34.6937, 135.5023")
    assertEquals(2, added)
    assertEquals(2, vm.uiState.value.tempCount)

    // 3. Jump to Yokohama -> triggers cooldown timer & tracks cooldown back to Permanent
    val yokohamaId = vm.uiState.value.coordinates.first { it.label == "Yokohama" }.id
    val jumpOutcome = vm.jumpToCoordinate(yokohamaId)
    assertNotNull(jumpOutcome)
    assertTrue(jumpOutcome!!.jumpCooldownSeconds > 0)
    assertTrue((jumpOutcome.returnToPermanentSeconds ?: 0) > 0)
    assertTrue(vm.uiState.value.timerState.isRunning)
    assertNotNull(vm.uiState.value.returnToPermanentInfo)

    // 4. Switch Current back to Permanent -> remembers Yokohama as Last Location
    vm.switchAnchorToPermanent(triggerTimer = false)
    assertTrue(vm.uiState.value.isCurrentPermanent)
    assertEquals("Yokohama", vm.uiState.value.lastLocationCoord?.label)

    // Check Osaka: now compared to BOTH Permanent (primary) AND Last Location (Yokohama)
    val osakaEval = vm.uiState.value.evaluatedCoords.first { it.point.label == "Osaka" }
    assertTrue(osakaEval.distanceKm > 300.0)
    assertNotNull(osakaEval.distanceFromLastLocationKm)
    assertNotNull(osakaEval.cooldownFromLastLocationSeconds)

    // 5. Reset to Permanent clears all temporary coords while preserving Permanent Base
    vm.clearTempsAndResetToPermanent()
    assertEquals(0, vm.uiState.value.tempCount)
    assertEquals(1, vm.uiState.value.coordinates.size)
    assertEquals("Home Tokyo", vm.uiState.value.currentAnchor?.label)
    assertNull(vm.uiState.value.lastLocationCoord)
  }

  @Test
  fun `notification center preferences customize alerts and master mute cleanly`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = CoordShiftViewModel(app)

    vm.updateNotificationPreferences {
      it.copy(
        preAlertMinutes = 5,
        alertStyle = NotificationAlertStyle.VIBRATE_ONLY,
        vibrationPattern = VibrationPattern.URGENT,
        customReadyMessage = "Safe to jump!"
      )
    }

    val updated = vm.uiState.value.notificationPrefs
    assertEquals(5, updated.preAlertMinutes)
    assertEquals(NotificationAlertStyle.VIBRATE_ONLY, updated.alertStyle)
    assertEquals(VibrationPattern.URGENT, updated.vibrationPattern)
    assertEquals("Safe to jump!", updated.customReadyMessage)
    assertEquals("5m early · Ready", updated.activeAlertSummary)

    vm.updateNotificationPreferences { it.copy(masterEnabled = false) }
    assertFalse(vm.uiState.value.notificationPrefs.masterEnabled)
    assertEquals("Muted", vm.uiState.value.notificationPrefs.activeAlertSummary)
  }

  @Test
  fun `notification delivery slot deduplicates concurrent foreground and alarm triggers`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.notifications.CooldownNotificationManager.clearDeliverySlots(context)

    val epochA = 1_900_000_000_000L
    val epochB = 1_900_000_060_000L

    assertTrue(
      com.example.notifications.CooldownNotificationManager.claimDeliverySlot(
        context,
        isPreAlert = false,
        targetEndEpochMillis = epochA
      )
    )
    // Second claim for same timer epoch must be rejected
    assertFalse(
      com.example.notifications.CooldownNotificationManager.claimDeliverySlot(
        context,
        isPreAlert = false,
        targetEndEpochMillis = epochA
      )
    )
    // Pre-alert slot for same epoch is independent
    assertTrue(
      com.example.notifications.CooldownNotificationManager.claimDeliverySlot(
        context,
        isPreAlert = true,
        targetEndEpochMillis = epochA
      )
    )
    // New timer epoch can deliver again
    assertTrue(
      com.example.notifications.CooldownNotificationManager.claimDeliverySlot(
        context,
        isPreAlert = false,
        targetEndEpochMillis = epochB
      )
    )
  }

  @Test
  fun `nearby jump preserves active running cooldown timer and updates destination label`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = CoordShiftViewModel(app)

    assertNull(vm.setPermanentFromRaw("Home", "35.68950, 139.69170"))
    // Add a spot only ~15 meters away (0s cooldown)
    assertEquals(1, vm.addFromRawText("Next Door: 35.68960, 139.69175"))
    vm.startTimer(600, "Old Spot")
    assertTrue(vm.uiState.value.timerState.isRunning)

    val nextDoorId = vm.uiState.value.coordinates.first { it.label == "Next Door" }.id
    val outcome = vm.jumpToCoordinate(nextDoorId)
    assertNotNull(outcome)
    // Active 600s cooldown must NOT be deleted or shortened by a 0s jump
    assertTrue(outcome!!.jumpCooldownSeconds >= 595)
    assertTrue(vm.uiState.value.timerState.isActive)
    assertTrue(vm.uiState.value.timerState.isRunning)
    assertEquals("Next Door", vm.uiState.value.currentAnchor?.label)
    assertEquals("Next Door", vm.uiState.value.timerState.label)
  }

  @Test
  fun `jumped coordinate and cooldown timer persist across app restarts`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = CoordShiftViewModel(app)

    assertNull(vm.setPermanentFromRaw("Home Tokyo", "35.68950, 139.69170"))
    assertEquals(1, vm.addFromRawText("Yokohama: 35.44370, 139.63800"))
    val yokohamaId = vm.uiState.value.coordinates.first { it.label == "Yokohama" }.id

    val outcome = vm.jumpToCoordinate(yokohamaId)
    assertNotNull(outcome)
    assertTrue(outcome!!.jumpCooldownSeconds > 0)

    // Pump main looper so queued DataStore writes complete on IO & resume cleanly
    val repo = com.example.data.CoordPreferencesRepository(app)
    var persisted: com.example.data.PersistedSession? = null
    val deadline = System.currentTimeMillis() + 5_000L
    while (System.currentTimeMillis() < deadline) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      val snapshot = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        repo.sessionFlow.first()
      }
      if (snapshot.activeCoordinates.firstOrNull()?.label == "Yokohama" &&
        snapshot.timerState.isRunning &&
        snapshot.timerState.label == "Yokohama"
      ) {
        persisted = snapshot
        break
      }
      Thread.sleep(25L)
    }

    assertNotNull("Expected jumped coordinate and active timer to persist in DataStore", persisted)
    assertEquals("Yokohama", persisted!!.activeCoordinates.first().label)
    assertEquals("Home Tokyo", persisted.permanentCoord?.label)
    assertTrue(persisted.timerState.isRunning)
    assertTrue(persisted.timerState.remainingSeconds(System.currentTimeMillis()) > 0)

    // Simulate cold app restart with a fresh ViewModel reading from DataStore
    val restartedVm = CoordShiftViewModel(app)
    val restoreDeadline = System.currentTimeMillis() + 5_000L
    while (System.currentTimeMillis() < restoreDeadline) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      if (restartedVm.uiState.value.currentAnchor?.label == "Yokohama" &&
        restartedVm.uiState.value.timerState.isRunning
      ) {
        break
      }
      Thread.sleep(25L)
    }

    assertEquals("Yokohama", restartedVm.uiState.value.currentAnchor?.label)
    assertEquals("Home Tokyo", restartedVm.uiState.value.permanentCoord?.label)
    assertTrue(restartedVm.uiState.value.timerState.isRunning)
    assertEquals("Yokohama", restartedVm.uiState.value.timerState.label)
  }

  @Test
  fun `clear on jumped temp spot clears only extra candidates and protects active cooldown spot`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = CoordShiftViewModel(app)

    assertNull(vm.setPermanentFromRaw("Home Tokyo", "35.68950, 139.69170"))
    assertEquals(2, vm.addFromRawText("Yokohama: 35.44370, 139.63800\nOsaka: 34.69370, 135.50230"))

    val yokohamaId = vm.uiState.value.coordinates.first { it.label == "Yokohama" }.id
    assertNotNull(vm.jumpToCoordinate(yokohamaId))
    assertEquals("Yokohama", vm.uiState.value.currentAnchor?.label)
    // Jumping from Home to Temp must never self-reference destination as lastLocationCoord
    assertNull(vm.uiState.value.lastLocationCoord)
    assertTrue(vm.uiState.value.timerState.isRunning)

    // First Clear removes only unselected extra temp candidate (Osaka), keeping jumped spot + Home + timer
    val firstClearMsg = vm.clearTempsAndResetToPermanent()
    assertEquals("Cleared 1 extra spot", firstClearMsg)
    assertEquals("Yokohama", vm.uiState.value.currentAnchor?.label)
    assertEquals(2, vm.uiState.value.coordinates.size)
    assertTrue(vm.uiState.value.timerState.isRunning)

    // Second Clear while cooldown timer is active must NOT delete the active jumped spot
    val secondClearMsg = vm.clearTempsAndResetToPermanent()
    assertNull(secondClearMsg)
    assertEquals("Yokohama", vm.uiState.value.currentAnchor?.label)
    assertTrue(vm.uiState.value.timerState.isRunning)
  }

  @Test
  fun `new pasted coordinate appears at top of compare next and setting Home deduplicates`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = CoordShiftViewModel(app)

    assertNull(vm.setPermanentFromRaw("Home Tokyo", "35.68950, 139.69170"))
    assertEquals(1, vm.addFromRawText("Yokohama: 35.44370, 139.63800"))
    assertEquals(1, vm.addFromRawText("Osaka: 34.69370, 135.50230"))

    // Most recently pasted spot sits at index 1 (immediately below Current Anchor)
    assertEquals("Home Tokyo", vm.uiState.value.coordinates[0].label)
    assertEquals("Osaka", vm.uiState.value.coordinates[1].label)
    assertEquals("Yokohama", vm.uiState.value.coordinates[2].label)

    // Pasting duplicate mentions existing spot name
    assertEquals(0, vm.addFromRawText("35.44370, 139.63800"))
    assertEquals("Already in list as Yokohama", vm.uiState.value.inputError)

    // Setting Home to an existing coordinate deduplicates cleanly
    assertNull(vm.setPermanentFromRaw("New Base", "35.44370, 139.63800"))
    assertEquals(2, vm.uiState.value.coordinates.size)
    assertEquals("New Base", vm.uiState.value.permanentCoord?.label)
  }
}
