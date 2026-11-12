package ir.miladmedia.fdplayer.ui.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import ir.miladmedia.fdplayer.domain.model.Track

/** UI-side mirror of the service's conversion; keeps media ids equal to track ids. */
fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(Uri.parse(uri))
    .setMediaId(id.toString())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .build()
    )
    .build()
