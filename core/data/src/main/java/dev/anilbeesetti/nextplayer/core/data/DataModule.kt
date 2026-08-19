package dev.anilbeesetti.nextplayer.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anilbeesetti.nextplayer.core.data.repository.EpgRepository
import dev.anilbeesetti.nextplayer.core.data.repository.HttpEpgRepository
import dev.anilbeesetti.nextplayer.core.data.repository.HttpLiveChannelRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveChannelRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LiveSourceRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalLiveSourceRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalMediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalNetworkConnectionRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalPreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalPlaylistRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalSearchHistoryRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalVaultPinRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalVaultRepository
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.NetworkConnectionRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PlaylistRepository
import dev.anilbeesetti.nextplayer.core.data.repository.SearchHistoryRepository
import dev.anilbeesetti.nextplayer.core.data.repository.VaultPinRepository
import dev.anilbeesetti.nextplayer.core.data.repository.VaultRepository
import dev.anilbeesetti.nextplayer.core.data.security.CredentialCipher
import dev.anilbeesetti.nextplayer.core.data.security.KeystoreCredentialCipher
import javax.inject.Singleton

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
}
