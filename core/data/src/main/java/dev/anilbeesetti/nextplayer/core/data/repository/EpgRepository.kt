package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.model.LiveProgramme
import dev.anilbeesetti.nextplayer.core.model.channelKey

interface EpgRepository {

    /**
     * Today's listings, keyed by [channelKey], as a guide and a playlist rarely spell a station
     * the same way.
     *
     * Guides only cover a fraction of the channels a playlist carries, so a channel missing from
     * the result is expected rather than an error.
     */
    suspend fun getGuide(): Result<Map<String, List<LiveProgramme>>>
}
