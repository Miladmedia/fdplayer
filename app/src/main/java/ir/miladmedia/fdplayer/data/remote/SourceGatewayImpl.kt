package ir.miladmedia.fdplayer.data.remote

import ir.miladmedia.fdplayer.domain.model.CheckKind
import ir.miladmedia.fdplayer.domain.model.CheckResult
import ir.miladmedia.fdplayer.domain.model.ExtractResult
import ir.miladmedia.fdplayer.domain.model.SearchResult
import ir.miladmedia.fdplayer.domain.model.Source
import ir.miladmedia.fdplayer.domain.repository.SourceGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceGatewayImpl @Inject constructor(
    private val client: OkHttpClient
) : SourceGateway {

    private val audioExt = listOf(".mp3", ".m4a", ".flac", ".ogg", ".opus", ".wav", ".aac")

    private fun isAudio(url: String): Boolean {
        val path = url.substringBefore('?').lowercase()
        return audioExt.any { path.endsWith(it) }
    }

    private fun fetch(url: String): String? {
        val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 FDPlayer").build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            return r.body?.string()
        }
    }

    private fun titleFromUrl(url: String): String {
        val name = url.substringBefore('?').substringAfterLast('/')
        val decoded = runCatching { URLDecoder.decode(name, "UTF-8") }.getOrDefault(name)
        return decoded.substringBeforeLast('.').replace('_', ' ').trim()
    }

    private fun parse(html: String, baseUrl: String): List<SearchResult> {
        val doc = Jsoup.parse(html, baseUrl)
        val seen = HashSet<String>()
        val out = ArrayList<SearchResult>()
        for (a in doc.select("a[href]")) {
            val abs = a.absUrl("href")
            if (abs.isEmpty() || !isAudio(abs) || !seen.add(abs)) continue
            val text = a.text().trim()
            out.add(SearchResult(text.ifEmpty { titleFromUrl(abs) }, "", abs))
        }
        for (s in doc.select("audio[src], audio source[src]")) {
            val abs = s.absUrl("src")
            if (abs.isNotEmpty() && seen.add(abs)) out.add(SearchResult(titleFromUrl(abs), "", abs))
        }
        return out
    }

    override suspend fun search(sources: List<Source>, keyword: String, limit: Int): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val q = URLEncoder.encode(keyword, "UTF-8")
            val all = ArrayList<SearchResult>()
            for (s in sources) {
                if (all.size >= limit) break
                val url = when {
                    s.url.contains("{q}") -> s.url.replace("{q}", q)
                    s.url.contains("%s") -> s.url.replace("%s", q)
                    else -> s.url
                }
                val html = runCatching { fetch(url) }.getOrNull() ?: continue
                all.addAll(parse(html, url))
            }
            all.distinctBy { it.url }.take(limit)
        }

    override suspend fun extract(url: String): ExtractResult = withContext(Dispatchers.IO) {
        try {
            val html = fetch(url) ?: return@withContext ExtractResult(emptyList(), false, "HTTP error")
            val links = parse(html, url)
            val doc = Jsoup.parse(html)
            val js = links.isEmpty() && doc.select("script").size > 3 && doc.body().text().length < 300
            ExtractResult(links, js, null)
        } catch (e: Exception) {
            ExtractResult(emptyList(), false, e.message ?: e.javaClass.simpleName)
        }
    }

    override suspend fun test(url: String): List<CheckResult> = withContext(Dispatchers.IO) {
        val out = ArrayList<CheckResult>()
        val res = extract(url)
        val reachable = res.error == null
        out.add(CheckResult(CheckKind.REACHABLE, reachable, res.error ?: "OK"))
        out.add(CheckResult(CheckKind.LINKS, res.links.isNotEmpty(), "${res.links.size}"))
        val first = res.links.firstOrNull()
        if (first == null) {
            out.add(CheckResult(CheckKind.RANGE, false, "-"))
        } else {
            val ok = runCatching {
                val req = Request.Builder().url(first.url).header("Range", "bytes=0-0").build()
                client.newCall(req).execute().use { it.code == 206 }
            }.getOrDefault(false)
            out.add(CheckResult(CheckKind.RANGE, ok, if (ok) "206" else "no range"))
        }
        out
    }
}
