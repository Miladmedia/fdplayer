package ir.miladmedia.fdplayer.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.miladmedia.fdplayer.domain.model.DownloadItem
import ir.miladmedia.fdplayer.domain.model.DownloadStatus
import ir.miladmedia.fdplayer.domain.model.QueueResult
import ir.miladmedia.fdplayer.domain.model.SearchResult
import ir.miladmedia.fdplayer.domain.repository.DownloadRepository
import ir.miladmedia.fdplayer.domain.repository.SettingsRepository
import ir.miladmedia.fdplayer.domain.repository.SourceGateway
import ir.miladmedia.fdplayer.domain.repository.SourceRepository
import ir.miladmedia.fdplayer.domain.usecase.QueueDownloadUseCase
import ir.miladmedia.fdplayer.service.DownloadScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DownloadForm(
    val url: String = "",
    val title: String = "",
    val artist: String = "",
    val keyword: String = ""
)

data class DownloadsUiState(
    val form: DownloadForm = DownloadForm(),
    val items: List<DownloadItem> = emptyList(),
    val searching: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val lastQueue: QueueResult? = null,
    val searchFailed: Boolean = false,
    val extracting: Boolean = false,
    val extracted: List<SearchResult> = emptyList(),
    val extractChecked: Boolean = false
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloads: DownloadRepository,
    private val sources: SourceRepository,
    private val gateway: SourceGateway,
    private val settings: SettingsRepository,
    private val queueDownload: QueueDownloadUseCase,
    private val scheduler: DownloadScheduler
) : ViewModel() {

    private val form = MutableStateFlow(DownloadForm())
    private val searching = MutableStateFlow(false)
    private val results = MutableStateFlow<List<SearchResult>>(emptyList())
    private val lastQueue = MutableStateFlow<QueueResult?>(null)
    private val searchFailed = MutableStateFlow(false)
    private val extracting = MutableStateFlow(false)
    private val extracted = MutableStateFlow<List<SearchResult>>(emptyList())
    private val extractChecked = MutableStateFlow(false)

    val state: StateFlow<DownloadsUiState> = combineState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    private fun combineState(): Flow<DownloadsUiState> =
        combine(
            combine(form, downloads.observeAll(), searching) { f, items, s ->
                Triple(f, items, s)
            },
            combine(results, lastQueue, searchFailed) { r, q, failed ->
                Triple(r, q, failed)
            },
            combine(extracting, extracted, extractChecked) { ing, ed, checked ->
                Triple(ing, ed, checked)
            }
        ) { a, b, c ->
            DownloadsUiState(
                form = a.first,
                items = a.second,
                searching = a.third,
                results = b.first,
                lastQueue = b.second,
                searchFailed = b.third,
                extracting = c.first,
                extracted = c.second,
                extractChecked = c.third
            )
        }

    fun updateForm(transform: (DownloadForm) -> DownloadForm) {
        form.value = transform(form.value)
    }

    fun submit() {
        val f = form.value
        viewModelScope.launch {
            val outcome = queueDownload(f.url, f.title, f.artist, f.keyword)
            lastQueue.value = outcome.result
            if (outcome.result == QueueResult.Queued) {
                val wifiOnly = settings.settings.first().wifiOnly
                scheduler.schedule(outcome.id, wifiOnly)
                form.value = DownloadForm()
                results.value = emptyList()
            }
        }
    }

    fun search() {
        val keyword = form.value.keyword.trim()
        if (keyword.isEmpty() || searching.value) return
        viewModelScope.launch {
            searching.value = true
            searchFailed.value = false
            try {
                val list = sources.getAll()
                val limit = settings.settings.first().searchLimit
                results.value = gateway.search(list, keyword, limit)
                searchFailed.value = results.value.isEmpty()
            } catch (e: Exception) {
                results.value = emptyList()
                searchFailed.value = true
            } finally {
                searching.value = false
            }
        }
    }

    fun pickResult(result: SearchResult) {
        form.value = form.value.copy(
            url = result.url,
            title = result.title,
            artist = result.artist
        )
    }

    fun cancel(item: DownloadItem) {
        viewModelScope.launch {
            scheduler.cancel(item.id)
            downloads.setStatus(item.id, DownloadStatus.CANCELED, null)
        }
    }

    fun retry(item: DownloadItem) {
        viewModelScope.launch {
            downloads.setStatus(item.id, DownloadStatus.QUEUED, null)
            scheduler.schedule(item.id, settings.settings.first().wifiOnly)
        }
    }

    fun delete(item: DownloadItem) {
        viewModelScope.launch {
            scheduler.cancel(item.id)
            downloads.delete(item.id)
        }
    }

    fun clearFinished() {
        viewModelScope.launch { downloads.clearFinished() }
    }

    fun dismissQueueMessage() {
        lastQueue.value = null
    }

    fun extract() {
        val url = form.value.url.trim()
        if (url.isEmpty() || extracting.value) return
        viewModelScope.launch {
            extracting.value = true
            extractChecked.value = false
            try {
                val result = gateway.extract(url)
                extracted.value = result.links
            } catch (e: Exception) {
                extracted.value = emptyList()
            } finally {
                extracting.value = false
                extractChecked.value = true
            }
        }
    }

    fun queueAll() {
        val list = extracted.value
        if (list.isEmpty()) return
        val keyword = form.value.keyword.trim()
        viewModelScope.launch {
            val wifiOnly = settings.settings.first().wifiOnly
            var queued = 0
            for (link in list) {
                val outcome = queueDownload(link.url, link.title, link.artist, keyword)
                if (outcome.result == QueueResult.Queued) {
                    scheduler.schedule(outcome.id, wifiOnly)
                    queued++
                }
            }
            extracted.value = emptyList()
            lastQueue.value = if (queued > 0) QueueResult.Queued else QueueResult.InvalidUrl
        }
    }

    fun queueSingle(link: SearchResult) {
        val keyword = form.value.keyword.trim()
        viewModelScope.launch {
            val outcome = queueDownload(link.url, link.title, link.artist, keyword)
            if (outcome.result == QueueResult.Queued) {
                val wifiOnly = settings.settings.first().wifiOnly
                scheduler.schedule(outcome.id, wifiOnly)
            }
            extracted.value = extracted.value.filter { it !== link }
            lastQueue.value = outcome.result
        }
    }
}
