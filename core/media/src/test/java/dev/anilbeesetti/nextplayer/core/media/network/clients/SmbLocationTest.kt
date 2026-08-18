package dev.anilbeesetti.nextplayer.core.media.network.clients

import org.junit.Assert.assertEquals
import org.junit.Test

class SmbLocationTest {

    @Test
    fun `a pinned share keeps browse paths relative to it`() {
        assertEquals(
            SmbLocation(share = "Media", path = ""),
            smbLocationOf(configuredShare = "Media", browsePath = ""),
        )
        assertEquals(
            SmbLocation(share = "Media", path = "Movies/Alien.mkv"),
            smbLocationOf(configuredShare = "Media", browsePath = "Movies/Alien.mkv"),
        )
    }

    @Test
    fun `without a pinned share an empty path means list the shares`() {
        assertEquals(
            SmbLocation(share = "", path = ""),
            smbLocationOf(configuredShare = "", browsePath = ""),
        )
    }

    @Test
    fun `without a pinned share the leading segment names the share`() {
        assertEquals(
            SmbLocation(share = "Media", path = ""),
            smbLocationOf(configuredShare = "", browsePath = "Media"),
        )
        assertEquals(
            SmbLocation(share = "Media", path = "Movies"),
            smbLocationOf(configuredShare = "", browsePath = "Media/Movies"),
        )
        assertEquals(
            SmbLocation(share = "Media", path = "Movies/Alien.mkv"),
            smbLocationOf(configuredShare = "", browsePath = "Media/Movies/Alien.mkv"),
        )
    }

    @Test
    fun `surrounding slashes are ignored`() {
        assertEquals(
            SmbLocation(share = "Media", path = "Movies"),
            smbLocationOf(configuredShare = "", browsePath = "/Media/Movies/"),
        )
        assertEquals(
            SmbLocation(share = "Media", path = "Movies"),
            smbLocationOf(configuredShare = "Media", browsePath = "/Movies/"),
        )
    }
}
