package dev.anilbeesetti.nextplayer.feature.live.screens.channels

import dev.anilbeesetti.nextplayer.core.model.LiveChannel

/** Groups channels by `group-title`, keeping the order in which groups appear in the playlist. */
internal fun playlistGroups(channels: List<LiveChannel>): List<LiveChannelGroup> =
    channels.groupBy { it.group }
        .map { (group, grouped) -> LiveChannelGroup(name = group, channels = grouped) }
