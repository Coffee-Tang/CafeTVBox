package dev.anilbeesetti.nextplayer.feature.player.ui

import dev.anilbeesetti.nextplayer.core.model.WorkPickerDirection

/** Android [android.view.KeyEvent] codes the work picker understands. */
internal const val KEYCODE_DPAD_UP = 19
internal const val KEYCODE_DPAD_DOWN = 20
internal const val KEYCODE_DPAD_LEFT = 21
internal const val KEYCODE_DPAD_RIGHT = 22
internal const val KEYCODE_DPAD_CENTER = 23
internal const val KEYCODE_ENTER = 66
internal const val KEYCODE_NUMPAD_ENTER = 160
internal const val KEYCODE_BUTTON_A = 96
internal const val KEYCODE_SPACE = 62
internal const val KEYCODE_BUTTON_SELECT = 109
internal const val KEYCODE_SYSTEM_NAVIGATION_UP = 280
internal const val KEYCODE_SYSTEM_NAVIGATION_DOWN = 281
internal const val KEYCODE_SYSTEM_NAVIGATION_LEFT = 282
internal const val KEYCODE_SYSTEM_NAVIGATION_RIGHT = 283
internal const val KEYCODE_BACK = 4
internal const val KEYCODE_ESCAPE = 111

fun workPickerDirectionOf(keyCode: Int): WorkPickerDirection? = when (keyCode) {
    KEYCODE_DPAD_UP, KEYCODE_SYSTEM_NAVIGATION_UP -> WorkPickerDirection.UP
    KEYCODE_DPAD_DOWN, KEYCODE_SYSTEM_NAVIGATION_DOWN -> WorkPickerDirection.DOWN
    KEYCODE_DPAD_LEFT, KEYCODE_SYSTEM_NAVIGATION_LEFT -> WorkPickerDirection.LEFT
    KEYCODE_DPAD_RIGHT, KEYCODE_SYSTEM_NAVIGATION_RIGHT -> WorkPickerDirection.RIGHT
    else -> null
}

fun isWorkPickerConfirmKey(keyCode: Int): Boolean = when (keyCode) {
    KEYCODE_DPAD_CENTER,
    KEYCODE_ENTER,
    KEYCODE_NUMPAD_ENTER,
    KEYCODE_BUTTON_A,
    KEYCODE_SPACE,
    KEYCODE_BUTTON_SELECT,
    -> true
    else -> false
}

fun isWorkPickerHandledKey(keyCode: Int): Boolean =
    workPickerDirectionOf(keyCode) != null || isWorkPickerConfirmKey(keyCode)

fun isBackKey(keyCode: Int): Boolean =
    keyCode == KEYCODE_BACK || keyCode == KEYCODE_ESCAPE

fun shouldHideControlsOnBack(overlayOpen: Boolean, controlsVisible: Boolean): Boolean =
    !overlayOpen && controlsVisible

/** Overlay Back stays on the Activity so PlayerView cannot swallow it. */
fun shouldDismissOverlayOnBack(overlayOpen: Boolean): Boolean = overlayOpen

fun shouldSwallowDismissingBackUp(
    swallowArmed: Boolean,
    isBack: Boolean,
    isActionUp: Boolean,
): Boolean = swallowArmed && isBack && isActionUp

/**
 * A focused SurfaceView swallows D-pad and OK, so chrome takes focus off it while it is up.
 *
 * With nothing up the surface keeps focus instead of nothing having it, which is what stops the
 * framework from spending a remote key on leaving touch mode.
 */
fun shouldDisableSurfaceFocus(overlayOpen: Boolean, controlsVisible: Boolean): Boolean =
    overlayOpen || controlsVisible

fun shouldForwardChromeKeys(overlayIsWorkPicker: Boolean, chromeVisible: Boolean): Boolean =
    !overlayIsWorkPicker && chromeVisible

/** Hidden-state player keys stay on the Activity; the full-screen root Box must not take focus. */
fun shouldHandleHiddenPlayerKey(overlayOpen: Boolean, controlsVisible: Boolean): Boolean =
    !overlayOpen && !controlsVisible

/** An overlay dims the video itself, and locked controls are too sparse to need it. */
fun shouldShowControlScrim(
    controlsVisible: Boolean,
    controlsLocked: Boolean,
    overlayOpen: Boolean,
): Boolean = controlsVisible && !controlsLocked && !overlayOpen
