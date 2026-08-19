package dev.anilbeesetti.nextplayer.navigation

import android.content.Context
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.anilbeesetti.nextplayer.feature.live.navigation.addLiveSourceEntry
import dev.anilbeesetti.nextplayer.feature.live.navigation.liveChannelsEntry
import dev.anilbeesetti.nextplayer.feature.live.navigation.liveEntry
import dev.anilbeesetti.nextplayer.feature.live.navigation.navigateToAddLiveSource
import dev.anilbeesetti.nextplayer.feature.live.navigation.navigateToLiveChannels
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToSettings

fun EntryProviderScope<NavKey>.liveNavGraph(
    context: Context,
    backStack: NavBackStack<NavKey>,
) {
    liveEntry(
        onAddSource = { backStack.navigateToAddLiveSource() },
        onEditSource = { id -> backStack.navigateToAddLiveSource(id) },
        onOpenSource = { id -> backStack.navigateToLiveChannels(id) },
        onSettingsClick = backStack::navigateToSettings,
    )

    addLiveSourceEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )

    liveChannelsEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
        onPlayChannel = { channel -> context.startPlayback(channel) },
    )
}
