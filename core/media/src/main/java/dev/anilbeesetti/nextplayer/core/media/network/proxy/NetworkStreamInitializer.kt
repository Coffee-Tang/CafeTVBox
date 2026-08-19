package dev.anilbeesetti.nextplayer.core.media.network.proxy

import dev.anilbeesetti.nextplayer.core.media.network.NetworkClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes the shared-client setup required before a response opens its own file stream.
 *
 * One initializer serves every file of a playback session, so the files queued after the current
 * one cannot each start their own connection attempt.
 */
internal class NetworkStreamInitializer(private val client: NetworkClient) {
    private val mutex = Mutex()
    private val fileSizes = mutableMapOf<String, Long>()

    suspend fun initialize(filePath: String): Long = mutex.withLock {
        if (!client.isConnected()) client.connect().getOrThrow()
        fileSizes.getOrPut(filePath) { client.fileSize(filePath) }
    }
}
