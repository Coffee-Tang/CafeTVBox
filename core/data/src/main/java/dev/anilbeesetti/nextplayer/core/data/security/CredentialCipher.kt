package dev.anilbeesetti.nextplayer.core.data.security

/**
 * Encrypts and decrypts sensitive credential strings (e.g. network passwords) so they are never
 * persisted in plain text.
 *
 * Implementations must treat legacy plain-text values gracefully: [decrypt] returns any value that
 * is not recognised as ciphertext unchanged, so existing connections keep working after upgrade.
 */
interface CredentialCipher {

    /**
     * Returns an encrypted, storable representation of [plainText]. An empty input is returned
     * unchanged so that "no credential" stays empty.
     */
    fun encrypt(plainText: String): String

    /**
     * Returns the plain-text value for [storedValue], or `null` when [storedValue] is ciphertext
     * this device can no longer read. Values that are not recognised as ciphertext (legacy plain
     * text) are returned unchanged.
     *
     * An unreadable credential is gone for good: the key that could open it no longer exists, so
     * only the person who knows the credential can restore it. Callers must tell that apart from
     * "no credential was saved", or they will authenticate with an empty one and blame the server
     * for refusing them.
     */
    fun decrypt(storedValue: String): String?
}
