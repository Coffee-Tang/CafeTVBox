package dev.anilbeesetti.nextplayer.core.model

/**
 * A work as the shelf shows it: what was last scanned, and the poster if a catalogue has been bound.
 */
data class LibraryWork(
    val id: Long,
    val libraryId: Long,
    val workKey: String,
    val kind: WorkKind,
    val title: String,
    val otherTitle: String?,
    val year: Int?,
    val posterUrl: String?,
)

/**
 * The address a TMDB poster is fetched from, or [posterPath] itself when it is already an address.
 * The two-size copies that live on the share are a later step; until then the shelf reads TMDB.
 */
fun tmdbImageUrl(posterPath: String?, width: String = "w500"): String? {
    if (posterPath.isNullOrBlank()) return null
    if (posterPath.startsWith("http://") || posterPath.startsWith("https://")) return posterPath
    val path = if (posterPath.startsWith("/")) posterPath else "/$posterPath"
    return "https://image.tmdb.org/t/p/$width$path"
}
