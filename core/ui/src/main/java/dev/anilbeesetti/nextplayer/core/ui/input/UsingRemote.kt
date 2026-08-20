package dev.anilbeesetti.nextplayer.core.ui.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager

/**
 * Whether the viewer is working the screen with a remote or a keyboard rather than with a finger.
 *
 * Whether focus is drawn, and whether a panel takes focus as it opens, follows from how the screen
 * is being worked right now rather than from what kind of device it is. A screen that has both a
 * touch panel and a remote is each of those at different moments, and asking the device answers for
 * neither: it reports that it is no television, so nothing written for a remote ever applies to it.
 *
 * Android already keeps this as touch mode, which any arrow key leaves and any touch returns to, and
 * Compose reads it here. The answer changes within a session, so what is drawn changes with it.
 */
val usingRemote: Boolean
    @Composable get() = LocalInputModeManager.current.inputMode == InputMode.Keyboard
