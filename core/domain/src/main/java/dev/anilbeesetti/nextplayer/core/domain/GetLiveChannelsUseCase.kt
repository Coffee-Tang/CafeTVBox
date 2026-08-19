package dev.anilbeesetti.nextplayer.core.domain

import dev.anilbeesetti.nextplayer.core.data.live.LiveChannelGatherer
import dev.anilbeesetti.nextplayer.core.data.live.LiveChannels
import javax.inject.Inject

/**
 * Every channel the configured sources offer between them.
 *
 * The reading and merging itself lives beside the playlists in the layer below, since playback has
 * to do the same thing when it turns a remembered channel back into a line.
 */
class GetLiveChannelsUseCase @Inject constructor(
    private val gatherLiveChannels: LiveChannelGatherer,
) {

    suspend operator fun invoke(refresh: Boolean = false): LiveChannels = gatherLiveChannels(refresh)

    /**
     * What the sources gave when they were last read, or null while none of them has given anything.
     *
     * Worth showing at once, and replacing with [invoke], when waiting on the sources would otherwise
     * leave the reader looking at nothing for several seconds.
     */
    suspend fun stored(): LiveChannels? = gatherLiveChannels.stored()
}
