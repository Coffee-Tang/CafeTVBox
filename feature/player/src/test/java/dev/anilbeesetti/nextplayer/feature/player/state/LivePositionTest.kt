package dev.anilbeesetti.nextplayer.feature.player.state

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LivePositionTest {

    @Test
    fun `a viewer sitting on the live edge is watching the broadcast go out`() {
        assertEquals(
            LivePosition.AtLiveEdge,
            livePositionOf(positionMs = 30_000, liveEdgeMs = 30_000),
        )
    }

    @Test
    fun `a viewer who has rewound is placed by how far behind the edge they are`() {
        assertEquals(
            LivePosition.BehindLiveEdge(behindBy = 15.seconds),
            livePositionOf(positionMs = 45_000, liveEdgeMs = 60_000),
        )
    }

    @Test
    fun `playing on past the edge the window last reported still counts as being on it`() {
        assertEquals(
            LivePosition.AtLiveEdge,
            livePositionOf(positionMs = 38_000, liveEdgeMs = 30_000),
        )
    }

    @Test
    fun `a rewind too small to have been meant is not worth reporting`() {
        assertEquals(
            LivePosition.AtLiveEdge,
            livePositionOf(positionMs = 52_000, liveEdgeMs = 60_000),
        )
    }

    @Test
    fun `a rewind past the drift the window leaves behind it is reported in full`() {
        assertEquals(
            LivePosition.BehindLiveEdge(behindBy = 11.seconds),
            livePositionOf(positionMs = 49_000, liveEdgeMs = 60_000),
        )
    }

    @Test
    fun `an hour back into a channel that keeps one is said to be an hour back`() {
        assertEquals(
            LivePosition.BehindLiveEdge(behindBy = 1.hours),
            livePositionOf(positionMs = 3_600_000, liveEdgeMs = 7_200_000),
        )
    }

    @Test
    fun `a broadcast holding only half a minute is not worth a seek bar`() {
        assertFalse(offersRewind(isLive = true, windowMs = 30_000))
    }

    @Test
    fun `a broadcast whose window length is not known yet is not worth a seek bar`() {
        assertFalse(offersRewind(isLive = true, windowMs = 0))
    }

    @Test
    fun `a broadcast keeping a minute behind it is worth a seek bar`() {
        assertTrue(offersRewind(isLive = true, windowMs = 1.minutes.inWholeMilliseconds))
    }

    @Test
    fun `a channel with hours of rewind keeps its seek bar`() {
        assertTrue(offersRewind(isLive = true, windowMs = 6.hours.inWholeMilliseconds))
    }

    @Test
    fun `a film keeps its seek bar however short it is`() {
        assertTrue(offersRewind(isLive = false, windowMs = 5_000))
    }
}
