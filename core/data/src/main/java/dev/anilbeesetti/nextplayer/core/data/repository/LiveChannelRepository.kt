package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.model.LiveChannel

interface LiveChannelRepository {

    /**
     * Returns the channels of the m3u playlist at [url], or a failure if the playlist could not be
     * fetched or contains no channels.
     *
     * A playlist that was read once is kept for the rest of the run, as reopening the channel list
     * must not pay to download every source again. Pass [refresh] to read it afresh.
     */
    suspend fun getChannels(url: String, refresh: Boolean = false): Result<List<LiveChannel>>
}
