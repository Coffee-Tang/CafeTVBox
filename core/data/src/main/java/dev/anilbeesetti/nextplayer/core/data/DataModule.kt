package dev.anilbeesetti.nextplayer.core.data

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.anilbeesetti.nextplayer.core.data.catalogue.TmdbApiKey
import dev.anilbeesetti.nextplayer.core.data.repository.CatalogueRepository
import dev.anilbeesetti.nextplayer.core.data.repository.EpgRepository
import dev.anilbeesetti.nextplayer.core.data.repository.HttpEpgRepository
import dev.anilbeesetti.nextplayer.core.data.repository.HttpLiveChannelRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveChannelRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveSourceRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalCatalogueRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalLiveSourceRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalMediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalNetworkConnectionRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalPlaylistRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalPreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalSearchHistoryRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalVaultPinRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalVaultRepository
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.NetworkConnectionRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PlaylistRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.repository.SearchHistoryRepository
import dev.anilbeesetti.nextplayer.core.data.repository.VaultPinRepository
import dev.anilbeesetti.nextplayer.core.data.repository.VaultRepository
import dev.anilbeesetti.nextplayer.core.data.security.CredentialCipher
import dev.anilbeesetti.nextplayer.core.data.security.KeystoreCredentialCipher
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * The directory the playlists read from live sources are kept in between runs.
 *
 * Kept among the app's files rather than its cache: what they are for is a channel list that appears
 * the moment the Live tab is opened, and a copy the system may throw out at any time would make that
 * hold true only some of the time.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaylistStore

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    @Singleton
    fun bindsPlaylistRepository(
        playlistRepository: LocalPlaylistRepository,
    ): PlaylistRepository

    @Binds
    fun bindsMediaRepository(
        videoRepository: LocalMediaRepository,
    ): MediaRepository

    @Binds
    @Singleton
    fun bindsPreferencesRepository(
        preferencesRepository: LocalPreferencesRepository,
    ): PreferencesRepository

    @Binds
    @Singleton
    fun bindsSearchHistoryRepository(
        searchHistoryRepository: LocalSearchHistoryRepository,
    ): SearchHistoryRepository

    @Binds
    @Singleton
    fun bindsVaultRepository(
        vaultRepository: LocalVaultRepository,
    ): VaultRepository

    @Binds
    @Singleton
    fun bindsVaultPinRepository(
        vaultPinRepository: LocalVaultPinRepository,
    ): VaultPinRepository

    @Binds
    @Singleton
    fun bindsNetworkConnectionRepository(
        networkConnectionRepository: LocalNetworkConnectionRepository,
    ): NetworkConnectionRepository

    @Binds
    @Singleton
    fun bindsCredentialCipher(
        credentialCipher: KeystoreCredentialCipher,
    ): CredentialCipher

    @Binds
    @Singleton
    fun bindsLiveSourceRepository(
        liveSourceRepository: LocalLiveSourceRepository,
    ): LiveSourceRepository

    @Binds
    @Singleton
    fun bindsLiveChannelRepository(
        liveChannelRepository: HttpLiveChannelRepository,
    ): LiveChannelRepository

    @Binds
    @Singleton
    fun bindsEpgRepository(
        epgRepository: HttpEpgRepository,
    ): EpgRepository

    @Binds
    @Singleton
    fun bindsCatalogueRepository(
        catalogueRepository: LocalCatalogueRepository,
    ): CatalogueRepository

    companion object {

        @Provides
        @Singleton
        @PlaylistStore
        fun providesPlaylistStore(@ApplicationContext context: Context): File =
            File(context.filesDir, "playlists")

        /**
         * The catalogue key this build was given, which is nothing at all in a build whose
         * `local.properties` named none. Searching then fails saying so, rather than asking TMDB a
         * question it can only refuse.
         */
        @Provides
        @Singleton
        fun providesTmdbApiKey(): TmdbApiKey = TmdbApiKey { BuildConfig.TMDB_API_KEY.ifEmpty { null } }
    }
}
