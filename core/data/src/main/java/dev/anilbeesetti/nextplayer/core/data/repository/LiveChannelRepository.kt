package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.model.LiveChannel

interface LiveChannelRepository {

    /**
     * Downloads the m3u playlist at [url] and returns its channels, or a failure if the playlist
     * could not be fetched or contains no channels.
     */
    suspend fun getChannels(url: String): Result<List<LiveChannel>>
}
