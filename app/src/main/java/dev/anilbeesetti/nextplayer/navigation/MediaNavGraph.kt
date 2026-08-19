package dev.anilbeesetti.nextplayer.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.anilbeesetti.nextplayer.core.data.playback.PlayableMedia
import dev.anilbeesetti.nextplayer.core.media.live.LiveMediaKey
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.model.channelKey
import dev.anilbeesetti.nextplayer.feature.player.EXTRA_LIVE_LINES
import dev.anilbeesetti.nextplayer.feature.player.EXTRA_MEDIA_KEY
import dev.anilbeesetti.nextplayer.feature.player.EXTRA_MEDIA_KEYS
import dev.anilbeesetti.nextplayer.feature.player.EXTRA_MEDIA_TITLE
import dev.anilbeesetti.nextplayer.feature.player.PlayerActivity
import dev.anilbeesetti.nextplayer.feature.player.utils.PlayerApi
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.mediaPickerEntry
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.navigateToMediaPickerScreen
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.navigateToSearch
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.navigateToVault
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.navigateToWatchHistory
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.searchEntry
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.vaultEntry
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.watchHistoryEntry
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToSettings

fun EntryProviderScope<NavKey>.mediaNavGraph(
    context: Context,
    backStack: NavBackStack<NavKey>,
) {
    mediaPickerEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
        onPlayVideo = { uri -> context.startPlayback(uri) },
        onPlayVideos = { uris -> context.startPlayback(uris) },
        onResumeWatching = { media, mediaKey, title ->
            context.startPlayback(media, mediaKey = mediaKey, title = title)
        },
        onWatchHistoryClick = backStack::navigateToWatchHistory,
        onFolderClick = backStack::navigateToMediaPickerScreen,
        onSettingsClick = backStack::navigateToSettings,
        onSearchClick = backStack::navigateToSearch,
        onVaultClick = backStack::navigateToVault,
    )

    searchEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
        onPlayVideo = { uri -> context.startPlayback(uri) },
        onFolderClick = backStack::navigateToMediaPickerScreen,
    )

    watchHistoryEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
        onResumeWatching = { media, mediaKey, title ->
            context.startPlayback(media, mediaKey = mediaKey, title = title)
        },
    )

    vaultEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
        // Vault files are served through FileProvider, so read access must be granted at
        // playback time for both PlayerActivity and the (separate) PlayerService component.
        onPlayVideo = { uri -> context.startPlayback(uri, grantReadPermission = true) },
        onPlayVideos = { uris -> context.startPlayback(uris, grantReadPermission = true) },
    )
}

/**
 * Reopens something the watch history holds.
 *
 * A station is handed every line it is carried on, for the same reason as when it is picked from the
 * channel list: the line playback starts on may be dead, and moving on to the next one is only
 * possible if the rest came along.
 */
internal fun Context.startPlayback(
    media: PlayableMedia,
    mediaKey: String? = null,
    title: String? = null,
) {
    startPlayback(
        uri = media.uri,
        playlist = null,
        mediaKey = mediaKey,
        title = title,
        grantReadPermission = false,
        lines = media.lines,
    )
}

internal fun Context.startPlayback(
    uri: Uri,
    mediaKey: String? = null,
    title: String? = null,
    grantReadPermission: Boolean = false,
) {
    startPlayback(
        uri = uri,
        playlist = null,
        mediaKey = mediaKey,
        title = title,
        grantReadPermission = grantReadPermission,
    )
}

/**
 * Plays a broadcast, handing over every line it can be reached by to fall back on.
 *
 * The channel is named by its station rather than by the line playback happens to start on, so that
 * watching it again — on another line, or after a playlist was added that reorders them — goes on
 * adding to the one entry playback history already holds for it.
 */
internal fun Context.startPlayback(channel: LiveChannel) {
    startPlayback(
        uri = channel.url.toUri(),
        playlist = null,
        mediaKey = LiveMediaKey(channelKey(channel.name)).toString(),
        title = channel.name,
        grantReadPermission = false,
        lines = channel.urls,
    )
}

internal fun Context.startPlayback(
    uris: List<Uri>,
    startUri: Uri? = null,
    grantReadPermission: Boolean = false,
) {
    val uri = startUri?.takeIf(uris::contains) ?: uris.firstOrNull() ?: return
    startPlayback(
        uri = uri,
        playlist = uris,
        mediaKey = null,
        title = null,
        grantReadPermission = grantReadPermission,
    )
}

/** Plays a queue whose items each have a durable identity, starting at [startIndex]. */
internal fun Context.startPlayback(
    uris: List<Uri>,
    mediaKeys: List<String>,
    startIndex: Int,
) {
    val uri = uris.getOrNull(startIndex) ?: return
    startPlayback(
        uri = uri,
        playlist = uris,
        mediaKey = null,
        mediaKeys = mediaKeys,
        title = null,
        grantReadPermission = false,
    )
}

private fun Context.startPlayback(
    uri: Uri,
    playlist: List<Uri>?,
    mediaKey: String?,
    title: String?,
    grantReadPermission: Boolean,
    mediaKeys: List<String>? = null,
    lines: List<String>? = null,
) {
    if (grantReadPermission) {
        (playlist ?: listOf(uri)).forEach {
            grantUriPermission(packageName, it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    val intent = Intent(this, PlayerActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = uri
        playlist?.let { putParcelableArrayListExtra(PlayerApi.API_PLAYLIST, ArrayList(it)) }
        mediaKey?.let { putExtra(EXTRA_MEDIA_KEY, it) }
        mediaKeys?.let { putStringArrayListExtra(EXTRA_MEDIA_KEYS, ArrayList(it)) }
        lines?.takeIf { it.size > 1 }?.let { putStringArrayListExtra(EXTRA_LIVE_LINES, ArrayList(it)) }
        title?.let { putExtra(EXTRA_MEDIA_TITLE, it) }
        if (grantReadPermission) addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(intent)
}
