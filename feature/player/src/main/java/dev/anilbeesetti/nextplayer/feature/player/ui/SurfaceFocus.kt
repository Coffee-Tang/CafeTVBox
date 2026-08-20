package dev.anilbeesetti.nextplayer.feature.player.ui

import android.os.Build
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup

/**
 * Finds every [SurfaceView] under [root] and sets whether it may take D-pad focus.
 *
 * A surface allowed to take focus takes it at once, because `ViewRootImpl` spends the first
 * navigation key on leaving touch mode unless some view outside a ViewGroup already holds focus,
 * and that key never reaches the activity. Any touch puts the window back into touch mode, so on a
 * device with both a remote and a touchscreen this has to hold for as long as the video is what is
 * on screen, not only at startup.
 *
 * The default focus highlight goes with it: a [SurfaceView] is an ordinary view, so the framework
 * would otherwise paint that highlight across the whole video.
 */
internal fun setDescendantSurfaceFocusable(root: View, focusable: Boolean): Int {
    var count = 0
    fun walk(view: View) {
        if (view is SurfaceView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                view.defaultFocusHighlightEnabled = false
            }
            view.isFocusable = focusable
            view.isFocusableInTouchMode = focusable
            if (focusable) view.requestFocus() else view.clearFocus()
            count++
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                walk(view.getChildAt(index))
            }
        }
    }
    walk(root)
    return count
}
