package dev.anilbeesetti.nextplayer.core.data.history

import dev.anilbeesetti.nextplayer.core.database.entities.MediumStateEntity
import dev.anilbeesetti.nextplayer.core.database.entities.RecentPlayback
import dev.anilbeesetti.nextplayer.core.model.RecentMedium
import dev.anilbeesetti.nextplayer.core.model.WorkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentHistoryTest {

    @Test
    fun `a series watched over several evenings is one entry, at the latest episode`() {
        val history = listOf(
            played(uri = "share://silicon/s02e05", at = 300, work = 1, season = 2, episode = 5),
            played(uri = "share://silicon/s02e04", at = 200, work = 1, season = 2, episode = 4),
            played(uri = "share://silicon/s01e01", at = 100, work = 1, season = 1, episode = 1),
        ).foldByWork(limit = 10)

        assertEquals(1, history.size)
        assertEquals("share://silicon/s02e05", history.single().mediaKey)
        assertEquals(2, history.single().season)
        assertEquals(5, history.single().episode)
    }

    @Test
    fun `works keep the order they were last watched in`() {
        val history = listOf(
            played(uri = "share://b/e1", at = 400, work = 2, season = 1, episode = 1),
            played(uri = "share://a/e9", at = 300, work = 1, season = 1, episode = 9),
            played(uri = "share://a/e8", at = 200, work = 1, season = 1, episode = 8),
        ).foldByWork(limit = 10)

        assertEquals(listOf("share://b/e1", "share://a/e9"), history.map { it.mediaKey })
    }

    @Test
    fun `what no library catalogued stands on its own, however much of it there is`() {
        val history = listOf(
            played(uri = "cafeplayer-live://cctv1", at = 300, work = null),
            played(uri = "cafeplayer-live://cctv2", at = 200, work = null),
            played(uri = "content://videos/42", at = 100, work = null),
        ).foldByWork(limit = 10)

        assertEquals(3, history.size)
    }

    @Test
    fun `the limit counts entries, not the episodes folded into them`() {
        val rows = listOf(
            played(uri = "share://a/e3", at = 500, work = 1, season = 1, episode = 3),
            played(uri = "share://a/e2", at = 400, work = 1, season = 1, episode = 2),
            played(uri = "share://a/e1", at = 300, work = 1, season = 1, episode = 1),
            played(uri = "share://b/e1", at = 200, work = 2, season = 1, episode = 1),
            played(uri = "share://c/e1", at = 100, work = 3, season = 1, episode = 1),
        )

        assertEquals(
            listOf("share://a/e3", "share://b/e1"),
            rows.foldByWork(limit = 2).map { it.mediaKey },
        )
    }

    @Test
    fun `an entry left by an earlier version is renamed after the work it turned out to be`() {
        val history = listOf(
            played(
                uri = "share://silicon/s02e07",
                at = 100,
                work = 1,
                season = 2,
                episode = 7,
                fileTitle = "硅谷.Silicon.Valley.2015.S02E07.1080p.WEB-DL.mkv",
                workTitle = "硅谷",
            ),
        ).foldByWork(limit = 10)

        assertEquals("硅谷", history.single().title)
    }

    @Test
    fun `a film is not numbered as an episode`() {
        val history = listOf(
            played(uri = "share://dune", at = 100, work = 1, season = 1, episode = 1, kind = WorkKind.FILM),
        ).foldByWork(limit = 10)

        assertNull(history.single().season)
        assertNull(history.single().episode)
    }

    @Test
    fun `where an episode was left comes through, so it can be carried on from history`() {
        val history = listOf(
            played(uri = "share://a/e1", at = 100, work = 1, season = 1, episode = 1, position = 933_872),
        ).foldByWork(limit = 10)

        assertEquals(933_872L, history.single().positionMs)
        assertEquals(RecentMedium.Source.STREAM, history.single().source)
    }

    @Test
    fun `the work an entry belongs to comes through, so resuming can still reach its other episodes`() {
        val history = listOf(
            played(uri = "share://silicon/s02e05", at = 200, work = 7, season = 2, episode = 5),
            played(uri = "cafeplayer-live://cctv1", at = 100, work = null),
        ).foldByWork(limit = 10)

        assertEquals(7L, history.first().workId)
        assertNull(history.last().workId)
    }

    private fun played(
        uri: String,
        at: Long,
        work: Long?,
        season: Int? = null,
        episode: Int? = null,
        position: Long = 0,
        fileTitle: String = "Something",
        workTitle: String = "A work",
        kind: WorkKind = WorkKind.SERIES,
    ) = RecentPlayback(
        state = MediumStateEntity(
            uriString = uri,
            playbackPosition = position,
            lastPlayedTime = at,
            title = fileTitle,
            duration = 1_800_000,
        ),
        workId = work,
        workTitle = work?.let { workTitle },
        workKind = work?.let { kind.name },
        season = season,
        episode = episode,
    )
}
