package dev.anilbeesetti.nextplayer.feature.network.screens.browse

import dev.anilbeesetti.nextplayer.core.model.NetworkFile

/** The files to play from a folder, in the order it is listed in, and which one to start with. */
internal data class FolderQueue(val files: List<NetworkFile>, val startIndex: Int)

/**
 * The queue for playing [clicked] out of the folder listing [files].
 *
 * Playback carries on through the rest of the folder, so what is queued is every file shown after
 * (and before) it, in the same order — folders are left out, as they are not playable.
 *
 * `null` when [clicked] is not a playable entry of this listing.
 */
internal fun folderQueueFor(files: List<NetworkFile>, clicked: NetworkFile): FolderQueue? {
    if (clicked.isDirectory) return null
    val playable = files.filterNot { it.isDirectory }
    val startIndex = playable.indexOfFirst { it.path == clicked.path }
    if (startIndex < 0) return null
    return FolderQueue(files = playable, startIndex = startIndex)
}
