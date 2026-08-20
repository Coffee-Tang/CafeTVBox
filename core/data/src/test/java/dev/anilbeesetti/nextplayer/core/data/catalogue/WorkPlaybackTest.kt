package dev.anilbeesetti.nextplayer.core.data.catalogue

import dev.anilbeesetti.nextplayer.core.database.entities.ItemPlayback
import dev.anilbeesetti.nextplayer.core.model.episodeToResume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkPlaybackTest {

    @Test
    fun `episodes come back in order, whatever order their files are in`() {
        val episodes = listOf(
            file(item = 3, season = 2, episode = 1),
            file(item = 1, season = 1, episode = 1),
            file(item = 2, season = 1, episode = 2),
        ).toEpisodes()

        assertEquals(listOf(1L, 2L, 3L), episodes.map { it.id })
    }

    @Test
    fun `an episode never played opens on a file it has, at the beginning`() {
        val episode = listOf(file(item = 1, season = 1, episode = 1, uri = "share://a/e1")).toEpisodes().single()

        assertEquals("share://a/e1", episode.mediaKey)
        assertEquals(0L, episode.playbackPosition)
        assertNull(episode.lastPlayedTime)
    }

    @Test
    fun `an episode held in two copies opens on the copy last played, at its position`() {
        val episode = listOf(
            file(item = 1, season = 1, episode = 1, uri = "share://a/e1.1080p", at = null, position = null),
            file(item = 1, season = 1, episode = 1, uri = "share://a/e1.720p", at = 500, position = 12_000),
        ).toEpisodes().single()

        assertEquals("share://a/e1.720p", episode.mediaKey)
        assertEquals(12_000L, episode.playbackPosition)
        assertEquals(500L, episode.lastPlayedTime)
    }

    @Test
    fun `an episode with no files yet has nothing to open`() {
        val episode = listOf(file(item = 1, season = 1, episode = 1, uri = null)).toEpisodes().single()

        assertNull(episode.mediaKey)
    }

    @Test
    fun `a work carries on from the episode watched last, not the one after it`() {
        val episodes = listOf(
            file(item = 1, season = 1, episode = 1, uri = "share://a/e1", at = 100, position = 1_000),
            file(item = 2, season = 1, episode = 2, uri = "share://a/e2"),
            file(item = 3, season = 1, episode = 3, uri = "share://a/e3", at = 300, position = 2_000),
            file(item = 4, season = 1, episode = 4, uri = "share://a/e4"),
        ).toEpisodes()

        assertEquals("share://a/e3", episodes.episodeToResume()?.mediaKey)
    }

    @Test
    fun `a work never watched starts at its first episode`() {
        val episodes = listOf(
            file(item = 2, season = 1, episode = 2, uri = "share://a/e2"),
            file(item = 1, season = 1, episode = 1, uri = "share://a/e1"),
        ).toEpisodes()

        assertEquals("share://a/e1", episodes.episodeToResume()?.mediaKey)
    }

    private fun file(
        item: Long,
        season: Int,
        episode: Int,
        uri: String? = "share://work/$season-$episode",
        at: Long? = null,
        position: Long? = null,
    ) = ItemPlayback(
        itemId = item,
        season = season,
        episode = episode,
        uri = uri,
        playbackPosition = position,
        lastPlayedTime = at,
    )
}
