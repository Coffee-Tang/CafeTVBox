package dev.anilbeesetti.nextplayer.core.data.catalogue

import dev.anilbeesetti.nextplayer.core.model.MediaName
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bodies answered here are what TMDB really replied with, cut down to the fields a match is made
 * out of and left holding the ones it is not, so that ignoring the rest of an answer is tested too.
 */
class TmdbCatalogueTest {

    @Test
    fun `an episode is asked about at the host that answers in China, in its catalogue of series`() = runTest {
        val fetch = Recorded { siliconValleyAnswer }

        TmdbCatalogue(TmdbApiKey { "a-key" }, fetch).search(siliconValley).getOrThrow()

        assertTrue(fetch.urls.all { it.startsWith("https://api.tmdb.org/3/search/tv?") })
        assertTrue(fetch.urls.none { it.contains("themoviedb") })
        assertTrue(fetch.urls.all { it.contains("api_key=a-key") && it.contains("language=zh-CN") })
    }

    @Test
    fun `a film is asked about in the catalogue of films instead`() = runTest {
        val fetch = Recorded { inceptionAnswer }

        TmdbCatalogue(TmdbApiKey { "a-key" }, fetch).search(inception).getOrThrow()

        assertEquals(1, fetch.urls.size)
        assertTrue(fetch.urls.single().startsWith("https://api.tmdb.org/3/search/movie?"))
    }

    @Test
    fun `both of the titles a name carries are asked about, and a work both found is offered once`() = runTest {
        val fetch = Recorded { siliconValleyAnswer }

        val matches = TmdbCatalogue(TmdbApiKey { "a-key" }, fetch).search(siliconValley).getOrThrow()

        assertEquals(2, fetch.urls.size)
        assertTrue(fetch.urls[0].contains("query=Silicon+Valley"))
        assertTrue(fetch.urls[1].contains("query=%E7%A1%85%E8%B0%B7"))
        assertEquals(listOf(60573, 78094), matches.map { it.work.id })
    }

    @Test
    fun `the series a real answer holds is the one picked out of it`() = runTest {
        val matches = TmdbCatalogue(TmdbApiKey { "a-key" }, Recorded { siliconValleyAnswer })
            .search(siliconValley)
            .getOrThrow()

        val best = matches.first()
        assertEquals(60573, best.work.id)
        assertEquals("硅谷", best.work.title)
        assertEquals("Silicon Valley", best.work.originalTitle)
        assertEquals(2014, best.work.year)
        assertEquals("/aA5qXvsBHOtmDrCTFsxpAJXsvOL.jpg", best.work.posterPath)
        assertTrue(best.certain)
    }

    @Test
    fun `a work the catalogue dates with nothing is offered with no year rather than refused`() = runTest {
        val matches = TmdbCatalogue(TmdbApiKey { "a-key" }, Recorded { tiandaoAnswer })
            .search(MediaName.Episode("天道", null, season = 1, episode = 1, seasonYear = null))
            .getOrThrow()

        assertEquals(listOf(62032, 243777, 139129), matches.map { it.work.id })
        assertEquals(null, matches[1].work.year)
    }

    @Test
    fun `a search that found nothing answers with nothing rather than failing`() = runTest {
        val matches = TmdbCatalogue(TmdbApiKey { "a-key" }, Recorded { """{"page":1,"results":[],"total_results":0}""" })
            .search(inception)
            .getOrThrow()

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `a body that is no answer of the catalogue's is reported as a failure`() = runTest {
        val result = TmdbCatalogue(TmdbApiKey { "a-key" }, Recorded { "<html><head><title>502</title></head></html>" })
            .search(inception)

        assertTrue(result.isFailure)
    }

    @Test
    fun `an answer shaped unlike a page of results is reported as a failure`() = runTest {
        val result = TmdbCatalogue(TmdbApiKey { "a-key" }, Recorded { """{"results":{"id":27205}}""" })
            .search(inception)

        assertTrue(result.isFailure)
    }

    @Test
    fun `a request the host refuses is reported as a failure`() = runTest {
        val result = TmdbCatalogue(TmdbApiKey { "a-key" }, Recorded { throw IOException("Request failed: HTTP 401") })
            .search(inception)

        assertTrue(result.isFailure)
    }

    @Test
    fun `a build given no key asks the catalogue nothing`() = runTest {
        val fetch = Recorded { error("Nothing should be asked without a key") }

        val result = TmdbCatalogue(TmdbApiKey { null }, fetch).search(inception)

        assertTrue(result.isFailure)
        assertTrue(fetch.urls.isEmpty())
    }

    private val siliconValley = MediaName.Episode(
        title = "Silicon Valley",
        otherTitle = "硅谷",
        season = 3,
        episode = 1,
        seasonYear = 2016,
    )

    private val inception = MediaName.Film(title = "Inception", otherTitle = null, year = 2010)
}

/** A fetch that answers as a test tells it to and keeps what it was asked for. */
private class Recorded(private val answer: (String) -> String) : (String) -> String {

    val urls = mutableListOf<String>()

    override fun invoke(url: String): String {
        urls += url
        return answer(url)
    }
}

private val siliconValleyAnswer = """
    {
      "page": 1,
      "results": [
        {
          "adult": false,
          "id": 60573,
          "name": "硅谷",
          "original_name": "Silicon Valley",
          "first_air_date": "2014-04-06",
          "origin_country": ["US"],
          "popularity": 28.2218,
          "poster_path": "/aA5qXvsBHOtmDrCTFsxpAJXsvOL.jpg",
          "overview": "六个在硅谷的程序员创业的故事。",
          "vote_average": 8.1
        },
        {
          "id": 78094,
          "name": "Silicon Valley: The Untold Story",
          "original_name": "Silicon Valley: The Untold Story",
          "first_air_date": "2018-03-19",
          "popularity": 1.6144,
          "poster_path": null,
          "overview": ""
        }
      ],
      "total_pages": 1,
      "total_results": 2
    }
""".trimIndent()

private val tiandaoAnswer = """
    {
      "page": 1,
      "results": [
        {
          "id": 62032,
          "name": "天道",
          "original_name": "天道",
          "first_air_date": "2008-06-01",
          "popularity": 5.8804,
          "poster_path": "/tiandao.jpg",
          "overview": ""
        },
        {
          "id": 139129,
          "name": "大道通天",
          "original_name": "大道通天",
          "first_air_date": "2015-01-15",
          "popularity": 1.5213,
          "poster_path": null,
          "overview": ""
        },
        {
          "id": 243777,
          "name": "天道",
          "original_name": "天道",
          "first_air_date": "",
          "popularity": 0.3121,
          "poster_path": null,
          "overview": ""
        }
      ],
      "total_pages": 3,
      "total_results": 58
    }
""".trimIndent()

private val inceptionAnswer = """
    {
      "page": 1,
      "results": [
        {
          "id": 27205,
          "title": "盗梦空间",
          "original_title": "Inception",
          "release_date": "2010-07-15",
          "popularity": 34.1234,
          "poster_path": "/inception.jpg",
          "overview": ""
        }
      ],
      "total_pages": 1,
      "total_results": 1
    }
""".trimIndent()
