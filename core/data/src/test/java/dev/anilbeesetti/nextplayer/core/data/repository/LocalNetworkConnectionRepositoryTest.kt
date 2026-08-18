package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.security.CredentialCipher
import dev.anilbeesetti.nextplayer.core.database.dao.NetworkConnectionDao
import dev.anilbeesetti.nextplayer.core.database.entities.NetworkConnectionEntity
import dev.anilbeesetti.nextplayer.core.model.NetworkAuthentication
import dev.anilbeesetti.nextplayer.core.model.NetworkConnection
import dev.anilbeesetti.nextplayer.core.model.NetworkProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkConnectionRepositoryTest {
    private val dao = FakeNetworkConnectionDao()
    private val cipher = FakeCredentialCipher()
    private val repository = LocalNetworkConnectionRepository(dao, cipher)

    @Test
    fun `upsert and get connection preserves authentication settings`() = runTest {
        val connection = NetworkConnection(
            name = "SFTP",
            protocol = NetworkProtocol.SFTP,
            host = "10.0.2.2",
            username = "alice",
            authentication = NetworkAuthentication.SSH_KEY,
            privateKeyFileName = "123.key",
            privateKeyPassphrase = "passphrase",
            hostKeyFingerprint = "SHA256:abc",
        )

        assertEquals(1L, repository.upsert(connection))
        assertEquals(connection.copy(id = 1), repository.getConnection(1))
    }

    @Test
    fun `unknown stored authentication falls back to password`() = runTest {
        dao.seed(
            NetworkConnectionEntity(
                id = 7,
                name = "Legacy",
                protocol = NetworkProtocol.SFTP.name,
                host = "10.0.2.2",
                port = 22,
                authentication = "UNKNOWN",
            ),
        )

        assertEquals(
            NetworkAuthentication.PASSWORD,
            repository.getConnection(7)?.authentication,
        )
    }

    @Test
    fun `upsert encrypts sensitive fields before persisting`() = runTest {
        val connection = NetworkConnection(
            name = "SMB",
            protocol = NetworkProtocol.SMB,
            host = "10.0.2.2",
            username = "alice",
            password = "s3cret",
            authentication = NetworkAuthentication.PASSWORD,
        )

        val id = repository.upsert(connection)

        val stored = dao.getById(id)!!
        assertNotEquals("s3cret", stored.password)
        assertTrue(stored.password.startsWith(FakeCredentialCipher.PREFIX))
        assertEquals("s3cret", repository.getConnection(id)?.password)
    }

    @Test
    fun `passphrase is encrypted at rest and decrypted on read`() = runTest {
        val connection = NetworkConnection(
            name = "SFTP",
            protocol = NetworkProtocol.SFTP,
            host = "10.0.2.2",
            username = "alice",
            authentication = NetworkAuthentication.SSH_KEY,
            privateKeyFileName = "id.key",
            privateKeyPassphrase = "passphrase",
        )

        val id = repository.upsert(connection)

        val stored = dao.getById(id)!!
        assertNotEquals("passphrase", stored.privateKeyPassphrase)
        assertEquals("passphrase", repository.getConnection(id)?.privateKeyPassphrase)
    }

    @Test
    fun `legacy plain text password is read unchanged`() = runTest {
        dao.seed(
            NetworkConnectionEntity(
                id = 5,
                name = "Legacy",
                protocol = NetworkProtocol.SMB.name,
                host = "10.0.2.2",
                port = null,
                username = "bob",
                password = "legacyPlain",
            ),
        )

        assertEquals("legacyPlain", repository.getConnection(5)?.password)
    }

    @Test
    fun `empty password stays empty`() = runTest {
        val id = repository.upsert(
            NetworkConnection(
                name = "Guest",
                protocol = NetworkProtocol.SMB,
                host = "10.0.2.2",
            ),
        )

        assertEquals("", dao.getById(id)?.password)
        assertEquals("", repository.getConnection(id)?.password)
    }

    /**
     * Mirrors the production cipher's contract without the Android Keystore: empty values pass
     * through, encrypted values are prefixed, and legacy (unprefixed) values decrypt unchanged.
     */
    private class FakeCredentialCipher : CredentialCipher {
        override fun encrypt(plainText: String): String =
            if (plainText.isEmpty()) plainText else PREFIX + plainText

        override fun decrypt(storedValue: String): String =
            if (storedValue.startsWith(PREFIX)) storedValue.removePrefix(PREFIX) else storedValue

        companion object {
            const val PREFIX = "enc:test:"
        }
    }

    private class FakeNetworkConnectionDao : NetworkConnectionDao {
        private val connections = MutableStateFlow<List<NetworkConnectionEntity>>(emptyList())

        fun seed(connection: NetworkConnectionEntity) {
            connections.value = connections.value + connection
        }

        override suspend fun upsert(connection: NetworkConnectionEntity): Long {
            val id = connection.id.takeIf { it != 0L } ?: 1L
            val savedConnection = connection.copy(id = id)
            connections.value = connections.value.filterNot { it.id == id } + savedConnection
            return id
        }

        override fun getAll(): Flow<List<NetworkConnectionEntity>> = connections

        override suspend fun getById(id: Long): NetworkConnectionEntity? =
            connections.value.firstOrNull { it.id == id }

        override suspend fun deleteById(id: Long) {
            connections.value = connections.value.filterNot { it.id == id }
        }
    }
}
