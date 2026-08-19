package dev.anilbeesetti.nextplayer.core.data.repository.fake

import android.net.Uri
import dev.anilbeesetti.nextplayer.core.data.models.VideoState
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.model.Folder
import dev.anilbeesetti.nextplayer.core.model.MediaInfo
import dev.anilbeesetti.nextplayer.core.model.RecentMedium
import dev.anilbeesetti.nextplayer.core.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMediaRepository : MediaRepository {

    val videos = mutableListOf<Video>()
    val directories = mutableListOf<Folder>()
    val recentlyPlayed = mutableListOf<RecentMedium>()
    private val updates = MutableStateFlow(0L)

    override fun observeFolders(folderPath: String?): Flow<List<Folder>> = updates.map {
        directories.filter { folderPath == null || it.path.startsWith(folderPath) }
    }

    override fun observeVideos(folderPath: String?): Flow<List<Video>> = updates.map {
        videos.filter { folderPath == null || it.path.startsWith(folderPath) }
    }

    override suspend fun fetchFolders(folderPath: String?): List<Folder> = directories.filter { folderPath == null || it.path.startsWith(folderPath) }

    override suspend fun fetchVideos(folderPath: String?): List<Video> = videos.filter { folderPath == null || it.path.startsWith(folderPath) }

    override suspend fun getVideoByUri(uri: String): Video? = videos.find { it.uriString == uri }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<RecentMedium>> = updates.map { recentlyPlayed.take(limit) }

    override suspend fun removeFromRecentlyPlayed(mediaKey: String) {
        recentlyPlayed.removeAll { it.mediaKey == mediaKey }
        notifyMediaChanged()
    }

    override suspend fun clearRecentlyPlayed() {
        recentlyPlayed.clear()
        notifyMediaChanged()
    }

    fun notifyMediaChanged() {
        updates.value += 1
    }

    override suspend fun getVideoState(uri: String): VideoState? = null

    override suspend fun getMediaInfo(uri: String): MediaInfo? = null

    override suspend fun updateMediumPlayback(
        uri: String,
        position: Long,
        title: String?,
        duration: Long?,
    ) {
    }

    override suspend fun updateMediumPlaybackSpeed(uri: String, playbackSpeed: Float) {
    }

    override suspend fun updateMediumAudioTrack(uri: String, audioTrackIndex: Int) {
    }

    override suspend fun updateMediumSubtitleTrack(uri: String, subtitleTrackIndex: Int) {
    }

    override suspend fun updateMediumZoom(uri: String, zoom: Float) {
    }

    override suspend fun addExternalSubtitleToMedium(uri: String, subtitleUri: Uri) {
    }

    override suspend fun updateSubtitleDelay(uri: String, delay: Long) {
    }

    override suspend fun updateSubtitleSpeed(uri: String, speed: Float) {
    }

    override suspend fun updateMediumLastLine(uri: String, line: String) {
    }
}
