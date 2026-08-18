package dev.anilbeesetti.nextplayer.core.data.playback

import android.net.Uri
import androidx.core.net.toUri
import dev.anilbeesetti.nextplayer.core.data.repository.NetworkConnectionRepository
import dev.anilbeesetti.nextplayer.core.media.network.NetworkMediaKey
import dev.anilbeesetti.nextplayer.core.media.network.proxy.NetworkStreamingProxy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a stored media key back into something the player can open.
 *
 * Only files on a network connection need work: the proxy URL they were played from is gone once the
 * app restarts, so a fresh one is registered. Every other key is a URI in its own right.
 */
@Singleton
class PlayableMediaResolver @Inject constructor(
    private val connectionRepository: NetworkConnectionRepository,
    private val streamingProxy: NetworkStreamingProxy,
) {

    /** The URI to play [mediaKey] from, or null when the connection it names no longer exists. */
    suspend fun resolve(mediaKey: String): Uri? {
        val location = NetworkMediaKey.of(mediaKey) ?: return mediaKey.toUri()
        val connection = connectionRepository.getConnection(location.connectionId) ?: return null
        return streamingProxy.registerStream(
            connection = connection,
            filePath = location.path,
            fileName = location.fileName,
        ).toUri()
    }
}
