package ir.miladmedia.fdplayer.domain.model

data class Playlist(
    val id: Long = 0L,
    val name: String,
    val trackIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
