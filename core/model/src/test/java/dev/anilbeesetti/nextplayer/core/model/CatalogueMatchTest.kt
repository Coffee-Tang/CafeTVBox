package dev.anilbeesetti.nextplayer.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The works named here are the ones TMDB really answered these searches with, so that the ordering
 * being asserted is the ordering the app will have to get right.
 */
class CatalogueMatchTest {

    @Test
    fun `a name is looked for among films or among series, never both`() {
        assertEquals(WorkKind.FILM, MediaName.Film("Inception", null, 2010).kind)
        assertEquals(WorkKind.SERIES, MediaName.Episode("硅谷", null, 3, 1, 2016).kind)
    }

    @Test
    fun `the series a release name carries both titles of is picked out of what else is titled like it`() {
        val matches = matchesFor(siliconValleyEpisode(season = 3, seasonYear = 2016), siliconValleySearch)

        assertEquals(60573, matches.first().work.id)
        assertTrue(matches.first().certain)
        assertFalse(matches.drop(1).any { it.certain })
    }

    @Test
    fun `the Chinese title of a work the catalogue answers in Chinese finds it just as well`() {
        val matches = matchesFor(
            MediaName.Film("盗梦空间", null, 2010),
            listOf(film(27205, title = "盗梦空间", originalTitle = "Inception", year = 2010, popularity = 34.0)),
        )

        assertEquals(27205, matches.first().work.id)
        assertTrue(matches.first().certain)
    }

    @Test
    fun `the year on an episode dates its season and is not held against the series' own year`() {
        val impostor = series(998, "Silicon Valley", year = 2019, popularity = 3.0)

        val matches = matchesFor(
            siliconValleyEpisode(season = 6, seasonYear = 2019),
            siliconValleySearch + impostor,
        )

        assertEquals(60573, matches.first().work.id)
    }

    @Test
    fun `a series the catalogue dates after the season was broadcast loses to one dated before it`() {
        val impostor = series(999, "Silicon Valley", year = 2021, popularity = 50.0)

        val matches = matchesFor(
            siliconValleyEpisode(season = 6, seasonYear = 2019),
            listOf(impostor) + siliconValleySearch,
        )

        assertEquals(60573, matches.first().work.id)
        assertEquals(999, matches[1].work.id)
    }

    @Test
    fun `two series titled the same are separated by which of them is being watched`() {
        val matches = matchesFor(MediaName.Episode("天道", null, 1, 1, null), tiandaoSearch)

        assertEquals(listOf(62032, 243777), matches.filter { it.certain }.map { it.work.id })
        assertEquals(62032, matches.first().work.id)
    }

    @Test
    fun `every work that merely contains the title is offered, and none of them near the one that is it`() {
        val matches = matchesFor(siliconValleyEpisode(season = 3, seasonYear = 2016), siliconValleySearch)

        assertEquals(siliconValleySearch.size, matches.size)
        assertEquals(60573, matches.first().work.id)
        assertTrue(matches.drop(1).all { it.titleSimilarity < 0.7 })
    }

    @Test
    fun `the year a film gives picks between two works titled the same`() {
        val matches = matchesFor(
            MediaName.Film("Inception", null, 2010),
            listOf(
                film(1, title = "Inception", year = 1998, popularity = 100.0),
                film(2, title = "Inception", year = 2010, popularity = 1.0),
            ),
        )

        assertEquals(listOf(2, 1), matches.map { it.work.id })
    }

    @Test
    fun `a film's year going against a title that matches outright does not overturn it`() {
        val matches = matchesFor(
            MediaName.Film("Inception", null, 2010),
            listOf(
                film(1, title = "Inception", year = 1998, popularity = 0.5),
                film(2, title = "Inception: The Cobol Job", year = 2010, popularity = 90.0),
            ),
        )

        assertEquals(listOf(1, 2), matches.map { it.work.id })
        assertTrue(matches.first().certain)
    }

    @Test
    fun `an episode is not answered with a film of the same title, nor a film with a series`() {
        val film = film(1, title = "Silicon Valley", year = 2016, popularity = 90.0)
        val series = series(2, "Inception", year = 2010, popularity = 90.0)

        assertEquals(emptyList<Int>(), matchesFor(siliconValleyEpisode(3, 2016), listOf(film)).map { it.work.id })
        assertEquals(emptyList<Int>(), matchesFor(MediaName.Film("Inception", null, 2010), listOf(series)).map { it.work.id })
    }

    @Test
    fun `a search that turned nothing up matches nothing`() {
        assertEquals(emptyList<WorkMatch>(), matchesFor(siliconValleyEpisode(3, 2016), emptyList()))
    }

    @Test
    fun `a work the search only half agrees with is offered without being called certain`() {
        val matches = matchesFor(
            siliconValleyEpisode(season = 3, seasonYear = 2016),
            listOf(series(78094, "Silicon Valley: The Untold Story", year = 2018)),
        )

        assertEquals(78094, matches.single().work.id)
        assertFalse(matches.single().certain)
        assertTrue(matches.single().titleSimilarity > 0.4)
        assertTrue(matches.single().titleSimilarity < 0.6)
    }

    @Test
    fun `a work with nothing of the title in it is ranked last rather than called an answer`() {
        val matches = matchesFor(MediaName.Episode("天道", null, 1, 1, null), tiandaoSearch)

        assertEquals(139129, matches.last().work.id)
        assertTrue(matches.last().titleSimilarity < 0.5)
    }

    private fun siliconValleyEpisode(season: Int, seasonYear: Int) =
        MediaName.Episode("Silicon Valley", "硅谷", season = season, episode = 1, seasonYear = seasonYear)

    /** What `search/tv?language=zh-CN&query=Silicon Valley` answered, less an unrepeatable title. */
    private val siliconValleySearch = listOf(
        series(60573, title = "硅谷", originalTitle = "Silicon Valley", year = 2014, popularity = 28.2218),
        series(78094, title = "Silicon Valley: The Untold Story", year = 2018, popularity = 1.6144),
        series(78807, title = "Secrets of Silicon Valley", year = 2017, popularity = 0.3609),
        series(58540, title = "Start-Ups: Silicon Valley", year = null, popularity = 0.257),
    )

    /** What `search/tv?language=zh-CN&query=天道` answered, in the order it answered. */
    private val tiandaoSearch = listOf(
        series(62032, title = "天道", year = 2008, popularity = 5.8804),
        series(139129, title = "大道通天", year = 2015, popularity = 1.5213),
        series(243777, title = "天道", year = null, popularity = 0.3121),
    )

    private fun series(
        id: Int,
        title: String,
        originalTitle: String = title,
        year: Int? = null,
        popularity: Double = 1.0,
    ) = work(WorkKind.SERIES, id, title, originalTitle, year, popularity)

    private fun film(
        id: Int,
        title: String,
        originalTitle: String = title,
        year: Int? = null,
        popularity: Double = 1.0,
    ) = work(WorkKind.FILM, id, title, originalTitle, year, popularity)

    private fun work(
        kind: WorkKind,
        id: Int,
        title: String,
        originalTitle: String,
        year: Int?,
        popularity: Double,
    ) = CatalogueWork(
        kind = kind,
        id = id,
        title = title,
        originalTitle = originalTitle,
        year = year,
        popularity = popularity,
        posterPath = null,
        overview = "",
    )
}
