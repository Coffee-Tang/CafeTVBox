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
 * Something the player can open: where to start, and the other lines to fall back on when what was
 * asked for is a station carried on more than one.
 */
data class PlayableMedia(val uri: Uri, val lines: List<String> = emptyList())

/**
 * Turns a stored media key back into something the player can open.
 *
 * Two kinds of key need work. A file on a network connection was played through a proxy URL that is
 * gone once the app restarts, so a fresh one is registered. A live channel names a station rather
 * than an address, so the configured playlists are consulted for the lines it is carried on. Every
 * other key is a URI in its own right.
 */
@Singleton
class PlayableMediaResolver @Inject constructor(
    private val connectionRepository: NetworkConnectionRepository,
    private val streamingProxy: NetworkStreamingProxy,
    private val gatherLiveChannels: LiveChannelGatherer,
) {

    /**
     * What to play [mediaKey] from, or null when what it names has gone: a connection that is no
     * longer configured, or a channel no configured playlist carries any more.
     */
    suspend fun resolve(mediaKey: String): PlayableMedia? {
        LiveMediaKey.of(mediaKey)?.let { channel -> return resolveLive(channel) }
        val location = NetworkMediaKey.of(mediaKey) ?: return PlayableMedia(mediaKey.toUri())
        val connection = connectionRepository.getConnection(location.connectionId) ?: return null
        return PlayableMedia(
            streamingProxy.registerStream(
                connection = connection,
                filePath = location.path,
                fileName = location.fileName,
            ).toUri(),
        )
    }

    /**
     * Every line the station is carried on, in the order the sources offer them.
     *
     * Handing over all of them is what lets playback move on by itself when the line it starts on is
     * dead, which a line out of a kept playlist may well be. Which to try first is not decided here:
     * the player leads with whichever line last came through for the station.
     */
    private suspend fun resolveLive(channel: LiveMediaKey): PlayableMedia? =
        gatherLiveChannels.channelFor(channel.channelKey)
            ?.let { station -> PlayableMedia(station.url.toUri(), station.urls) }
}
