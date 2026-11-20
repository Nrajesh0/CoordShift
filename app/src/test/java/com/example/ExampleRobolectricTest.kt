package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.NotificationAlertStyle
import com.example.data.VibrationPattern
import com.example.viewmodel.CoordShiftViewModel
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
}
