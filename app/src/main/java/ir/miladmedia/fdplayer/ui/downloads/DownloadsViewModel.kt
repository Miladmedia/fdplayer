package ir.miladmedia.fdplayer.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.miladmedia.fdplayer.data.preferences.TipsPreferences
import ir.miladmedia.fdplayer.domain.model.DownloadableAudio
import ir.miladmedia.fdplayer.domain.repository.DownloadRepository
import ir.miladmedia.fdplayer.domain.usecase.ExtractWebAudioUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUiState(
    val urlInput: String = "",
    val isExtracting: Boolean = false,
    val items: List<DownloadableAudio> = emptyList(),
    val statusMessageRes: Int? = null,
    val showFirstTimeTip: Boolean = false
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val extractWebAudioUseCase: ExtractWebAudioUseCase,
    private val downloadRepository: DownloadRepository,
    private val tipsPreferences: TipsPreferences
) : ViewModel() {

    private val urlInput = MutableStateFlow("")
    private val isExtracting = MutableStateFlow(false)
    private val statusMessageRes = MutableStateFlow<Int?>(null)
    private val showTip = MutableStateFlow(!tipsPreferences.isTipDismissed("downloads"))

    val uiState: StateFlow<DownloadsUiState> = combine(
        urlInput,
        isExtracting,
        downloadRepository.observeDownloads(),
        statusMessageRes,
        showTip
    ) { url, loading, items, msgRes, tip ->
        DownloadsUiState(
            urlInput = url,
            isExtracting = loading,
            items = items,
            statusMessageRes = msgRes,
            showFirstTimeTip = tip
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DownloadsUiState()
    )

    fun onUrlChanged(newUrl: String) {
        urlInput.value = newUrl
        statusMessageRes.value = null
    }

    fun extractLinks() {
        val currentUrl = urlInput.value.trim()
        if (currentUrl.isEmpty()) return

        viewModelScope.launch {
            isExtracting.value = true
            statusMessageRes.value = null
            val result = extractWebAudioUseCase(currentUrl)
            isExtracting.value = false

            result.onSuccess { found ->
                if (found.isEmpty()) {
                    statusMessageRes.value = ir.miladmedia.fdplayer.R.string.no_audio_links_found
                }
            }.onFailure { err ->
                statusMessageRes.value = if (err.message == "INVALID_URL") {
                    ir.miladmedia.fdplayer.R.string.invalid_url_msg
                } else {
                    ir.miladmedia.fdplayer.R.string.no_audio_links_found
                }
            }
        }
    }

    fun downloadSingle(item: DownloadableAudio) {
        downloadRepository.enqueueDownload(item)
    }

    fun downloadAll() {
        downloadRepository.enqueueAll(uiState.value.items)
    }

    fun dismissTip() {
        tipsPreferences.dismissTip("downloads")
        showTip.value = false
    }
}
