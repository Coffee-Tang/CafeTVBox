package dev.anilbeesetti.nextplayer.feature.player.state

import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.feature.player.extensions.formatted
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun rememberMediaPresentationState(player: Player): MediaPresentationState {
    val mediaPresentationState = remember { MediaPresentationState(player) }
    LaunchedEffect(player) { mediaPresentationState.observe() }
    return mediaPresentationState
}

@Stable
class MediaPresentationState(
    private val player: Player,
    @param:IntRange(from = 0) private val tickIntervalMs: Long = 500,
) {
    var position: Long by mutableLongStateOf(0L)
        private set

    var duration: Long by mutableLongStateOf(0L)
        private set

    var isPlaying: Boolean by mutableStateOf(false)
        private set

    var isLoading: Boolean by mutableStateOf(true)
        private set

    var isBuffering: Boolean by mutableStateOf(false)
        private set

    /**
     * Whether what is playing is a broadcast rather than something with an end of its own.
     *
     * Media3 only reports a live window once the manifest has said as much, which can come after
     * playback has begun. Having seen it live once is therefore enough, and spares the controls
     * that a broadcast has no use for from being shown for a moment before going away again.
     */
    var isLive: Boolean by mutableStateOf(false)
        private set

    /**
     * Where in the window the player treats as live, so that a viewer who has rewound can be told
     * apart from one watching the broadcast go out.
     *
     * This is the point [Player.seekToDefaultPosition] returns to, and it only moves when the
     * window rolls, so playback runs on past it between one roll and the next.
     */
    var liveEdgePosition: Long by mutableLongStateOf(0L)
        private set

    private val window = Timeline.Window()

    suspend fun observe() {
        updatePosition()
        updateDuration()
        updateIsLive()
        isPlaying = player.isPlaying
        isLoading = player.isLoading
        isBuffering = player.playbackState == Player.STATE_BUFFERING

        coroutineScope {
            launch {
                player.listen { events ->
                    if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                        this@MediaPresentationState.isLive = false
                    }

                    if (events.containsAny(
                            Player.EVENT_MEDIA_ITEM_TRANSITION,
                            Player.EVENT_TIMELINE_CHANGED,
                            Player.EVENT_PLAYBACK_STATE_CHANGED,
                        )
                    ) {
                        updateDuration()
                        updateIsLive()
                    }

                    if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                        this@MediaPresentationState.isBuffering = player.playbackState == Player.STATE_BUFFERING
                    }

                    if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                        this@MediaPresentationState.isPlaying = player.isPlaying
                    }

                    if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
                        updatePosition()
                    }

                    if (events.containsAny(Player.EVENT_IS_LOADING_CHANGED)) {
                        this@MediaPresentationState.isLoading = player.isLoading
                    }
                }
            }

            while (true) {
                delay(tickIntervalMs)
                if (player.isPlaying) {
                    updatePosition()
                }
            }
        }
    }

    private fun updatePosition() {
        position = player.currentPosition.coerceAtLeast(0L)
        updateLiveEdgePosition()
    }

    private fun updateDuration() {
        duration = player.duration.coerceAtLeast(0L)
    }

    private fun updateIsLive() {
        isLive = isLive || player.isCurrentMediaItemLive
    }

    private fun updateLiveEdgePosition() {
        val timeline = player.currentTimeline
        liveEdgePosition = when {
            timeline.isEmpty -> 0L
            else -> timeline.getWindow(player.currentMediaItemIndex, window)
                .defaultPositionMs
                .coerceAtLeast(0L)
        }
    }
}

val MediaPresentationState.positionFormatted: String
    get() = position.milliseconds.formatted()

val MediaPresentationState.durationFormatted: String
    get() = duration.milliseconds.formatted()

val MediaPresentationState.pendingPositionFormatted: String
    get() = (duration - position).milliseconds.formatted()

/**
 * Where the viewer is in a broadcast, which only means anything said against the live edge.
 *
 * A live window is not a running time. It is the last stretch of the broadcast the server still
 * holds, often half a minute of it, and reading a position against that puts every viewer near
 * zero of a total that never grows, which is how a working channel came to read as a broken one.
 */
sealed interface LivePosition {

    /** Watching the broadcast as it goes out. */
    data object AtLiveEdge : LivePosition

    /** Watching [behindBy] earlier than it goes out, having rewound into the window. */
    data class BehindLiveEdge(val behindBy: Duration) : LivePosition
}

/**
 * How far behind the live edge the viewer is, or that they are not behind it at all.
 *
 * The player is only told where the edge is when the window rolls and plays on in between, so its
 * position drifts a segment or so past the edge it last heard about. [driftTolerance] covers that,
 * and with it a rewind too small to have been meant.
 */
fun livePositionOf(
    positionMs: Long,
    liveEdgeMs: Long,
    driftTolerance: Duration = 10.seconds,
): LivePosition {
    val behindBy = (liveEdgeMs - positionMs).milliseconds
    return when {
        behindBy <= driftTolerance -> LivePosition.AtLiveEdge
        else -> LivePosition.BehindLiveEdge(behindBy)
    }
}

/**
 * Whether a seek bar has anywhere to take the viewer.
 *
 * A channel with only seconds of its broadcast still on the server offers no rewind worth the
 * name, and a bar spanning that little turns a nudge into a jump past the end of the window, which
 * asks for a segment the server has already dropped. Anything with a minute or more behind it is
 * worth keeping the bar for, which is the DVR rewind it was kept for in the first place.
 */
fun offersRewind(
    isLive: Boolean,
    windowMs: Long,
    shortestWorthwhileWindow: Duration = 1.minutes,
): Boolean = !isLive || windowMs.milliseconds >= shortestWorthwhileWindow
