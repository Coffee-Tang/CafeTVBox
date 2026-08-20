package dev.anilbeesetti.nextplayer.feature.videopicker.composables

enum class HomeRemoteFocusTarget {
    CONTINUE_WATCHING,
    RECENT_LIVE,
    WORKS,
    EMPTY_CTA,
}

fun homeRemoteFocusTarget(
    showContinueWatching: Boolean,
    showRecentLive: Boolean,
    hasWorks: Boolean,
    isEmptyLibrary: Boolean,
): HomeRemoteFocusTarget? = when {
    showContinueWatching -> HomeRemoteFocusTarget.CONTINUE_WATCHING
    showRecentLive -> HomeRemoteFocusTarget.RECENT_LIVE
    hasWorks -> HomeRemoteFocusTarget.WORKS
    isEmptyLibrary -> HomeRemoteFocusTarget.EMPTY_CTA
    else -> null
}
