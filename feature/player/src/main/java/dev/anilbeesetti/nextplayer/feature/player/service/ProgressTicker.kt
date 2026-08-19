package dev.anilbeesetti.nextplayer.feature.player.service

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Writes where playback has reached at a steady interval, for as long as it is playing.
 *
 * Otherwise the position is only written when something happens to playback — it starts, it is
 * paused, the next episode begins, the viewer leaves. An hour watched straight through writes
 * nothing until it ends, so a box switched off at the socket, or a process the system reclaims,
 * loses the whole hour. [interval] is what such an ending can cost.
 */
internal class ProgressTicker(
    private val scope: CoroutineScope,
    private val interval: Duration = DEFAULT_INTERVAL,
    private val save: suspend () -> Unit,
) {

    private var ticking: Job? = null

    /**
     * Starts writing while [isPlaying], and stops when it is not.
     *
     * Being told what it is already doing changes nothing, so a pause reported twice does not
     * restart the interval, and playback reported twice does not write twice as often.
     */
    fun playing(isPlaying: Boolean) {
        if (isPlaying == (ticking?.isActive == true)) return
        ticking?.cancel()
        ticking = if (isPlaying) scope.launch { tick() } else null
    }

    private suspend fun tick() {
        while (true) {
            // Waits first: whatever started playback has just written the position itself.
            delay(interval)
            save()
        }
    }

    private companion object {
        val DEFAULT_INTERVAL = 10.seconds
    }
}
