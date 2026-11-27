package ir.miladmedia.fdplayer.domain.model

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String?,
    val displayName: String
)
