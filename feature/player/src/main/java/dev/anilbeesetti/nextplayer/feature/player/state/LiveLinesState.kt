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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@UnstableApi
@Composable
fun rememberLiveLinesState(
    player: Player,
    lines: List<String>,
    onLinePlaying: (String) -> Unit = {},
): LiveLinesState {
    val liveLinesState = remember(player, lines) { LiveLinesState(player, lines, onLinePlaying = onLinePlaying) }
    LaunchedEffect(liveLinesState) { liveLinesState.observe() }
    return liveLinesState
}

/**
 * The lines to try for a channel, starting with the one that last played.
 *
 * Which server comes through says more about the viewer's network than about the channel, so the
 * one that worked for them is worth returning to before the rest. A line the playlists no longer
 * carry is passed over in silence, as the viewer never chose it by address.
 */
fun linesStartingWith(lines: List<String>, lastPlayed: String?): List<String> = when {
    lastPlayed == null || lastPlayed !in lines -> lines
    else -> listOf(lastPlayed) + lines.filterNot { it == lastPlayed }
}

/**
 * Which line a channel is on, and whether it was put there or got there by itself.
 *
 * Falling through the lines is only worth reporting as a channel being unreachable when the player
 * walked them itself. A viewer who picked the last line has not asked about the ones before it.
 */
internal data class LineInUse(val number: Int = 1, val wasChosenByHand: Boolean = false) {

    /** The next line to fall back on, or null when the channel offers no more. */
    fun next(lineCount: Int): LineInUse? = LineInUse(number + 1, wasChosenByHand)
        .takeIf { it.number <= lineCount }

    /**
     * The line asked for by hand, or null when the channel does not offer it.
     *
     * Asking for the line already in use still counts as asking: the player may have moved there
     * by itself a moment before the viewer chose it, and their choice should not be lost to that.
     */
    fun chosen(number: Int, lineCount: Int): LineInUse? = LineInUse(number, wasChosenByHand = true)
        .takeIf { number in 1..lineCount }

    /** Whether running out of lines from here means the channel could not be reached at all. */
    val mayReportEveryLineUnreachable: Boolean get() = !wasChosenByHand
}

/**
 * How a line is faring, named so that moving to another line is a change in its own right.
 *
 * Whether a picture is arriving is not enough to go on: a line can be swapped for another without
 * playback ever stopping, and a report that left the line out would then read the same before and
 * after, hiding the move from anything watching for changes.
 */
internal sealed interface LineReport {

    /** The line is connected and a picture is arriving. */
    data class Playing(val line: Int) : LineReport

    /** The line should be showing something and is not, so it is being given time to. */
    data class AwaitingPicture(val line: Int) : LineReport

    /** Nothing is expected of any line, playback having been stopped or paused. */
    data object Stopped : LineReport

    companion object {
        fun of(line: Int, isPlaying: Boolean, playWhenReady: Boolean): LineReport = when {
            isPlaying -> Playing(line)
            playWhenReady -> AwaitingPicture(line)
            else -> Stopped
        }
    }
}

/**
 * Moves a broadcast onto another line its channel offers, by itself when the one in use does not
 * come through, or on request.
 *
 * A public channel is carried by servers of wildly differing reliability, so one line failing says
 * nothing about the rest. Leaving it to the viewer means asking them to tell a dead server from a
 * slow one, which nothing on screen tells them.
 *
 * Two things count as not coming through. A line may fail outright, which the screen hands over as
 * soon as it hears; or it may accept the connection and then send nothing, which shows only as a
 * picture that never arrives and so has to be waited out for [patience]. Both are met the same way,
 * as either leaves the viewer looking at nothing.
 *
 * [onLinePlaying] is told which line is coming through once it actually is, which is later than it
 * being selected and later than it connecting.
 */
@UnstableApi
@Stable
class LiveLinesState(
    private val player: Player,
    private val lines: List<String>,
    private val patience: Duration = 15.seconds,
    private val onLinePlaying: (String) -> Unit = {},
) {
    private var lineInUseState: LineInUse by mutableStateOf(LineInUse())

    /** Which line is in use, counting from one, so that it can be said out loud. */
    val lineInUse: Int get() = lineInUseState.number

    /** Whether a line has been given up on and the next one has yet to arrive. */
    var isSwitching: Boolean by mutableStateOf(false)
        private set

    /** Whether every line has now been tried, leaving nothing further to fall back on. */
    var hasGivenUp: Boolean by mutableStateOf(false)
        private set

    val lineCount: Int get() = lines.size

    val hasAnotherLine: Boolean get() = lineInUse < lines.size

    /** Falls back to the line after the one in use, when there is one. */
    fun switchToNextLine(): Boolean = switchTo(lineInUseState.next(lines.size))

    /** Moves to the line the viewer asked for, counting from one as [lineInUse] does. */
    fun switchToLine(number: Int): Boolean {
        val line = lineInUseState.chosen(number, lines.size) ?: return false
        // The line asked for is already in use, so there is nothing to interrupt in order to honour
        // the choice; recording it is enough to stop the player deciding otherwise.
        if (line.number == lineInUse) {
            lineInUseState = line
            return true
        }
        return switchTo(line)
    }

    /**
     * Points the player at another line, keeping the item's identity so that a channel stays one
     * entry in the viewing history however many of its lines were tried.
     */
    private fun switchTo(line: LineInUse?): Boolean {
        line ?: return false
        val playing = player.currentMediaItem ?: return false
        lineInUseState = line
        isSwitching = true
        hasGivenUp = false
        player.replaceMediaItem(
            player.currentMediaItemIndex,
            playing.buildUpon().setUri(lines[line.number - 1]).build(),
        )
        player.prepare()
        player.play()
        return true
    }

    suspend fun observe() {
        if (lines.size < 2) return
        lineReports()
            .distinctUntilChanged()
            .collectLatest { report ->
                when (report) {
                    is LineReport.Playing -> {
                        isSwitching = false
                        hasGivenUp = false
                        onLinePlaying(lines[report.line - 1])
                    }
                    is LineReport.AwaitingPicture -> {
                        delay(patience)
                        if (!switchToNextLine() && lineInUseState.mayReportEveryLineUnreachable) {
                            hasGivenUp = true
                        }
                    }
                    LineReport.Stopped -> Unit
                }
            }
    }

    /**
     * How the line in use is faring, reported afresh whenever the player has news.
     *
     * The line a report names is what makes each line's wait its own: moving on gives the next line
     * the full patience rather than what is left of the last one's, and a line chosen by hand is not
     * switched away from on the strength of an earlier line having been waited for.
     */
    private fun lineReports(): Flow<LineReport> = channelFlow {
        fun report() {
            trySend(
                LineReport.of(
                    line = lineInUse,
                    isPlaying = player.isPlaying,
                    playWhenReady = player.playWhenReady,
                ),
            )
        }

        report()
        player.listen { events ->
            if (events.containsAny(
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAYER_ERROR,
                    // A line can be swapped for another without playback pausing for it.
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                )
            ) {
                report()
            }
        }
        awaitClose()
    }
}
