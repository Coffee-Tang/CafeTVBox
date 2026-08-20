package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRemoteFocusTargetTest {

    @Test
    fun `continue watching wins over the wall and empty cta`() {
        assertEquals(
            HomeRemoteFocusTarget.CONTINUE_WATCHING,
            homeRemoteFocusTarget(
                showContinueWatching = true,
                showRecentLive = true,
                hasWorks = true,
                isEmptyLibrary = false,
            ),
        )
    }

    @Test
    fun `recent live wins when there is no continue watching`() {
        assertEquals(
            HomeRemoteFocusTarget.RECENT_LIVE,
            homeRemoteFocusTarget(
                showContinueWatching = false,
                showRecentLive = true,
                hasWorks = true,
                isEmptyLibrary = false,
            ),
        )
    }

    @Test
    fun `first work is the target on a populated wall`() {
        assertEquals(
            HomeRemoteFocusTarget.WORKS,
            homeRemoteFocusTarget(
                showContinueWatching = false,
                showRecentLive = false,
                hasWorks = true,
                isEmptyLibrary = false,
            ),
        )
    }

    @Test
    fun `empty library lands on the network cta`() {
        assertEquals(
            HomeRemoteFocusTarget.EMPTY_CTA,
            homeRemoteFocusTarget(
                showContinueWatching = false,
                showRecentLive = false,
                hasWorks = false,
                isEmptyLibrary = true,
            ),
        )
    }
}
