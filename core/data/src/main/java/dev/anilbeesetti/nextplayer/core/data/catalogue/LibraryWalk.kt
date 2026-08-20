package dev.anilbeesetti.nextplayer.core.data.catalogue

import dev.anilbeesetti.nextplayer.core.media.network.isNetworkBrowsableEntry
import dev.anilbeesetti.nextplayer.core.model.LibraryEntry
import dev.anilbeesetti.nextplayer.core.model.NetworkFile

/**
 * Walks [rootPath] and every folder under it, and returns the videos as [LibraryEntry]s.
 *
 * Pictures, sidecars and housekeeping directories are already refused by
 * [isNetworkBrowsableEntry], so a `海报.jpg` never becomes a work titled 海报.
 */
internal suspend fun collectLibraryEntries(
    listFiles: suspend (String) -> Result<List<NetworkFile>>,
    rootPath: String,
): List<LibraryEntry> {
    val found = mutableListOf<LibraryEntry>()
    val pending = ArrayDeque<String>()
    val seen = mutableSetOf<String>()
    pending += rootPath
    while (pending.isNotEmpty()) {
        val path = pending.removeFirst()
        if (!seen.add(path)) continue
        val entries = listFiles(path).getOrElse { continue }
        for (entry in entries) {
            if (!isNetworkBrowsableEntry(entry.name, entry.isDirectory)) continue
            if (entry.isDirectory) {
                pending += entry.path
            } else {
                found += LibraryEntry(
                    fileName = entry.name,
                    folderName = folderNameOf(entry.path, rootPath),
                    path = entry.path,
                )
            }
        }
    }
    return found
}

private fun folderNameOf(filePath: String, rootPath: String): String {
    val parent = filePath.trimEnd('/').substringBeforeLast('/', missingDelimiterValue = "")
    val name = parent.trimEnd('/').substringAfterLast('/')
    if (name.isNotEmpty()) return name
    return rootPath.trimEnd('/').substringAfterLast('/').ifEmpty { rootPath }
}
