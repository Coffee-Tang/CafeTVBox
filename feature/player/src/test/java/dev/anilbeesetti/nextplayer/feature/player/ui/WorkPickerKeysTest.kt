package dev.anilbeesetti.nextplayer.feature.player.ui

import dev.anilbeesetti.nextplayer.core.model.WorkPickerDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkPickerKeysTest {

    @Test
    fun `dpad and system navigation keys move the picker`() {
        assertEquals(WorkPickerDirection.UP, workPickerDirectionOf(KEYCODE_DPAD_UP))
        assertEquals(WorkPickerDirection.DOWN, workPickerDirectionOf(KEYCODE_DPAD_DOWN))
        assertEquals(WorkPickerDirection.LEFT, workPickerDirectionOf(KEYCODE_DPAD_LEFT))
        assertEquals(WorkPickerDirection.RIGHT, workPickerDirectionOf(KEYCODE_DPAD_RIGHT))
        assertEquals(WorkPickerDirection.LEFT, workPickerDirectionOf(KEYCODE_SYSTEM_NAVIGATION_LEFT))
    }

    @Test
    fun `center enter and a confirm a pick`() {
        assertTrue(isWorkPickerConfirmKey(KEYCODE_DPAD_CENTER))
        assertTrue(isWorkPickerConfirmKey(KEYCODE_ENTER))
        assertTrue(isWorkPickerConfirmKey(KEYCODE_BUTTON_A))
        assertTrue(isWorkPickerConfirmKey(KEYCODE_SPACE))
        assertTrue(isWorkPickerConfirmKey(KEYCODE_BUTTON_SELECT))
        assertFalse(isWorkPickerConfirmKey(KEYCODE_DPAD_LEFT))
    }

    @Test
    fun `unrelated keys are left for the player`() {
        assertFalse(isWorkPickerHandledKey(KEYCODE_BACK))
        assertEquals(null, workPickerDirectionOf(KEYCODE_BACK))
    }

    @Test
    fun `back hides the control panel only when it is showing and no overlay is open`() {
        assertTrue(isBackKey(KEYCODE_BACK))
        assertTrue(shouldHideControlsOnBack(overlayOpen = false, controlsVisible = true))
        assertFalse(shouldHideControlsOnBack(overlayOpen = true, controlsVisible = true))
        assertFalse(shouldHideControlsOnBack(overlayOpen = false, controlsVisible = false))
    }

    @Test
    fun `back dismisses an overlay on the activity so the picker cannot leak it`() {
        assertTrue(shouldDismissOverlayOnBack(overlayOpen = true))
        assertFalse(shouldDismissOverlayOnBack(overlayOpen = false))
        assertFalse(isWorkPickerHandledKey(KEYCODE_BACK))
    }

    @Test
    fun `dismissing back swallows only the matching key up`() {
        assertTrue(shouldSwallowDismissingBackUp(swallowArmed = true, isBack = true, isActionUp = true))
        assertFalse(shouldSwallowDismissingBackUp(swallowArmed = true, isBack = true, isActionUp = false))
        assertFalse(shouldSwallowDismissingBackUp(swallowArmed = false, isBack = true, isActionUp = true))
        assertFalse(shouldSwallowDismissingBackUp(swallowArmed = true, isBack = false, isActionUp = true))
    }

    @Test
    fun `chrome takes focus off the surface, and with nothing up the surface keeps it`() {
        assertTrue(shouldDisableSurfaceFocus(overlayOpen = true, controlsVisible = false))
        assertTrue(shouldDisableSurfaceFocus(overlayOpen = false, controlsVisible = true))
        assertFalse(shouldDisableSurfaceFocus(overlayOpen = false, controlsVisible = false))
    }

    @Test
    fun `chrome keys are forwarded except while the work picker owns them`() {
        assertTrue(shouldForwardChromeKeys(overlayIsWorkPicker = false, chromeVisible = true))
        assertFalse(shouldForwardChromeKeys(overlayIsWorkPicker = true, chromeVisible = true))
        assertFalse(shouldForwardChromeKeys(overlayIsWorkPicker = false, chromeVisible = false))
    }

    @Test
    fun `hidden player keys are handled without focusing the root box`() {
        assertTrue(shouldHandleHiddenPlayerKey(overlayOpen = false, controlsVisible = false))
        assertFalse(shouldHandleHiddenPlayerKey(overlayOpen = false, controlsVisible = true))
        assertFalse(shouldHandleHiddenPlayerKey(overlayOpen = true, controlsVisible = false))
    }

    @Test
    fun `the control panel dims the video, whatever the device is taken for`() {
        assertTrue(shouldShowControlScrim(controlsVisible = true, controlsLocked = false, overlayOpen = false))
        assertFalse(shouldShowControlScrim(controlsVisible = false, controlsLocked = false, overlayOpen = false))
    }

    @Test
    fun `a locked panel or an overlay of its own leaves the video alone`() {
        assertFalse(shouldShowControlScrim(controlsVisible = true, controlsLocked = true, overlayOpen = false))
        assertFalse(shouldShowControlScrim(controlsVisible = true, controlsLocked = false, overlayOpen = true))
    }
}
