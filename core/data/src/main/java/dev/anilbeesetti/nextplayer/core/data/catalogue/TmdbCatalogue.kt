package dev.anilbeesetti.nextplayer.core.data.catalogue

import dev.anilbeesetti.nextplayer.core.data.network.httpText
import dev.anilbeesetti.nextplayer.core.model.CatalogueWork
import dev.anilbeesetti.nextplayer.core.model.MediaName
import dev.anilbeesetti.nextplayer.core.model.WorkKind
import dev.anilbeesetti.nextplayer.core.model.WorkMatch
import dev.anilbeesetti.nextplayer.core.model.kind
import dev.anilbeesetti.nextplayer.core.model.matchesFor
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Asks TMDB which work a file's name is, and puts what it offers in the order it answers the name.
 *
 * Requests go to `api.tmdb.org` and not to the `api.themoviedb.org` that the documentation names.
 * The two serve the same API, but only the shorter one can be reached on the connection this app is
 * for: from the box it runs on, `api.tmdb.org` replies in about 160ms while every packet sent to the
 * other is lost, and a player built against the other host failed there with a timeout. Posters come
 * from `image.tmdb.org`, which answers as well.
 *
 * A search is never narrowed by year, not even for a film, because a catalogue holding the work
 * under a date the release name disagrees with would then not offer it at all. A year is for putting
 * in order what a title found, which is what [matchesFor] does with it.
 */
@Singleton
class TmdbCatalogue internal constructor(
    private val apiKey: TmdbApiKey,
    private val fetchText: (url: String) -> String,
) {

    @Inject
    constructor(apiKey: TmdbApiKey) : this(apiKey, ::httpText)

    /**
     * The works TMDB offers for [name], best answer first, or the failure that stopped it answering.
     *
     * Both of the titles a name carries are searched for and what the two found is put together,
     * because TMDB finds a work by its Chinese title and by the one it was released under but not
     * always as readily by both: `硅谷` and `Silicon Valley` each find the same series, while a work
     * nobody has filed a translation of is only ever found by one of them. A work both searches
     * found is the same work and is offered once.
     *
     * Only the one catalogue that suits the name is searched, films for a film and series for an
     * episode. Searching the other after this one found little would turn a name it has no work for
     * into a confident answer of the wrong kind, which is worse than saying nothing.
     */
    suspend fun search(name: MediaName): Result<List<WorkMatch>> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey.value() ?: throw IOException("No TMDB API key has been supplied")
            val titles = listOfNotNull(name.title, name.otherTitle).filter { it.isNotBlank() }
            val works = titles.flatMap { title -> worksFor(key, name.kind, title) }
            matchesFor(name, works.distinctBy { it.id })
        }
    }

    private fun worksFor(key: String, kind: WorkKind, title: String): List<CatalogueWork> {
        val catalogue = if (kind == WorkKind.FILM) "movie" else "tv"
        val query = URLEncoder.encode(title, Charsets.UTF_8.name())
        val body = fetchText("$SEARCH_URL/$catalogue?api_key=$key&language=$LANGUAGE&query=$query")
        return json.decodeFromString<Answer>(body).results.map { it.asWork(kind) }
    }

    private companion object {

        const val SEARCH_URL = "https://api.tmdb.org/3/search"

        /**
         * The language titles are wanted in. Asked in Chinese, TMDB gives a work's Chinese title
         * where somebody has filed one and leaves the original standing where nobody has, so both of
         * the titles a file's name might carry come back from the one request.
         */
        const val LANGUAGE = "zh-CN"

        /** A catalogue answers with far more about a work than matching it takes. */
        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * A page of what a search found.
 *
 * Both catalogues answer in the same shape but under different names for the same things: a film has
 * a `title` and a `release_date` where a series has a `name` and a `first_air_date`. One reader
 * takes either, since a search only ever asks one of the two and the other's fields simply stay
 * empty.
 */
@Serializable
private class Answer(val results: List<Found> = emptyList())

@Serializable
private class Found(
    val id: Int,
    val title: String = "",
    val name: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    @SerialName("original_name") val originalName: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("first_air_date") val firstAirDate: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    val overview: String = "",
    val popularity: Double = 0.0,
)

private fun Found.asWork(kind: WorkKind) = CatalogueWork(
    kind = kind,
    id = id,
    title = title.ifEmpty { name },
    originalTitle = originalTitle.ifEmpty { originalName },
    year = releaseDate.ifEmpty { firstAirDate }.take(4).toIntOrNull(),
    popularity = popularity,
    posterPath = posterPath,
    overview = overview,
)
