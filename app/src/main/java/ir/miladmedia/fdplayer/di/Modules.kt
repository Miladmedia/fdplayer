package ir.miladmedia.fdplayer.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.miladmedia.fdplayer.data.local.AppDatabase
import ir.miladmedia.fdplayer.data.prefs.PlaybackStateRepositoryImpl
import ir.miladmedia.fdplayer.data.prefs.SettingsRepositoryImpl
import ir.miladmedia.fdplayer.data.remote.SourceGatewayImpl
import ir.miladmedia.fdplayer.data.repository.BluetoothRepositoryImpl
import ir.miladmedia.fdplayer.data.repository.DownloadRepositoryImpl
import ir.miladmedia.fdplayer.data.repository.LibraryRepositoryImpl
import ir.miladmedia.fdplayer.data.repository.PlaylistRepositoryImpl
import ir.miladmedia.fdplayer.data.repository.SourceRepositoryImpl
import ir.miladmedia.fdplayer.domain.repository.BluetoothRepository
import ir.miladmedia.fdplayer.domain.repository.DownloadRepository
import ir.miladmedia.fdplayer.domain.repository.LibraryRepository
import ir.miladmedia.fdplayer.domain.repository.PlaybackStateRepository
import ir.miladmedia.fdplayer.domain.repository.PlaylistRepository
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import ir.miladmedia.fdplayer.domain.repository.SourceGateway
import ir.miladmedia.fdplayer.domain.repository.SourceRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun library(i: LibraryRepositoryImpl): LibraryRepository
    @Binds @Singleton abstract fun playlists(i: PlaylistRepositoryImpl): PlaylistRepository
    @Binds @Singleton abstract fun downloads(i: DownloadRepositoryImpl): DownloadRepository
    @Binds @Singleton abstract fun sources(i: SourceRepositoryImpl): SourceRepository
    @Binds @Singleton abstract fun bluetooth(i: BluetoothRepositoryImpl): BluetoothRepository
    @Binds @Singleton abstract fun settings(i: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun playback(i: PlaybackStateRepositoryImpl): PlaybackStateRepository
    @Binds @Singleton abstract fun gateway(i: SourceGatewayImpl): SourceGateway
}

@Module
@InstallIn(SingletonComponent::class)
object ProvidesModule {
    @Provides @Singleton
    fun database(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "fdplayer.db").build()

    @Provides fun trackDao(db: AppDatabase) = db.trackDao()
    @Provides fun playlistDao(db: AppDatabase) = db.playlistDao()
    @Provides fun downloadDao(db: AppDatabase) = db.downloadDao()
    @Provides fun sourceDao(db: AppDatabase) = db.sourceDao()
    @Provides fun bluetoothDao(db: AppDatabase) = db.bluetoothDao()

    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
}
