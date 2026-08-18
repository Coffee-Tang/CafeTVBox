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
     * Returns the plain-text value for [storedValue]. Values that are not recognised as ciphertext
     * (legacy plain text) are returned unchanged.
     */
    fun decrypt(storedValue: String): String
}
