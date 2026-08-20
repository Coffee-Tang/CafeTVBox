package dev.anilbeesetti.nextplayer.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogueBindTest {

    @Test
    fun `one certain match among others is bound`() {
        val right = work(id = 60573, title = "硅谷")
        val matches = listOf(
            WorkMatch(right, score = 1.0, titleSimilarity = 1.0),
            WorkMatch(work(id = 1, title = "Start-Ups: Silicon Valley"), score = 0.6, titleSimilarity = 0.6),
        )

        assertEquals(right, autoBind(matches))
    }

    @Test
    fun `two certain matches are left unbound`() {
        val matches = listOf(
            WorkMatch(work(id = 62032, title = "天道"), score = 1.0, titleSimilarity = 1.0),
            WorkMatch(work(id = 243777, title = "天道"), score = 1.0, titleSimilarity = 1.0),
        )

        assertNull(autoBind(matches))
    }

    @Test
    fun `nothing certain is left unbound`() {
        val matches = listOf(
            WorkMatch(work(id = 1, title = "大道通天"), score = 0.4, titleSimilarity = 0.4),
        )

        assertNull(autoBind(matches))
    }

    @Test
    fun `a tmdb poster path is turned into an image address`() {
        assertEquals(
            "https://image.tmdb.org/t/p/w500/abc.jpg",
            tmdbImageUrl("/abc.jpg"),
        )
        assertEquals(
            "https://cdn.example/poster.jpg",
            tmdbImageUrl("https://cdn.example/poster.jpg"),
        )
        assertNull(tmdbImageUrl(null))
    }

    private fun work(id: Int, title: String) = CatalogueWork(
        kind = WorkKind.SERIES,
        id = id,
        title = title,
        originalTitle = title,
        year = 2008,
        popularity = 1.0,
        posterPath = null,
        overview = "",
    )
}
