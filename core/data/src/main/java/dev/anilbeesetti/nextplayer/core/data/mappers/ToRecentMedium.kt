package dev.anilbeesetti.nextplayer.core.data.mappers

import dev.anilbeesetti.nextplayer.core.database.entities.MediumStateEntity
import dev.anilbeesetti.nextplayer.core.media.network.NetworkMediaKey
import dev.anilbeesetti.nextplayer.core.model.RecentMedium

fun MediumStateEntity.toRecentMedium(): RecentMedium = RecentMedium(
    mediaKey = uriString,
    title = title.orEmpty(),
    source = sourceOf(uriString),
    positionMs = playbackPosition,
    durationMs = duration,
    lastPlayedTime = lastPlayedTime ?: 0L,
)

/**
 * Where a media key points, told from the key itself.
 *
 * Files on the device carry their content or file URI, network files carry a [NetworkMediaKey], and
 * anything else is a URL played as it arrives.
 */
private fun sourceOf(mediaKey: String): RecentMedium.Source = when {
    NetworkMediaKey.of(mediaKey) != null -> RecentMedium.Source.SHARE
    mediaKey.startsWith("content://") || mediaKey.startsWith("file://") -> RecentMedium.Source.LOCAL
    else -> RecentMedium.Source.STREAM
}
