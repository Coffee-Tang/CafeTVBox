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

    /**
     * The channels [url] last gave, from this run or an earlier one, or null when it has given none.
     *
     * Opening a source takes seconds on a home connection while a playlist weighs tens of kilobytes
     * and changes rarely, so what it said last time is worth showing while it is asked again. This
     * is deliberately the only way to reach a kept copy: a caller has to say that a list of unknown
     * age will do, which is not true of everyone who wants channels.
     */
    suspend fun getStoredChannels(url: String): List<LiveChannel>?
}
