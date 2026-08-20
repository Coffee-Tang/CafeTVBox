package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.input.usingRemote

/**
 * Draws a ring around a component while it (or a child) holds D-pad focus, so that focus can be
 * seen. Nothing is drawn when [enabled] is `false`, which is how a finger gets an unmarked screen.
 *
 * Pass a [shape] that matches the component's own outline (default [CircleShape] for icon buttons).
 */
@Composable
fun Modifier.tvFocusRing(
    enabled: Boolean,
    shape: Shape = CircleShape,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 3.dp,
): Modifier {
    if (!enabled) return this
    var focused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { focused = it.hasFocus }
        .thenIf(focused) { border(width = width, color = color, shape = shape) }
}

/**
 * Overload of [tvFocusRing] that asks how the screen is being worked, for the call sites that have
 * no reason to care beyond wanting focus to show when it is a remote doing the moving.
 */
@Composable
fun Modifier.tvFocusRing(
    shape: Shape = CircleShape,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 3.dp,
): Modifier = tvFocusRing(enabled = usingRemote, shape = shape, color = color, width = width)
