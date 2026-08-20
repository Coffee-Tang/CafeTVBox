package dev.anilbeesetti.nextplayer.core.model.search

/**
 * The items [query] can be said to name, best answer first.
 *
 * [names] gives the ways one item can be named — a title, the title it is also known by, the path
 * it sits at — and the nearest of them decides where the item lands.
 *
 * Items that score alike keep the order they came in, so a list already arranged sensibly, works by
 * title or channels by source, stays that way among equals.
 */
fun <T> Iterable<T>.rankedBy(query: String, names: (T) -> List<String?>): List<T> {
    val matcher = SearchMatcher(query)
    return mapNotNull { item ->
        val score = matcher.score(*names(item).toTypedArray())
        if (score > 0) item to score else null
    }.sortedByDescending { it.second }.map { it.first }
}
