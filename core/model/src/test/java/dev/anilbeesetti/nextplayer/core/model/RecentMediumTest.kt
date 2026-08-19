package dev.anilbeesetti.nextplayer.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentMediumTest {

    @Test
    fun `a station named by its channel was watched live`() {
        assertTrue(medium("cafeplayer-live://CCTV1", RecentMedium.Source.STREAM).isLive)
    }

    @Test
    fun `a station named by the line it was on, as older entries are, was watched live`() {
        assertTrue(medium("https://a.example/live/cctv1.m3u8", RecentMedium.Source.STREAM).isLive)
    }

    @Test
    fun `a video reached by address plays from end to end`() {
        val video = medium(
            mediaKey = "https://a.example/holiday.mp4",
            source = RecentMedium.Source.STREAM,
            durationMs = 95_000,
        )

        assertFalse(video.isLive)
    }

    @Test
    fun `files are never live, whatever is known of their length`() {
        assertFalse(medium("file:///sdcard/holiday.mp4", RecentMedium.Source.LOCAL).isLive)
        assertFalse(medium("cafeplayer-network://1/Shows/One.mkv", RecentMedium.Source.SHARE).isLive)
    }

    private fun medium(
        mediaKey: String,
        source: RecentMedium.Source,
        durationMs: Long? = null,
    ) = RecentMedium(
        mediaKey = mediaKey,
        title = "Something",
        source = source,
        positionMs = 0,
        durationMs = durationMs,
        lastPlayedTime = 1,
    )
}
