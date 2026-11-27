package ir.miladmedia.fdplayer.domain.model

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadableAudio(
    val id: String,
    val url: String,
    val fileName: String,
    val title: String,
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val errorMessage: String? = null
)
