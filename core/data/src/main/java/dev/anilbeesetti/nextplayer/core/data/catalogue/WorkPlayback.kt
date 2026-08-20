package dev.anilbeesetti.nextplayer.core.data.catalogue

import dev.anilbeesetti.nextplayer.core.database.entities.ItemPlayback
import dev.anilbeesetti.nextplayer.core.model.LibraryEpisode

/**
 * The episodes of a work, each knowing where it was left.
 *
 * An episode can sit in more than one file — a second copy, a different cut — so the rows of one
 * episode collapse into the file it was last played from, since that is the copy holding the
 * position worth returning to. An episode never played opens on any file it has.
 */
internal fun List<ItemPlayback>.toEpisodes(): List<LibraryEpisode> =
    groupBy { it.itemId }
        .map { (itemId, rows) -> rows.toEpisode(itemId) }
        .sortedWith(compareBy(LibraryEpisode::season, LibraryEpisode::episode))

private fun List<ItemPlayback>.toEpisode(itemId: Long): LibraryEpisode {
    val played = filter { it.lastPlayedTime != null }.maxByOrNull { it.lastPlayedTime!! }
    return LibraryEpisode(
        id = itemId,
        season = first().season,
        episode = first().episode,
        mediaKey = played?.uri ?: firstNotNullOfOrNull { it.uri },
        lastPlayedTime = played?.lastPlayedTime,
        playbackPosition = played?.playbackPosition ?: 0L,
    )
}
