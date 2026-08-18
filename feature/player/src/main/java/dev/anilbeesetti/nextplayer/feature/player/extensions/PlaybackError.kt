package dev.anilbeesetti.nextplayer.feature.player.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat.getSystemService
import androidx.media3.common.PlaybackException
import dev.anilbeesetti.nextplayer.core.ui.R
import java.net.Inet6Address
import java.net.InetAddress

/**
 * A message explaining [this] failure in the user's terms, or null when the exception's own
 * message is the best available description.
 */
@StringRes
fun PlaybackException.explanationOrNull(context: Context, mediaUri: Uri?): Int? = explanationFor(
    host = mediaUri?.host,
    error = this,
    hasGlobalIpv6 = context.hasGlobalIpv6Address(),
)

@StringRes
internal fun explanationFor(host: String?, error: Throwable?, hasGlobalIpv6: Boolean): Int? {
    val hostNeedsIpv6 = host != null && host.isIpv6Literal()
    val ipv6IsOutOfReach = !hasGlobalIpv6 || error.reportsUnreachableNetwork()
    return R.string.error_ipv6_unreachable.takeIf { hostNeedsIpv6 && ipv6IsOutOfReach }
}

/** A host is an IPv6 literal when it contains a colon, which a DNS name never does. */
private fun String.isIpv6Literal(): Boolean = trim('[', ']').contains(':')

/**
 * Whether this device holds a globally routable IPv6 address.
 *
 * IPTV playlists commonly carry hosts such as `[2409:8087:8:21::18]`, which need one. How they fail
 * without it varies: a socket error when IPv6 is absent, but an HTTP status relayed from a local
 * proxy when a VPN blackholes IPv6, so the address is a steadier signal than the failure itself.
 */
private fun Context.hasGlobalIpv6Address(): Boolean {
    val connectivityManager = getSystemService(this, ConnectivityManager::class.java) ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val linkAddresses = connectivityManager.getLinkProperties(network)?.linkAddresses ?: return false
    return linkAddresses.any { it.address.isGlobalIpv6() }
}

internal fun InetAddress.isGlobalIpv6(): Boolean = when {
    this !is Inet6Address -> false
    isAnyLocalAddress || isLoopbackAddress || isMulticastAddress -> false
    isLinkLocalAddress || isSiteLocalAddress || isUniqueLocal() -> false
    else -> true
}

/** `fc00::/7`, the IPv6 private range, which [Inet6Address] itself does not recognise. */
private fun Inet6Address.isUniqueLocal(): Boolean = (address[0].toInt() and 0xFE) == 0xFC

private fun Throwable?.reportsUnreachableNetwork(): Boolean =
    generateSequence(this) { it.cause }.any { throwable ->
        val message = throwable.message ?: return@any false
        UNREACHABLE_MARKERS.any { message.contains(it, ignoreCase = true) }
    }

private val UNREACHABLE_MARKERS = listOf(
    "ENETUNREACH",
    "EHOSTUNREACH",
    "Network is unreachable",
    "No route to host",
)
