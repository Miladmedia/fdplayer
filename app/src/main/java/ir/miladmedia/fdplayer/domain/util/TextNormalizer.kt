package ir.miladmedia.fdplayer.domain.util

/** Persian/Arabic aware text normalization used for filtering, keywords and duplicate keys. */
object TextNormalizer {

    fun normalize(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                'ي', 'ى' -> sb.append('ی')
                'ك' -> sb.append('ک')
                'ة' -> sb.append('ه')
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
                in '۰'..'۹' -> sb.append('0' + (ch - '۰'))
                '\u200C' -> sb.append(' ')
                '\u0640' -> Unit
                in '\u064B'..'\u065F' -> Unit
                '\u0670' -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString().lowercase().trim().replace(Regex("\\s+"), " ")
    }

    fun nameKey(title: String, artist: String): String =
        normalize(title) + "|" + normalize(artist)

    fun titleKey(title: String): String = normalize(title)
}
