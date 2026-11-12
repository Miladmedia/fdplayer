package ir.miladmedia.fdplayer.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.miladmedia.fdplayer.domain.model.CheckKind
import ir.miladmedia.fdplayer.domain.model.CheckResult
import ir.miladmedia.fdplayer.domain.model.Source
import ir.miladmedia.fdplayer.domain.repository.SourceGateway
import ir.miladmedia.fdplayer.domain.repository.SourceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val gateway: SourceGateway
) : ViewModel() {

    val sources: StateFlow<List<Source>> = sourceRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _testing = MutableStateFlow<Set<Long>>(emptySet())
    val testing: StateFlow<Set<Long>> = _testing.asStateFlow()

    private val _results = MutableStateFlow<Map<Long, List<CheckResult>>>(emptyMap())
    val results: StateFlow<Map<Long, List<CheckResult>>> = _results.asStateFlow()

    /** Accepts only http(s) URLs with a non-blank name. Returns false when the input is rejected. */
    fun add(name: String, url: String): Boolean {
        val cleanName = name.trim()
        val cleanUrl = url.trim()
        if (cleanName.isEmpty()) return false
        if (!(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://"))) return false
        viewModelScope.launch { sourceRepository.add(cleanName, cleanUrl) }
        return true
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            sourceRepository.remove(id)
            _results.value = _results.value - id
        }
    }

    fun test(source: Source) {
        if (source.id in _testing.value) return
        _testing.value = _testing.value + source.id
        viewModelScope.launch {
            val outcome = try {
                gateway.test(source.url)
            } catch (e: Exception) {
                listOf(CheckResult(CheckKind.REACHABLE, false, e.message ?: ""))
            }
            _results.value = _results.value + (source.id to outcome)
            _testing.value = _testing.value - source.id
        }
    }
}
