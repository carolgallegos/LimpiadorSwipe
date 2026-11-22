package com.limpiador.swipe

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File

class MediaRepository(private val context: Context) {

    fun load(category: Category): List<Item> = when (category) {
        Category.PHOTOS -> queryImages(null, null)
        Category.VIDEOS -> queryVideos(null, null)
        Category.SCREENSHOTS -> queryImages(
            "(${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?)",
            arrayOf("%Screenshot%", "Screenshot%")
        )
        Category.WHATSAPP -> {
            val sel = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val args = arrayOf("%WhatsApp%")
            queryImages(sel, args) + queryVideos(sel, args)
        }
        Category.FILES -> loadFiles()
    }

    private fun queryImages(sel: String?, args: Array<String>?) =
        query(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), false, sel, args)

    private fun queryVideos(sel: String?, args: Array<String>?) =
        query(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), true, sel, args)

    private fun query(
        collection: Uri,
        isVideo: Boolean,
        selection: String?,
        args: Array<String>?
    ): List<Item> {
        val base = listOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        val projection = (if (isVideo) base + MediaStore.MediaColumns.DURATION else base).toTypedArray()

        val out = ArrayList<Item>()
        context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val durCol = if (isVideo) c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION) else -1

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val taken = if (c.isNull(takenCol)) 0L else c.getLong(takenCol)
                val added = c.getLong(addedCol) * 1000L
                out += Item(
                    key = "m:${if (isVideo) "v" else "i"}:$id",
                    uri = ContentUris.withAppendedId(collection, id),
                    name = c.getString(nameCol) ?: "sin nombre",
                    size = c.getLong(sizeCol),
                    dateMillis = if (taken > 0) taken else added,
                    isVideo = isVideo,
                    isImage = !isVideo,
                    folder = c.getString(pathCol)?.trimEnd('/') ?: "",
                    filePath = null,
                    durationMs = if (durCol >= 0 && !c.isNull(durCol)) c.getLong(durCol) else 0L
                )
            }
        }
        return out
    }

    private fun loadFiles(): List<Item> {
        if (!Environment.isExternalStorageManager()) return emptyList()
        val storageRoot = Environment.getExternalStorageDirectory().absolutePath + "/"
        val roots = listOf(Environment.DIRECTORY_DOWNLOADS, Environment.DIRECTORY_DOCUMENTS)
            .map { Environment.getExternalStoragePublicDirectory(it) }
            .filter { it.exists() }

        val out = ArrayList<Item>()
        for (root in roots) {
            root.walkTopDown()
                .onEnter { !it.name.startsWith(".") }
                .maxDepth(6)
                .filter { it.isFile && !it.name.startsWith(".") }
                .forEach { f ->
                    val ext = f.extension.lowercase()
                    out += Item(
                        key = "f:${f.absolutePath}",
                        uri = Uri.fromFile(f),
                        name = f.name,
                        size = f.length(),
                        dateMillis = f.lastModified(),
                        isVideo = ext in VIDEO_EXT,
                        isImage = ext in IMAGE_EXT,
                        folder = f.parentFile?.absolutePath?.removePrefix(storageRoot) ?: "",
                        filePath = f.absolutePath
                    )
                }
        }
        return out
    }

    /** Borra archivos de forma definitiva. Devuelve los que sí se borraron. */
    fun deleteFiles(items: List<Item>): List<Item> {
        val deleted = items.filter { item -> item.filePath?.let { File(it).delete() } == true }
        if (deleted.isNotEmpty()) {
            MediaScannerConnection.scanFile(
                context, deleted.mapNotNull { it.filePath }.toTypedArray(), null, null
            )
        }
        return deleted
    }

    /** Pide a Android mover fotos/videos a la papelera (se pueden recuperar ~30 días). */
    fun trashRequest(items: List<Item>): IntentSender =
        MediaStore.createTrashRequest(context.contentResolver, items.map { it.uri }, true).intentSender

    companion object {
        val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp")
        val VIDEO_EXT = setOf("mp4", "mkv", "webm", "3gp", "mov", "avi")
    }
}
