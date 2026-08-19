package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.live.M3uParser
import dev.anilbeesetti.nextplayer.core.data.network.httpFetch
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Serves the channels of an m3u playlist, keeping each playlist it has read in memory.
 *
 * The copy lasts for the run only: a playlist is small enough to fetch again on the next start,
 * and a stale one would offer lines its source has already retired.
 */
@Singleton
class HttpLiveChannelRepository @Inject constructor() : LiveChannelRepository {

    private val mutex = Mutex()
    private val cachedPlaylists = mutableMapOf<String, List<LiveChannel>>()

    override suspend fun getChannels(url: String, refresh: Boolean): Result<List<LiveChannel>> =
        withContext(Dispatchers.IO) {
            val cached = if (refresh) null else mutex.withLock { cachedPlaylists[url] }
            if (cached != null) return@withContext Result.success(cached)
            runCatching {
                val playlist = httpFetch(url) { body -> body.bufferedReader().readText() }
                val channels = M3uParser.parse(playlist)
                if (channels.isEmpty()) throw IOException("Playlist contains no channels")
                channels
            }.onSuccess { channels -> mutex.withLock { cachedPlaylists[url] = channels } }
        }
}
