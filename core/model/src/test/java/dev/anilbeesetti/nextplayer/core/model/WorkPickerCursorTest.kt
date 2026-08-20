package dev.anilbeesetti.nextplayer.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkPickerCursorTest {

    @Test
    fun `down opens the picker only when controls are hidden and the title has a work`() {
        assertTrue(shouldOpenWorkPickerOnDown(controlsVisible = false, hasWork = true))
        assertFalse(shouldOpenWorkPickerOnDown(controlsVisible = true, hasWork = true))
        assertFalse(shouldOpenWorkPickerOnDown(controlsVisible = false, hasWork = false))
    }

    @Test
    fun `cursor opens on the episode now playing`() {
        val seasons = siliconSeasons()
        val playing = seasons[1].episodes[1]

        val cursor = workPickerCursorToOpen(
            seasons = seasons,
            currentMediaKey = playing.mediaKey,
            focusedEpisodeId = seasons[0].episodes[0].id,
        )

        assertEquals(WorkPickerBand.EPISODES, cursor?.band)
        assertEquals(1, cursor?.seasonIndex)
        assertEquals(1, cursor?.episodeIndex)
    }

    @Test
    fun `left and right on episodes stay inside the row`() {
        val seasons = siliconSeasons()
        val start = WorkPickerCursor(WorkPickerBand.EPISODES, seasonIndex = 0, episodeIndex = 0)

        val left = moveWorkPickerCursor(start, seasons, WorkPickerDirection.LEFT)
        assertEquals(0, left.episodeIndex)

        val right = moveWorkPickerCursor(start, seasons, WorkPickerDirection.RIGHT)
        assertEquals(1, right.episodeIndex)
    }

    @Test
    fun `up from the first episode row moves to the season tabs`() {
        val seasons = siliconSeasons()
        val start = WorkPickerCursor(WorkPickerBand.EPISODES, seasonIndex = 0, episodeIndex = 2)

        val up = moveWorkPickerCursor(start, seasons, WorkPickerDirection.UP)

        assertEquals(WorkPickerBand.SEASONS, up.band)
        assertEquals(0, up.seasonIndex)
    }

    @Test
    fun `left and right on season tabs change season and land on an episode`() {
        val seasons = siliconSeasons()
        val start = WorkPickerCursor(WorkPickerBand.SEASONS, seasonIndex = 0, episodeIndex = 0)

        val right = moveWorkPickerCursor(start, seasons, WorkPickerDirection.RIGHT)
        assertEquals(WorkPickerBand.SEASONS, right.band)
        assertEquals(1, right.seasonIndex)

        val down = moveWorkPickerCursor(right, seasons, WorkPickerDirection.DOWN)
        assertEquals(WorkPickerBand.EPISODES, down.band)
        assertEquals(1, down.seasonIndex)
    }

    private fun siliconSeasons(): List<LibrarySeason> = workDetailOf(
        work = LibraryWork(
            id = 1,
            libraryId = 1,
            workKey = "siliconvalley",
            kind = WorkKind.SERIES,
            title = "Silicon Valley",
            otherTitle = "硅谷",
            year = null,
            posterUrl = null,
        ),
        episodes = listOf(
            LibraryEpisode(id = 1, season = 2, episode = 1, mediaKey = "s2e1"),
            LibraryEpisode(id = 2, season = 2, episode = 2, mediaKey = "s2e2"),
            LibraryEpisode(id = 3, season = 2, episode = 3, mediaKey = "s2e3"),
            LibraryEpisode(id = 4, season = 3, episode = 1, mediaKey = "s3e1"),
            LibraryEpisode(id = 5, season = 3, episode = 2, mediaKey = "s3e2"),
        ),
    ).seasons
}
