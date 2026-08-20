package dev.anilbeesetti.nextplayer.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkDetailTest {

    @Test
    fun `episodes of one series become seasons in order`() {
        val detail = workDetailOf(
            work = silicon,
            episodes = listOf(
                episode(id = 3, season = 3, episode = 1),
                episode(id = 1, season = 2, episode = 2),
                episode(id = 2, season = 2, episode = 1),
            ),
        )

        assertEquals(listOf(2, 3), detail.seasons.map { it.season })
        assertEquals(listOf(1, 2), detail.seasons[0].episodes.map { it.episode })
        assertEquals(2L, detail.focusedEpisodeId)
    }

    @Test
    fun `focus lands on the episode last played`() {
        val detail = workDetailOf(
            work = silicon,
            episodes = listOf(
                episode(id = 1, season = 2, episode = 1, lastPlayedTime = 10),
                episode(id = 2, season = 5, episode = 3, lastPlayedTime = 40),
                episode(id = 3, season = 5, episode = 4, lastPlayedTime = 20),
            ),
        )

        assertEquals(2L, detail.focusedEpisodeId)
        assertEquals(5, focusedSeason(detail.seasons, detail.focusedEpisodeId)?.season)
    }

    @Test
    fun `a series with no history focuses the first episode and its season`() {
        val detail = workDetailOf(
            work = silicon,
            episodes = listOf(
                episode(id = 10, season = 3, episode = 1),
                episode(id = 11, season = 2, episode = 8),
            ),
        )

        assertEquals(11L, detail.focusedEpisodeId)
        assertEquals(2, focusedSeason(detail.seasons, detail.focusedEpisodeId)?.season)
    }

    @Test
    fun `a work with no episodes has nothing to focus`() {
        val detail = workDetailOf(work = silicon, episodes = emptyList())

        assertEquals(emptyList<LibrarySeason>(), detail.seasons)
        assertNull(detail.focusedEpisodeId)
        assertNull(focusedSeason(detail.seasons, detail.focusedEpisodeId))
        assertNull(seasonToOpen(detail.seasons, currentMediaKey = null, focusedEpisodeId = null))
        assertNull(episodeToOpen(null, currentMediaKey = null, focusedEpisodeId = null))
    }

    @Test
    fun `the picker opens on the season and episode now playing`() {
        val playing = episode(id = 3, season = 3, episode = 2)
        val seasons = workDetailOf(
            work = silicon,
            episodes = listOf(
                episode(id = 1, season = 2, episode = 1, lastPlayedTime = 99),
                playing,
            ),
        ).seasons

        val season = seasonToOpen(seasons, currentMediaKey = playing.mediaKey, focusedEpisodeId = 1)
        assertEquals(3, season?.season)
        assertEquals(playing, episodeToOpen(season, playing.mediaKey, focusedEpisodeId = 1))
    }

    private fun episode(
        id: Long,
        season: Int,
        episode: Int,
        lastPlayedTime: Long? = null,
    ) = LibraryEpisode(
        id = id,
        season = season,
        episode = episode,
        mediaKey = "item-$id",
        lastPlayedTime = lastPlayedTime,
    )

    private val silicon = LibraryWork(
        id = 1,
        libraryId = 1,
        workKey = "siliconvalley",
        kind = WorkKind.SERIES,
        title = "Silicon Valley",
        otherTitle = "硅谷",
        year = null,
        posterUrl = null,
    )
}
