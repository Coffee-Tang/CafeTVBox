package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.PlaylistStore
import dev.anilbeesetti.nextplayer.core.data.live.M3uParser
import dev.anilbeesetti.nextplayer.core.data.network.httpFetch
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Serves the channels of an m3u playlist, keeping each playlist it reads in memory and on disk.
 *
 * The copy on disk is what lets the channel list appear at once after a cold start, which on a TV
 * box is most starts. Reading the configured sources took around nine seconds on the connection this
 * was measured on, nearly all of it spent reaching hosts abroad rather than transferring the few
 * tens of kilobytes a playlist weighs, so there is nothing to gain by making the download itself
 * quicker. A kept copy is served only through [getStoredChannels], never from [getChannels], which
 * always reads the source. Offering a line the source has since retired costs little now that
 * playback moves on to the next line by itself.
 */
@Singleton
class HttpLiveChannelRepository @Inject constructor(
    @PlaylistStore private val store: File,
) : LiveChannelRepository {

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
                keep(url, playlist)
                channels
            }.onSuccess { channels -> mutex.withLock { cachedPlaylists[url] = channels } }
        }

    override suspend fun getStoredChannels(url: String): List<LiveChannel>? =
        withContext(Dispatchers.IO) {
            mutex.withLock { cachedPlaylists[url] }
                ?: runCatching { M3uParser.parse(fileFor(url).readText()) }
                    .getOrNull()
                    ?.takeIf { it.isNotEmpty() }
        }

    /**
     * Writes the playlist beside its place and moves it in, so that a run that dies partway cannot
     * leave half a playlist to be read as a whole one. The name written under is unique because the
     * same address can be configured as two sources, whose reads would otherwise write over each
     * other's unfinished file. Failing to keep a copy is not worth reporting to a caller that
     * already has the channels it asked for.
     */
    private fun keep(url: String, playlist: String) {
        runCatching {
            store.mkdirs()
            val partial = File.createTempFile("playlist", ".part", store)
            try {
                partial.writeText(playlist)
                if (!partial.renameTo(fileFor(url))) throw IOException("Cannot move $partial in")
            } finally {
                partial.delete()
            }
        }
    }

    /** Names a copy after a digest of its address, an address not being usable as a filename. */
    private fun fileFor(url: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
        return File(store, "$digest.m3u")
    }
}
