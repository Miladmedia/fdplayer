package ir.miladmedia.fdplayer.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

/**
 * Moves a finished temp file to the user's chosen SAF tree, or to the app's
 * external Music directory when no tree is set. Returns the final content or file URI string.
 */
object StorageWriter {

    fun publish(context: Context, treeUri: String?, temp: File, displayName: String): String {
        val safe = sanitize(displayName).ifBlank { temp.nameWithoutExtension }
        val fileName = "$safe.mp3"
        if (treeUri != null) {
            val tree = Uri.parse(treeUri)
            val parentDoc = DocumentsContract.buildDocumentUriUsingTree(
                tree,
                DocumentsContract.getTreeDocumentId(tree)
            )
            val created = DocumentsContract.createDocument(
                context.contentResolver,
                parentDoc,
                "audio/mpeg",
                fileName
            ) ?: throw IllegalStateException("cannot create document")
            context.contentResolver.openOutputStream(created)?.use { out ->
                temp.inputStream().use { it.copyTo(out) }
            } ?: throw IllegalStateException("cannot open output")
            return created.toString()
        }
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
            ?: throw IllegalStateException("no music dir")
        dir.mkdirs()
        val dest = uniqueFile(dir, fileName)
        temp.copyTo(dest, overwrite = false)
        return Uri.fromFile(dest).toString()
    }

    private fun uniqueFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "${name.substringBeforeLast('.')} ($i).${name.substringAfterLast('.')}")
            i++
        }
        return candidate
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().take(120)
}
