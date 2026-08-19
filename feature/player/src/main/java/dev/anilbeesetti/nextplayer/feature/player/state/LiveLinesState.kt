package dev.anilbeesetti.nextplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@UnstableApi
@Composable
fun rememberLiveLinesState(player: Player, lines: List<String>): LiveLinesState {
    val liveLinesState = remember(player, lines) { LiveLinesState(player, lines) }
    LaunchedEffect(liveLinesState) { liveLinesState.observe() }
    return liveLinesState
}

/**
 * Moves a broadcast onto the next line its channel offers when the one in use does not come through.
 *
 * A public channel is carried by servers of wildly differing reliability, so one line failing says
 * nothing about the rest. Leaving it to the viewer means asking them to tell a dead server from a
 * slow one, which nothing on screen tells them.
 *
 * Two things count as not coming through. A line may fail outright, which the screen hands over as
 * soon as it hears; or it may accept the connection and then send nothing, which shows only as a
 * picture that never arrives and so has to be waited out for [patience]. Both are met the same way,
 * as either leaves the viewer looking at nothing.
 */
@UnstableApi
@Stable
class LiveLinesState(
    private val player: Player,
    private val lines: List<String>,
    private val patience: Duration = 15.seconds,
) {
    /** Which line is in use, counting from one, so that it can be said out loud. */
    var lineInUse: Int by mutableIntStateOf(1)
        private set

    /** Whether a line has been given up on and the next one has yet to arrive. */
    var isSwitching: Boolean by mutableStateOf(false)
        private set

    /** Whether every line has now been tried, leaving nothing further to fall back on. */
    var hasGivenUp: Boolean by mutableStateOf(false)
        private set

    val lineCount: Int get() = lines.size

    val hasAnotherLine: Boolean get() = lineInUse < lines.size

    /**
     * Points the player at the next line, keeping the item's identity so that a channel stays one
     * entry in the viewing history however many of its lines had to be tried.
     */
    fun switchToNextLine(): Boolean {
        if (!hasAnotherLine) return false
        val playing = player.currentMediaItem ?: return false
        lineInUse++
        isSwitching = true
        player.replaceMediaItem(
            player.currentMediaItemIndex,
            playing.buildUpon().setUri(lines[lineInUse - 1]).build(),
        )
        player.prepare()
        player.play()
        return true
    }

    suspend fun observe() {
        if (lines.size < 2) return
        lineAwaitingPicture()
            .distinctUntilChanged()
            .collectLatest { line ->
                if (player.isPlaying) {
                    isSwitching = false
                    hasGivenUp = false
                }
                line ?: return@collectLatest
                delay(patience)
                if (!switchToNextLine()) hasGivenUp = true
            }
    }

    /**
     * The line a picture is being waited for on, or null while one is arriving or playback is
     * stopped. Naming the line makes moving to the next one a change of its own, so that the next
     * line is given the same patience afresh rather than inheriting what is left of the last wait.
     */
    private fun lineAwaitingPicture(): Flow<Int?> = channelFlow {
        fun report() {
            trySend(lineInUse.takeIf { player.playWhenReady && !player.isPlaying })
        }

        report()
        player.listen { events ->
            if (events.containsAny(
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAYER_ERROR,
                )
            ) {
                report()
            }
        }
        awaitClose()
    }
}
