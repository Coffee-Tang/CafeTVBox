package dev.anilbeesetti.nextplayer.core.data.live

import dev.anilbeesetti.nextplayer.core.data.repository.LiveChannelRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveSourceRepository
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.model.LiveSource
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Every channel the configured sources offer between them, and the sources that could not be read.
 *
 * A source that fails costs the channels only it carried, so the list is still worth showing;
 * [failedSources] is what lets the caller say so. Nothing could be read at all when [channels] is
 * empty and every one of the [sourceCount] sources failed.
 */
data class LiveChannels(
    val channels: List<LiveChannel>,
    val failedSources: List<LiveSource>,
    val sourceCount: Int,
)

/**
 * Reads every configured source at once and gathers them into one list of channels.
 *
 * Sources are read in parallel because a slow one would otherwise hold up all the others, and the
 * results are put back in source order so that the oldest source keeps first claim on a station's
 * name and its lines are tried first.
 *
 * This sits beside the merge it ends in rather than in the layer above, because playback needs it
 * too: turning a stored channel key back into a line means asking the same question the channel
 * list asks, and one answer serves both.
 */
class LiveChannelGatherer @Inject constructor(
    private val sourceRepository: LiveSourceRepository,
    private val channelRepository: LiveChannelRepository,
) {

    suspend operator fun invoke(refresh: Boolean = false): LiveChannels {
        val sources = sourceRepository.getSources().first()
        val playlists = coroutineScope {
            sources
                .map { source -> async { channelRepository.getChannels(source.url, refresh) } }
                .awaitAll()
        }
        return LiveChannels(
            channels = mergeChannels(playlists.mapNotNull { it.getOrNull() }),
            failedSources = sources.filterIndexed { index, _ -> playlists[index].isFailure },
            sourceCount = sources.size,
        )
    }

    /**
     * What the sources gave when they were last read, or null while none of them has given anything.
     *
     * Gathered and merged exactly as [invoke] does, so that the list shown while the sources are
     * being read does not visibly rearrange itself into the list that replaces it. Sources that have
     * given nothing yet are simply absent, which makes this a partial answer and never a failed one:
     * nothing was attempted here, so [LiveChannels.failedSources] has nothing to report.
     */
    suspend fun stored(): LiveChannels? {
        val sources = sourceRepository.getSources().first()
        val playlists = sources.mapNotNull { source -> channelRepository.getStoredChannels(source.url) }
        if (playlists.isEmpty()) return null
        return LiveChannels(
            channels = mergeChannels(playlists),
            failedSources = emptyList(),
            sourceCount = sources.size,
        )
    }

    /**
     * The station [key] names, with every line the sources carry it on, or null when none does.
     *
     * What the sources gave last time is enough to answer with, and answering from it is what keeps
     * resuming a channel from waiting seconds on the sources. They are read when it has no such
     * station, so that one added since the channel list was last refreshed is still playable.
     */
    suspend fun channelFor(key: String): LiveChannel? =
        stored()?.channels?.channelForKey(key) ?: invoke().channels.channelForKey(key)
}
