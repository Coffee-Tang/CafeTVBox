package dev.anilbeesetti.nextplayer.feature.player.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveLinesTest {

    private val lines = listOf("http://a/1.m3u8", "http://b/1.m3u8", "http://c/1.m3u8")

    @Test
    fun `a channel opens on the line that last came through`() {
        assertEquals(
            listOf("http://c/1.m3u8", "http://a/1.m3u8", "http://b/1.m3u8"),
            linesStartingWith(lines, lastPlayed = "http://c/1.m3u8"),
        )
    }

    @Test
    fun `the lines behind the remembered one keep the order their playlists gave them`() {
        assertEquals(
            listOf("http://b/1.m3u8", "http://a/1.m3u8", "http://c/1.m3u8"),
            linesStartingWith(lines, lastPlayed = "http://b/1.m3u8"),
        )
    }

    @Test
    fun `a line no playlist carries any more is passed over`() {
        assertEquals(lines, linesStartingWith(lines, lastPlayed = "http://gone/1.m3u8"))
    }

    @Test
    fun `a channel never watched opens on the first line its playlists offer`() {
        assertEquals(lines, linesStartingWith(lines, lastPlayed = null))
    }

    @Test
    fun `falling back moves onto the line after the one in use`() {
        assertEquals(LineInUse(number = 2), LineInUse(number = 1).next(lineCount = 3))
    }

    @Test
    fun `falling back off the last line leaves nowhere to go`() {
        assertNull(LineInUse(number = 3).next(lineCount = 3))
    }

    @Test
    fun `a line can be asked for by name`() {
        assertEquals(
            LineInUse(number = 3, wasChosenByHand = true),
            LineInUse(number = 1).chosen(number = 3, lineCount = 3),
        )
    }

    @Test
    fun `a line the channel does not offer cannot be asked for`() {
        assertNull(LineInUse(number = 1).chosen(number = 4, lineCount = 3))
        assertNull(LineInUse(number = 1).chosen(number = 0, lineCount = 3))
    }

    @Test
    fun `asking for the line the player just moved to by itself still counts as choosing it`() {
        assertEquals(
            LineInUse(number = 2, wasChosenByHand = true),
            LineInUse(number = 2).chosen(number = 2, lineCount = 3),
        )
    }

    @Test
    fun `running out of lines the player walked itself means the channel could not be reached`() {
        assertTrue(LineInUse(number = 1).mayReportEveryLineUnreachable)
        assertTrue(LineInUse(number = 1).next(lineCount = 3)!!.mayReportEveryLineUnreachable)
    }

    @Test
    fun `a line is waited for only while it owes a picture`() {
        assertEquals(
            LineReport.Playing(line = 2),
            LineReport.of(line = 2, isPlaying = true, playWhenReady = true),
        )
        assertEquals(
            LineReport.AwaitingPicture(line = 2),
            LineReport.of(line = 2, isPlaying = false, playWhenReady = true),
        )
        assertEquals(
            LineReport.Stopped,
            LineReport.of(line = 2, isPlaying = false, playWhenReady = false),
        )
    }

    @Test
    fun `moving between lines that both come through is a change of its own`() {
        assertNotEquals(
            LineReport.of(line = 1, isPlaying = true, playWhenReady = true),
            LineReport.of(line = 2, isPlaying = true, playWhenReady = true),
        )
    }

    @Test
    fun `running out of lines after the viewer chose one says nothing about the channel`() {
        val chosen = LineInUse(number = 2).chosen(number = 3, lineCount = 3)!!

        assertFalse(chosen.mayReportEveryLineUnreachable)
        assertFalse(LineInUse(number = 1).chosen(number = 2, lineCount = 3)!!.next(lineCount = 3)!!.mayReportEveryLineUnreachable)
    }
}
