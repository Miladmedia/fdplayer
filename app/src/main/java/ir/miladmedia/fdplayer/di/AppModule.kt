package ir.miladmedia.fdplayer.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.miladmedia.fdplayer.data.local.FDPlayerDatabase
import ir.miladmedia.fdplayer.data.local.dao.BluetoothRuleDao
import ir.miladmedia.fdplayer.data.local.dao.PlaylistDao
import ir.miladmedia.fdplayer.data.repository.AudioRepositoryImpl
import ir.miladmedia.fdplayer.data.repository.BluetoothRuleRepositoryImpl
import ir.miladmedia.fdplayer.data.repository.DownloadRepositoryImpl
import ir.miladmedia.fdplayer.data.repository.PlaylistRepositoryImpl
import ir.miladmedia.fdplayer.data.repository.SettingsRepositoryImpl
import ir.miladmedia.fdplayer.domain.repository.AudioRepository
import ir.miladmedia.fdplayer.domain.repository.BluetoothRuleRepository
import ir.miladmedia.fdplayer.domain.repository.DownloadRepository
import ir.miladmedia.fdplayer.domain.repository.PlaybackController
import ir.miladmedia.fdplayer.domain.repository.PlaylistRepository
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import ir.miladmedia.fdplayer.service.ExoPlaybackManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseAndNetworkModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FDPlayerDatabase {
        return Room.databaseBuilder(
            context,
            FDPlayerDatabase::class.java,
            "fd_player.db"
        ).build()
    }

    @Provides
    fun providePlaylistDao(db: FDPlayerDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideBluetoothRuleDao(db: FDPlayerDatabase): BluetoothRuleDao = db.bluetoothRuleDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds
    @Singleton
    abstract fun bindAudioRepository(impl: AudioRepositoryImpl): AudioRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindBluetoothRuleRepository(impl: BluetoothRuleRepositoryImpl): BluetoothRuleRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackController(impl: ExoPlaybackManager): PlaybackController
}
