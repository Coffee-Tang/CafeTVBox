package dev.anilbeesetti.nextplayer.core.media.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkMediaUtilsTest {

    @Test
    fun `video files are browsable`() {
        assertTrue(isNetworkBrowsableEntry("Silicon.Valley.S02E01.mkv", isDirectory = false))
        assertTrue(isNetworkBrowsableEntry("movie.MP4", isDirectory = false))
        assertTrue(isNetworkBrowsableEntry("clip.ts", isDirectory = false))
    }

    @Test
    fun `non video files are hidden`() {
        assertFalse(isNetworkBrowsableEntry("notes.txt", isDirectory = false))
        assertFalse(isNetworkBrowsableEntry("cover.jpg", isDirectory = false))
        assertFalse(isNetworkBrowsableEntry("no-extension", isDirectory = false))
    }

    @Test
    fun `macOS AppleDouble sidecar is hidden despite its video extension`() {
        assertFalse(
            isNetworkBrowsableEntry("._Silicon.Valley.S02E01.mkv", isDirectory = false),
        )
    }

    @Test
    fun `hidden files and folders are skipped`() {
        assertFalse(isNetworkBrowsableEntry(".hidden.mkv", isDirectory = false))
        assertFalse(isNetworkBrowsableEntry(".DS_Store", isDirectory = false))
        assertFalse(isNetworkBrowsableEntry(".Trashes", isDirectory = true))
        assertFalse(isNetworkBrowsableEntry(".Spotlight-V100", isDirectory = true))
    }

    @Test
    fun `ordinary folders are browsable`() {
        assertTrue(isNetworkBrowsableEntry("Movies", isDirectory = true))
        assertTrue(isNetworkBrowsableEntry("公共", isDirectory = true))
        assertTrue(isNetworkBrowsableEntry("no-extension", isDirectory = true))
    }

    @Test
    fun `nas housekeeping folders are hidden regardless of case`() {
        assertFalse(isNetworkBrowsableEntry("@eaDir", isDirectory = true))
        assertFalse(isNetworkBrowsableEntry("#recycle", isDirectory = true))
        assertFalse(isNetworkBrowsableEntry("\$RECYCLE.BIN", isDirectory = true))
        assertFalse(isNetworkBrowsableEntry("System Volume Information", isDirectory = true))
        assertFalse(isNetworkBrowsableEntry("lost+found", isDirectory = true))
    }

    @Test
    fun `a video file named like a junk folder is still browsable`() {
        assertTrue(isNetworkBrowsableEntry("@eaDir.mkv", isDirectory = false))
    }
}
