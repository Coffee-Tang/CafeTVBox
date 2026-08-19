package dev.anilbeesetti.nextplayer.core.model

import kotlin.math.abs
import kotlin.math.max

/**
 * Whether a work is a film or a series.
 *
 * A catalogue keeps the two apart and so must a search: a film and a series of the same title are
 * different works, and the numbering in `S03E01` means something to only one of them.
 */
enum class WorkKind {
    FILM,
    SERIES,
}

/** The kind of work a name can be, and so the only kind worth looking for it among. */
val MediaName.kind: WorkKind
    get() = when (this) {
        is MediaName.Film -> WorkKind.FILM
        is MediaName.Episode -> WorkKind.SERIES
    }

/**
 * A work a catalogue offers, in as much of it as deciding whether it is the file's work takes, plus
 * the little that showing it on a shelf afterwards will want.
 *
 * Both titles are kept because a catalogue asked in Chinese answers with a Chinese [title] and with
 * the [originalTitle] the work was released under, and a file's name may carry either of them:
 * `硅谷` is the one and `Silicon Valley` the other for the same series.
 */
data class CatalogueWork(
    val kind: WorkKind,
    /** What the catalogue calls this work, which is all that asking it for more later takes. */
    val id: Int,
    /** The title in the language the catalogue was asked in. */
    val title: String,
    /** The title the work was released under, in whatever language that was. */
    val originalTitle: String,
    /** The year a film came out or a series first aired, and null when the catalogue omits it. */
    val year: Int?,
    /**
     * How much attention the work is getting, on an open-ended scale. It says nothing about whether
     * this is the right work, so it only ever settles a tie between answers otherwise as good as
     * each other.
     */
    val popularity: Double,
    /** Where the poster is kept, to be asked of `image.tmdb.org`, or null when there is none. */
    val posterPath: String?,
    val overview: String,
)

/** A work a search turned up, and how well it answers the name that was searched for. */
data class WorkMatch(
    val work: CatalogueWork,
    /**
     * How good an answer this is, for putting the answers in order and for nothing else.
     *
     * It is mostly [titleSimilarity], which the year and the work's popularity can only nudge, so
     * that a year the catalogue disagrees with reorders answers whose titles are as good as each
     * other but never overturns a title that matches outright.
     */
    val score: Double,
    /**
     * How much the searched title and this work's titles have in common, from 0 for nothing to 1
     * for the same title spelled the same way.
     */
    val titleSimilarity: Double,
) {

    /**
     * Whether the name searched for and this work are titled the same, once case and the
     * punctuation a release name is split up by are set aside.
     *
     * This is as much certainty as searching by title can offer, and it is not certainty about the
     * work: two works can be titled the same, as a series and its remake are, and then two matches
     * are certain at once and only a viewer can say which was meant. One certain match among
     * uncertain ones is the case worth binding to without asking anybody.
     */
    val certain: Boolean
        get() = titleSimilarity >= SAME_TITLE
}

/**
 * The [works] that could be what [name] names, best answer first.
 *
 * A work of the wrong kind is dropped rather than ranked low, because a film is not a poor answer
 * to an episode's name but no answer at all; a caller that searched the wrong catalogue is told so
 * by getting nothing back rather than by getting something plausible.
 */
fun matchesFor(name: MediaName, works: List<CatalogueWork>): List<WorkMatch> {
    val searched = listOfNotNull(name.title, name.otherTitle).map(::comparably).filter(String::isNotEmpty)
    val yearWeight = if (name is MediaName.Film) YEAR_WEIGHT_FOR_A_FILM else YEAR_WEIGHT_FOR_A_SEASON
    return works
        .filter { it.kind == name.kind }
        .map { work ->
            val similarity = titleSimilarity(searched, work)
            WorkMatch(
                work = work,
                score = similarity +
                    yearWeight * yearAgreement(name, work.year) +
                    POPULARITY_WEIGHT * attention(work.popularity),
                titleSimilarity = similarity,
            )
        }
        .sortedByDescending { it.score }
}

/**
 * The best any of the titles [searched] for does against any of the titles [work] carries.
 *
 * Every pairing is tried because neither side says which language it names the work in: a file
 * named `硅谷.Silicon.Valley` gives both, a catalogue answers with both, and which of the two
 * actually found the work differs from work to work.
 */
private fun titleSimilarity(searched: List<String>, work: CatalogueWork): Double {
    val offered = listOf(work.title, work.originalTitle).map(::comparably).filter(String::isNotEmpty)
    return searched.maxOfOrNull { one ->
        offered.maxOfOrNull { other -> similarity(one, other) } ?: 0.0
    } ?: 0.0
}

/**
 * How far the year [name] carries backs [year] up, from 0 for no support at all to 1 for full
 * support. It never reaches below 0, because a year is here to tell good answers apart and not to
 * rule any of them out.
 *
 * A film's year dates the film, so the two agreeing is real support and the two being far apart is
 * real doubt. An episode's year dates its season instead, and comparing that to when a series began
 * would count the true answer as five years wrong for `Silicon Valley.2019.S06E01`, whose series
 * began in 2014. What the season's number and year say together is roughly when the series began,
 * a season a year being the usual pace, so it is that estimate the catalogue's date is held against
 * and seasons skipping years is what the tolerance is for. A series the catalogue dates after the
 * season was broadcast cannot be the one, but dates go missing and go wrong often enough that even
 * that only withholds support: what a year can give either way is a fraction of what a title gives,
 * so a title matching outright still wins.
 */
private fun yearAgreement(name: MediaName, year: Int?): Double {
    if (year == null) return 0.0
    return when (name) {
        is MediaName.Film -> {
            val released = name.year ?: return 0.0
            max(0.0, 1.0 - abs(released - year) / A_FILM_MAY_BE_DATED_WRONG_BY)
        }

        is MediaName.Episode -> {
            val broadcast = name.seasonYear ?: return 0.0
            if (year > broadcast) return 0.0
            val began = broadcast - (name.season - 1)
            max(0.0, 1.0 - abs(began - year) / A_SEASON_MAY_SLIP_BY)
        }
    }
}

/**
 * Popularity as a number between 0 and 1, so that a runaway hit counts for no more than any other
 * well-known work and cannot outweigh the things ranked above it.
 */
private fun attention(popularity: Double): Double = popularity / (popularity + POPULAR_ENOUGH)

/**
 * How alike two titles are, from 0 to 1, measured by how much of the longer one would have to be
 * rewritten to become the other.
 *
 * Counting single characters suits both of the scripts this library is named in: it reads
 * `Silicon Valley` against `Silicon Valley: The Untold Story` as roughly half a match and `天道`
 * against `大道通天` as a quarter of one, and those gaps are what ordering answers needs.
 */
private fun similarity(one: String, other: String): Double {
    if (one == other) return 1.0
    if (one.isEmpty() || other.isEmpty()) return 0.0
    return 1.0 - rewrites(one, other).toDouble() / max(one.length, other.length)
}

/** The fewest single-character insertions, removals and replacements that turn one title into another. */
private fun rewrites(one: String, other: String): Int {
    var previous = IntArray(other.length + 1) { it }
    for (i in 1..one.length) {
        val current = IntArray(other.length + 1)
        current[0] = i
        for (j in 1..other.length) {
            val replaced = previous[j - 1] + if (one[i - 1] == other[j - 1]) 0 else 1
            current[j] = minOf(replaced, previous[j] + 1, current[j - 1] + 1)
        }
        previous = current
    }
    return previous[other.length]
}

/**
 * A title with everything that spells it rather than names it taken out, so that `Silicon.Valley`,
 * `Silicon Valley` and `silicon valley` are one title and not three.
 */
private fun comparably(title: String): String = title.lowercase().filter(Char::isLetterOrDigit)

/**
 * Nothing short of every character agreeing counts as the same title. [similarity] answers exactly
 * 1 for that case and strictly less for every other, so this compares no rounded quantity.
 */
private const val SAME_TITLE = 1.0

/**
 * What a year is allowed to be worth beside a title. A film's year dates the work itself and an
 * episode's only dates a season of it, so the firmer of the two counts for three times the softer,
 * and both count for a fraction of a title. Popularity settles what neither could.
 */
private const val YEAR_WEIGHT_FOR_A_FILM = 0.15
private const val YEAR_WEIGHT_FOR_A_SEASON = 0.05
private const val POPULARITY_WEIGHT = 0.01

/** How far out a film's year has to be before it stops saying anything in the work's favour. */
private const val A_FILM_MAY_BE_DATED_WRONG_BY = 4.0

/** How many years of skipped and doubled-up seasons the estimate of a series' first year allows. */
private const val A_SEASON_MAY_SLIP_BY = 6.0

/** The popularity at which a work counts as half as well-known as anything could be. */
private const val POPULAR_ENOUGH = 20.0
