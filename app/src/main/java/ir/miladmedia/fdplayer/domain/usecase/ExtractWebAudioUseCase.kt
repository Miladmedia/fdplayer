package ir.miladmedia.fdplayer.domain.usecase

import ir.miladmedia.fdplayer.domain.model.DownloadableAudio
import ir.miladmedia.fdplayer.domain.repository.DownloadRepository
import javax.inject.Inject

class ExtractWebAudioUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(url: String): Result<List<DownloadableAudio>> {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return Result.failure(IllegalArgumentException("INVALID_URL"))
        }
        return downloadRepository.extractAudioLinks(trimmed)
    }
}
