package dev.anilbeesetti.nextplayer.core.data.network

import java.io.InputStream
import java.io.PushbackInputStream
import java.util.zip.GZIPInputStream

/**
 * Unpacks a body that turns out to be gzipped, and passes anything else through untouched.
 *
 * A programme guide is a tenth of its size gzipped, which over a home connection is the difference
 * between arriving and timing out, so guides are published compressed. Deciding by what the body
 * starts with rather than by its address leaves an address free to point at either form.
 */
internal fun InputStream.unpacked(): InputStream {
    val head = PushbackInputStream(this, GZIP_MAGIC.size)
    val start = ByteArray(GZIP_MAGIC.size)
    val read = head.readNBytes(start, 0, start.size)
    head.unread(start, 0, read)
    return if (read == start.size && start.contentEquals(GZIP_MAGIC)) GZIPInputStream(head) else head
}

private val GZIP_MAGIC = byteArrayOf(0x1f, 0x8b.toByte())
