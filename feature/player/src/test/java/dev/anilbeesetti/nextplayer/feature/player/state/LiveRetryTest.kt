package dev.anilbeesetti.nextplayer.feature.player.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRetryTest {

    @Test
    fun `a broadcast that stops is asked again before its line is held to blame`() {
        assertEquals(
            PlaybackFailureResponse.RETRY_AT_LIVE_EDGE,
            responseToPlaybackFailure(isLive = true, mayRetryLine = true, hasAnotherLine = true),
        )
    }

    @Test
    fun `a line is only given up on once asking it again has stopped helping`() {
        assertEquals(
            PlaybackFailureResponse.SWITCH_LINE,
            responseToPlaybackFailure(isLive = true, mayRetryLine = false, hasAnotherLine = true),
        )
    }

    @Test
    fun `the viewer is told only once every line has been tried and asking again is spent`() {
        assertEquals(
            PlaybackFailureResponse.GIVE_UP,
            responseToPlaybackFailure(isLive = true, mayRetryLine = false, hasAnotherLine = false),
        )
    }

    @Test
    fun `the one line a channel offers is still worth asking again`() {
        assertEquals(
            PlaybackFailureResponse.RETRY_AT_LIVE_EDGE,
            responseToPlaybackFailure(isLive = true, mayRetryLine = true, hasAnotherLine = false),
        )
    }

    @Test
    fun `a file that could not be read is not worth reading again`() {
        assertEquals(
            PlaybackFailureResponse.GIVE_UP,
            responseToPlaybackFailure(isLive = false, mayRetryLine = true, hasAnotherLine = false),
        )
    }

    @Test
    fun `a line that never played long enough to be called live is still fallen back from`() {
        assertEquals(
            PlaybackFailureResponse.SWITCH_LINE,
            responseToPlaybackFailure(isLive = false, mayRetryLine = true, hasAnotherLine = true),
        )
    }

    @Test
    fun `a line arrives with every retry it is allowed`() {
        assertTrue(RetryAllowance(perLine = 2).hasRetryLeft)
    }

    @Test
    fun `a line runs out of retries after it has spent them all`() {
        val allowance = RetryAllowance(perLine = 2).spend().spend()

        assertFalse(allowance.hasRetryLeft)
    }

    @Test
    fun `a line still has a retry left with one of two spent`() {
        assertTrue(RetryAllowance(perLine = 2).spend().hasRetryLeft)
    }

    @Test
    fun `spending past the allowance cannot put a line further into debt`() {
        assertEquals(
            RetryAllowance(perLine = 1, spent = 1),
            RetryAllowance(perLine = 1).spend().spend().spend(),
        )
    }

    @Test
    fun `a line that has settled into playing is worth its full allowance again`() {
        val allowance = RetryAllowance(perLine = 2).spend().spend().renewed()

        assertTrue(allowance.hasRetryLeft)
        assertEquals(RetryAllowance(perLine = 2), allowance)
    }

    @Test
    fun `a channel offering no retries at all is fallen back from at once`() {
        assertFalse(RetryAllowance(perLine = 0).hasRetryLeft)
        assertEquals(
            PlaybackFailureResponse.SWITCH_LINE,
            responseToPlaybackFailure(
                isLive = true,
                mayRetryLine = RetryAllowance(perLine = 0).hasRetryLeft,
                hasAnotherLine = true,
            ),
        )
    }
}
