package dev.anilbeesetti.nextplayer.feature.player.service

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressTickerTest {

    @Test
    fun `nothing is written before playback starts`() = runTest {
        var saves = 0
        ProgressTicker(this, 10.seconds) { saves++ }

        advanceTimeBy(60.seconds)

        assertEquals(0, saves)
    }

    @Test
    fun `an hour watched straight through is written every interval`() = runTest {
        var saves = 0
        val ticker = ProgressTicker(this, 10.seconds) { saves++ }

        ticker.playing(true)
        advanceTimeBy(35.seconds)

        assertEquals(3, saves)
        ticker.playing(false)
    }

    @Test
    fun `the first write waits an interval, since starting playback wrote the position itself`() = runTest {
        var saves = 0
        val ticker = ProgressTicker(this, 10.seconds) { saves++ }

        ticker.playing(true)
        advanceTimeBy(9.seconds)

        assertEquals(0, saves)
        ticker.playing(false)
    }

    @Test
    fun `pausing stops the writing`() = runTest {
        var saves = 0
        val ticker = ProgressTicker(this, 10.seconds) { saves++ }

        ticker.playing(true)
        advanceTimeBy(25.seconds)
        ticker.playing(false)
        advanceTimeBy(60.seconds)

        assertEquals(2, saves)
    }

    @Test
    fun `playback reported again does not write twice as often`() = runTest {
        var saves = 0
        val ticker = ProgressTicker(this, 10.seconds) { saves++ }

        ticker.playing(true)
        ticker.playing(true)
        advanceTimeBy(25.seconds)

        assertEquals(2, saves)
        ticker.playing(false)
    }

    @Test
    fun `a pause reported twice does not restart the interval on the second`() = runTest {
        var saves = 0
        val ticker = ProgressTicker(this, 10.seconds) { saves++ }

        ticker.playing(false)
        ticker.playing(true)
        advanceTimeBy(15.seconds)
        ticker.playing(false)
        ticker.playing(false)
        advanceTimeBy(15.seconds)

        assertEquals(1, saves)
    }

    @Test
    fun `carrying on after a pause writes again`() = runTest {
        var saves = 0
        val ticker = ProgressTicker(this, 10.seconds) { saves++ }

        ticker.playing(true)
        advanceTimeBy(15.seconds)
        ticker.playing(false)
        ticker.playing(true)
        advanceTimeBy(15.seconds)

        assertEquals(2, saves)
        ticker.playing(false)
    }
}
