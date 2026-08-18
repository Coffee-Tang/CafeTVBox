package dev.anilbeesetti.nextplayer.core.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CredentialCipher] backed by a non-exportable AES-256-GCM key stored in the Android Keystore.
 *
 * The key does not require user authentication, so credentials can be decrypted automatically for
 * unattended reconnects (e.g. on boot). Ciphertext is stored as `enc:v1:<base64(iv + cipherBytes)>`
 * which lets [decrypt] distinguish encrypted values from legacy plain text.
 */
@Singleton
class KeystoreCredentialCipher @Inject constructor() : CredentialCipher {

    override fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return plainText
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + encrypted
        return PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    override fun decrypt(storedValue: String): String {
        if (!storedValue.startsWith(PREFIX)) return storedValue
        return runCatching {
            val combined = Base64.decode(storedValue.removePrefix(PREFIX), Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, IV_LENGTH)
            val encrypted = combined.copyOfRange(IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            }
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "nextplayer_network_credentials"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFIX = "enc:v1:"
        private const val IV_LENGTH = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_SIZE_BITS = 256
    }
}
