package dev.anilbeesetti.nextplayer.core.domain

import dev.anilbeesetti.nextplayer.core.data.live.mergeChannels
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
 */
class GetLiveChannelsUseCase @Inject constructor(
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
}
