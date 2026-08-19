package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.live.M3uParser
import dev.anilbeesetti.nextplayer.core.data.network.httpFetch
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class HttpLiveChannelRepository @Inject constructor() : LiveChannelRepository {

    override suspend fun getChannels(url: String): Result<List<LiveChannel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val playlist = httpFetch(url) { body -> body.bufferedReader().readText() }
                val channels = M3uParser.parse(playlist)
                if (channels.isEmpty()) throw IOException("Playlist contains no channels")
                channels
            }
        }
}
