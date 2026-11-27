package ir.miladmedia.fdplayer.data.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.domain.model.Track
import ir.miladmedia.fdplayer.domain.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioRepository {

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())

    override fun observeTracks(): Flow<List<Track>> = _tracks.asStateFlow()

    override suspend fun refreshTracks(): List<Track> = withContext(Dispatchers.IO) {
        if (!hasAudioPermission()) {
            _tracks.value = emptyList()
            return@withContext emptyList()
        }

        val result = mutableListOf<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val unknownArtistLabel = context.getString(R.string.unknown_artist)

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                val rawArtist = cursor.getString(artistCol)?.trim().orEmpty()
                val duration = cursor.getLong(durationCol)
                val displayName = cursor.getString(displayNameCol)?.trim().orEmpty()
                val albumId = cursor.getLong(albumIdCol)
                val albumName = if (albumCol >= 0) {
                    cursor.getString(albumCol)?.trim().orEmpty()
                } else {
                    ""
                }

                // If title is blank, use filename without extension
                val resolvedTitle = rawTitle.ifBlank {
                    displayName.substringBeforeLast('.').ifBlank { "Track $id" }
                }
                val resolvedArtist = if (rawArtist.isBlank() || rawArtist.equals("<unknown>", ignoreCase = true)) {
                    unknownArtistLabel
                } else {
                    rawArtist
                }

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()

                val albumArtUri = if (albumId > 0L) {
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()
                } else {
                    null
                }

                result.add(
                    Track(
                        id = id,
                        title = resolvedTitle,
                        artist = resolvedArtist,
                        album = albumName.ifBlank { resolvedTitle },
                        albumId = albumId,
                        durationMs = duration,
                        contentUri = contentUri,
                        albumArtUri = albumArtUri,
                        displayName = displayName
                    )
                )
            }
        }

        _tracks.value = result
        result
    }

    override suspend fun getTracksByIds(ids: List<Long>): List<Track> {
        val current = _tracks.value.ifEmpty { refreshTracks() }
        val byId = current.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}
