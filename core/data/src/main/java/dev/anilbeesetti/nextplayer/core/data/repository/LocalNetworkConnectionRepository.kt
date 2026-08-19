package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.security.CredentialCipher
import dev.anilbeesetti.nextplayer.core.database.dao.NetworkConnectionDao
import dev.anilbeesetti.nextplayer.core.database.entities.NetworkConnectionEntity
import dev.anilbeesetti.nextplayer.core.model.NetworkAuthentication
import dev.anilbeesetti.nextplayer.core.model.NetworkConnection
import dev.anilbeesetti.nextplayer.core.model.NetworkProtocol
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LocalNetworkConnectionRepository @Inject constructor(
    private val networkConnectionDao: NetworkConnectionDao,
    private val credentialCipher: CredentialCipher,
) : NetworkConnectionRepository {

    override fun getConnections(): Flow<List<NetworkConnection>> =
        networkConnectionDao.getAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun getConnection(id: Long): NetworkConnection? =
        networkConnectionDao.getById(id)?.toModel()

    override suspend fun upsert(connection: NetworkConnection): Long =
        networkConnectionDao.upsert(connection.toEntity())

    override suspend fun delete(id: Long) = networkConnectionDao.deleteById(id)

    private fun NetworkConnectionEntity.toModel(): NetworkConnection {
        val readPassword = credentialCipher.decrypt(password)
        val readPassphrase = credentialCipher.decrypt(privateKeyPassphrase)
        return NetworkConnection(
            id = id,
            name = name,
            protocol = runCatching { NetworkProtocol.valueOf(protocol) }.getOrDefault(NetworkProtocol.SMB),
            host = host,
            port = port,
            path = path,
            username = username,
            password = readPassword.orEmpty(),
            useHttps = useHttps,
            authentication = runCatching { NetworkAuthentication.valueOf(authentication) }
                .getOrDefault(NetworkAuthentication.PASSWORD),
            privateKeyFileName = privateKeyFileName,
            privateKeyPassphrase = readPassphrase.orEmpty(),
            hostKeyFingerprint = hostKeyFingerprint,
            credentialsUnreadable = readPassword == null || readPassphrase == null,
        )
    }

    private fun NetworkConnection.toEntity() = NetworkConnectionEntity(
        id = id,
        name = name,
        protocol = protocol.name,
        host = host,
        port = port,
        path = path,
        username = username,
        password = credentialCipher.encrypt(password),
        useHttps = useHttps,
        authentication = authentication.name,
        privateKeyFileName = privateKeyFileName,
        privateKeyPassphrase = credentialCipher.encrypt(privateKeyPassphrase),
        hostKeyFingerprint = hostKeyFingerprint,
    )
}
