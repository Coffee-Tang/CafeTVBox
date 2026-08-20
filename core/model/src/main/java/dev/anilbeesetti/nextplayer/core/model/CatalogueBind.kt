package dev.anilbeesetti.nextplayer.core.model

/**
 * The work to bind without asking, or null when a person has to choose.
 *
 * One certain match among uncertain ones is as much as a title search can offer. Two certain
 * matches are two works titled the same, as `天道` is, and only the viewer can say which was meant.
 */
fun autoBind(matches: List<WorkMatch>): CatalogueWork? {
    val certain = matches.filter { it.certain }
    return certain.singleOrNull()?.work
}

/** A name the catalogue can be searched with for a work the library already holds. */
fun searchName(
    kind: WorkKind,
    title: String,
    otherTitle: String?,
    year: Int?,
): MediaName = when (kind) {
    WorkKind.FILM -> MediaName.Film(title, otherTitle, year)
    WorkKind.SERIES -> MediaName.Episode(title, otherTitle, season = 1, episode = 1, seasonYear = year)
}
