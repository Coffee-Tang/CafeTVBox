package dev.anilbeesetti.nextplayer.core.data.catalogue

/**
 * Where the key that signs catalogue requests comes from.
 *
 * A key is a secret and so cannot be written into the sources: a build takes one out of
 * `local.properties`, which version control never sees. Asking for it here rather than reading the
 * built-in key where it is needed is what lets a key the viewer types in answer instead, without
 * anything that searches the catalogue knowing the difference.
 */
fun interface TmdbApiKey {

    /** The key to sign requests with, or null while nobody has supplied one. */
    fun value(): String?
}
