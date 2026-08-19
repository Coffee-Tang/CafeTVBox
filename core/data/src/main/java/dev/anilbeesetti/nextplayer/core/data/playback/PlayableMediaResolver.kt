package dev.anilbeesetti.nextplayer.core.data.playback

import android.net.Uri
import androidx.core.net.toUri
import dev.anilbeesetti.nextplayer.core.data.live.LiveChannelGatherer
import dev.anilbeesetti.nextplayer.core.data.repository.NetworkConnectionRepository
import dev.anilbeesetti.nextplayer.core.media.live.LiveMediaKey
import dev.anilbeesetti.nextplayer.core.media.network.NetworkMediaKey
import dev.anilbeesetti.nextplayer.core.media.network.proxy.NetworkStreamingProxy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a stored media key back into something the player can open.
 *
 * Two kinds of key need work. A file on a network connection was played through a proxy URL that is
 * gone once the app restarts, so a fresh one is registered. A live channel names a station rather
 * than an address, so the configured playlists are consulted for a line it is carried on. Every
 * other key is a URI in its own right.
 */
@Singleton
class PlayableMediaResolver @Inject constructor(
    private val connectionRepository: NetworkConnectionRepository,
    private val streamingProxy: NetworkStreamingProxy,
    private val gatherLiveChannels: LiveChannelGatherer,
) {

    /**
     * The URI to play [mediaKey] from, or null when what it names has gone: a connection that is no
     * longer configured, or a channel no configured playlist carries any more.
     */
    suspend fun resolve(mediaKey: String): Uri? {
        LiveMediaKey.of(mediaKey)?.let { channel -> return resolveLive(channel) }
        val location = NetworkMediaKey.of(mediaKey) ?: return mediaKey.toUri()
        val connection = connectionRepository.getConnection(location.connectionId) ?: return null
        return streamingProxy.registerStream(
            connection = connection,
            filePath = location.path,
            fileName = location.fileName,
        ).toUri()
    }

    private suspend fun resolveLive(channel: LiveMediaKey): Uri? =
        gatherLiveChannels.channelFor(channel.channelKey)?.url?.toUri()
}
