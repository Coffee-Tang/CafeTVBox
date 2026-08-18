package dev.anilbeesetti.nextplayer.feature.player.extensions

import dev.anilbeesetti.nextplayer.core.ui.R
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorTest {

    private val ipv6Host = "2409:8087:8:21::18"
    private val hostname = "live.example.com"
    private val genericFailure = IOException("Source error")

    @Test
    fun `ipv6 stream without ipv6 connectivity is explained whatever the failure`() {
        // A VPN that blackholes IPv6 answers through its local proxy, so the status says nothing
        // about IPv6; the missing address is what actually explains the failure.
        val proxyFailure = IOException("Response code: 502")

        assertEquals(
            R.string.error_ipv6_unreachable,
            explanationFor(ipv6Host, proxyFailure, hasGlobalIpv6 = false),
        )
    }

    @Test
    fun `bracketed host form is recognised`() {
        assertEquals(
            R.string.error_ipv6_unreachable,
            explanationFor("[$ipv6Host]", genericFailure, hasGlobalIpv6 = false),
        )
    }

    @Test
    fun `routing failure is explained even when the device does have ipv6`() {
        val error = ConnectException("connect failed: ENETUNREACH (Network is unreachable)")

        assertEquals(
            R.string.error_ipv6_unreachable,
            explanationFor(ipv6Host, error, hasGlobalIpv6 = true),
        )
    }

    @Test
    fun `routing failure is found through the cause chain`() {
        val error = IOException("Unable to connect", ConnectException("No route to host"))

        assertEquals(
            R.string.error_ipv6_unreachable,
            explanationFor(ipv6Host, error, hasGlobalIpv6 = true),
        )
    }

    @Test
    fun `ipv6 stream failing for its own reasons keeps the original message`() {
        val error = IOException("Response code: 404")

        assertNull(explanationFor(ipv6Host, error, hasGlobalIpv6 = true))
    }

    @Test
    fun `hostname streams are never blamed on ipv6`() {
        assertNull(explanationFor(hostname, genericFailure, hasGlobalIpv6 = false))
        assertNull(explanationFor(hostname, ConnectException("ENETUNREACH"), hasGlobalIpv6 = false))
    }

    @Test
    fun `missing host yields no explanation`() {
        assertNull(explanationFor(null, genericFailure, hasGlobalIpv6 = false))
    }

    @Test
    fun `only globally routable ipv6 addresses count as connectivity`() {
        assertTrue(InetAddress.getByName("2409:8087:8:21::18").isGlobalIpv6())

        assertFalse(InetAddress.getByName("fde5:e92:2b8e:48e9::1").isGlobalIpv6()) // unique local
        assertFalse(InetAddress.getByName("fe80::fea8:2f3d:3a00:d2a4").isGlobalIpv6()) // link local
        assertFalse(InetAddress.getByName("::1").isGlobalIpv6()) // loopback
        assertFalse(InetAddress.getByName("192.168.144.113").isGlobalIpv6()) // not IPv6 at all
    }
}
