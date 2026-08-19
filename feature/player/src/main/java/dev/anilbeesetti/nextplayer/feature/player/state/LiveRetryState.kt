package dev.anilbeesetti.nextplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@UnstableApi
@Composable
fun rememberLiveRetryState(player: Player): LiveRetryState {
    val liveRetryState = remember(player) { LiveRetryState(player) }
    LaunchedEffect(liveRetryState) { liveRetryState.observe() }
    return liveRetryState
}

/** What to do about playback having stopped, named for the thing worth trying. */
enum class PlaybackFailureResponse {

    /** Ask the line in use for the live edge again, the window it was reading having run out. */
    RETRY_AT_LIVE_EDGE,

    /** Give up on the line in use for the next one the channel offers. */
    SWITCH_LINE,

    /** Tell the viewer, nothing being left to try. */
    GIVE_UP,
}

/**
 * How playback failing is worth answering.
 *
 * A broadcast is worth asking again before anything else. A live window rolls out from under a
 * player that falls behind it, and the segment it goes on to ask for is one the server no longer
 * has; the gateways carrying these channels answer a fresh request with a window they do still
 * hold. Only once asking again has stopped helping has a line earned being given up on, and only
 * once the channel has no line left is there anything to tell the viewer.
 *
 * None of that applies to a file, whose missing bytes will still be missing on a second reading,
 * so [isLive] keeps the asking to the broadcasts it can help.
 */
fun responseToPlaybackFailure(
    isLive: Boolean,
    mayRetryLine: Boolean,
    hasAnotherLine: Boolean,
): PlaybackFailureResponse = when {
    isLive && mayRetryLine -> PlaybackFailureResponse.RETRY_AT_LIVE_EDGE
    hasAnotherLine -> PlaybackFailureResponse.SWITCH_LINE
    else -> PlaybackFailureResponse.GIVE_UP
}

/**
 * How many times over the line in use may still be asked again.
 *
 * A line has to run out of chances, or a channel that is simply gone would be asked for forever.
 * A line that goes on to play is worth its full allowance again, so that a broadcast dropping once
 * an hour is met the same way each time rather than being abandoned for the sum of the failures an
 * evening has collected.
 */
internal data class RetryAllowance(
    val perLine: Int = 2,
    val spent: Int = 0,
) {
    val hasRetryLeft: Boolean get() = spent < perLine

    fun spend(): RetryAllowance = copy(spent = (spent + 1).coerceAtMost(perLine))

    fun renewed(): RetryAllowance = copy(spent = 0)
}

/**
 * Asks a broadcast for the live edge again when it stops, before its channel gives up on the line.
 *
 * A line is only settled once it has played for [settledAfter] without stopping, which is what
 * earns it its allowance back. Anything shorter is the failing the allowance exists to bound: the
 * streams this is meant for play for a few seconds at a time between running off the end of a
 * window that never rolls.
 */
@UnstableApi
@Stable
class LiveRetryState(
    private val player: Player,
    retriesPerLine: Int = 2,
    private val settledAfter: Duration = 60.seconds,
) {
    private var allowance: RetryAllowance by mutableStateOf(RetryAllowance(perLine = retriesPerLine))

    /** Whether the line in use has a retry left to spend. */
    val mayRetryLine: Boolean get() = allowance.hasRetryLeft

    /**
     * Asks the line in use for the live edge again, spending one of its retries.
     *
     * Preparing alone would resume from where the player left off, which is the point the window
     * has already rolled past. The seek is what makes the request one the server can still answer.
     */
    fun retryAtLiveEdge() {
        allowance = allowance.spend()
        player.seekToDefaultPosition()
        player.prepare()
    }

    /** Hands a line just moved onto its own allowance, unspent by the line before it. */
    fun onLineChanged() {
        allowance = allowance.renewed()
    }

    suspend fun observe() {
        playingStretches().collectLatest { isPlaying ->
            if (!isPlaying) return@collectLatest
            delay(settledAfter)
            allowance = allowance.renewed()
        }
    }

    /**
     * Whether a picture is arriving, reported afresh each time that changes.
     *
     * Collecting the latest of these is what times a stretch of playback: a stretch cut short by
     * the stream stopping is abandoned along with the report that opened it.
     */
    private fun playingStretches(): Flow<Boolean> = channelFlow {
        trySend(player.isPlaying)
        player.listen { events ->
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                trySend(player.isPlaying)
            }
        }
    }.distinctUntilChanged()
}
