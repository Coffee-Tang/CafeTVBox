package dev.anilbeesetti.nextplayer.feature.network.screens.browse

import dev.anilbeesetti.nextplayer.core.data.repository.CatalogueRepository
import dev.anilbeesetti.nextplayer.core.data.repository.NetworkConnectionRepository
import dev.anilbeesetti.nextplayer.core.media.network.CredentialsRejected
import dev.anilbeesetti.nextplayer.core.media.network.NetworkClient
import dev.anilbeesetti.nextplayer.core.media.network.NetworkClientFactory
import dev.anilbeesetti.nextplayer.core.media.network.proxy.NetworkStreamingProxy
import dev.anilbeesetti.nextplayer.core.media.network.sftp.HostKeyMismatch
import dev.anilbeesetti.nextplayer.core.model.LibraryWork
import dev.anilbeesetti.nextplayer.core.model.NetworkConnection
import dev.anilbeesetti.nextplayer.core.model.NetworkFile
import dev.anilbeesetti.nextplayer.core.model.NetworkProtocol
import dev.anilbeesetti.nextplayer.core.model.WorkDetail
import dev.anilbeesetti.nextplayer.core.model.WorkKind
import dev.anilbeesetti.nextplayer.feature.network.MainDispatcherRule
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkBrowseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `wrapped host key mismatch retains trusted and presented fingerprints`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val mismatch = HostKeyMismatch(
                expectedFingerprint = "SHA256:trusted",
                presentedFingerprint = "SHA256:presented",
            )
            val viewModel = viewModel(
                connectResult = Result.failure(IllegalStateException("SSH failed", mismatch)),
            )

            advanceUntilIdle()

            assertEquals(
                NetworkBrowseError(
                    message = "SSH failed",
                    hostKeyMismatch = NetworkBrowseHostKeyMismatch(
                        trustedFingerprint = "SHA256:trusted",
                        presentedFingerprint = "SHA256:presented",
                    ),
                ),
                viewModel.uiState.value.error,
            )
        }

    @Test
    fun `ordinary connection error keeps its message without fingerprint details`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel(
                connectResult = Result.failure(IllegalStateException("Server unavailable")),
            )

            advanceUntilIdle()

            assertEquals("Server unavailable", viewModel.uiState.value.error?.message)
            assertNull(viewModel.uiState.value.error?.hostKeyMismatch)
            assertNull(viewModel.uiState.value.error?.credentialProblem)
        }

    @Test
    fun `a credential the server refuses is not offered as something to retry`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val refused = CredentialsRejected(IllegalStateException("STATUS_LOGON_FAILURE"))
            val viewModel = viewModel(connectResult = Result.failure(IOException("Connect failed", refused)))

            advanceUntilIdle()

            assertEquals(CredentialProblem.Rejected, viewModel.uiState.value.error?.credentialProblem)
        }

    @Test
    fun `a credential this device can no longer read is reported without asking the server`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val client = FakeNetworkClient(Result.success(Unit), files = emptyList())
            val factory = NetworkClientFactory { client }
            val viewModel = NetworkBrowseViewModel(
                connectionId = 7,
                path = null,
                repository = FakeRepository(connection().copy(credentialsUnreadable = true)),
                streamingProxy = NetworkStreamingProxy(factory),
                clientFactory = factory,
                catalogueRepository = FakeCatalogueRepository(),
            )

            advanceUntilIdle()

            assertEquals(CredentialProblem.Unreadable, viewModel.uiState.value.error?.credentialProblem)
            assertEquals(0, client.connectAttempts)
        }

    @Test
    fun `listing hides apple double sidecars hidden entries and nas folders`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel(
                connectResult = Result.success(Unit),
                files = listOf(
                    file("Silicon.Valley.S02E01.mkv"),
                    file("._Silicon.Valley.S02E01.mkv"),
                    file(".hidden.mkv"),
                    file("cover.jpg"),
                    directory("Season 2"),
                    directory("@eaDir"),
                    directory(".Trashes"),
                ),
            )

            advanceUntilIdle()

            assertEquals(
                listOf("Season 2", "Silicon.Valley.S02E01.mkv"),
                viewModel.uiState.value.files.map { it.name },
            )
        }

    private fun file(name: String) = NetworkFile(name = name, path = "/$name", isDirectory = false)

    private fun directory(name: String) = NetworkFile(name = name, path = "/$name", isDirectory = true)

    private fun viewModel(
        connectResult: Result<Unit>,
        files: List<NetworkFile>? = null,
    ): NetworkBrowseViewModel {
        val client = FakeNetworkClient(connectResult, files)
        val factory = NetworkClientFactory { client }
        return NetworkBrowseViewModel(
            connectionId = 7,
            path = null,
            repository = FakeRepository(connection()),
            streamingProxy = NetworkStreamingProxy(factory),
            clientFactory = factory,
            catalogueRepository = FakeCatalogueRepository(),
        )
    }

    private fun connection() = NetworkConnection(
        id = 7,
        name = "Media server",
        protocol = NetworkProtocol.SFTP,
        host = "sftp.example",
        username = "media",
        hostKeyFingerprint = "SHA256:trusted",
    )
}

private class FakeRepository(
    private val connection: NetworkConnection,
) : NetworkConnectionRepository {
    override fun getConnections(): Flow<List<NetworkConnection>> = flowOf(listOf(connection))

    override suspend fun getConnection(id: Long): NetworkConnection = connection

    override suspend fun upsert(connection: NetworkConnection): Long = error("Not used")

    override suspend fun delete(id: Long) = error("Not used")
}

private class FakeNetworkClient(
    private val connectResult: Result<Unit>,
    private val files: List<NetworkFile>? = null,
) : NetworkClient {
    var connectAttempts: Int = 0
        private set

    override val rootPath: String = "/"

    override suspend fun connect(): Result<Unit> {
        connectAttempts++
        return connectResult
    }

    override suspend fun disconnect() = Unit

    override fun isConnected(): Boolean = false

    override suspend fun listFiles(path: String): Result<List<NetworkFile>> =
        files?.let { Result.success(it) } ?: error("Not used")

    override suspend fun fileSize(path: String): Long = error("Not used")

    override suspend fun openStream(path: String, offset: Long): InputStream = error("Not used")
}

private class FakeCatalogueRepository : CatalogueRepository {
    override fun observeLibrariesExist(): Flow<Boolean> = flowOf(false)
    override fun observeWorks(): Flow<List<LibraryWork>> = flowOf(emptyList())
    override fun observeWork(workId: Long): Flow<WorkDetail?> = flowOf(null)
    override suspend fun addLibraryAndScan(
        name: String,
        root: String,
        kind: WorkKind,
        connectionId: Long,
    ): Result<Unit> = Result.success(Unit)
    override suspend fun reconcile() = Unit
    override suspend fun keyToResume(workId: Long): String? = null
    override suspend fun playableKey(itemId: Long): String? = null
}
