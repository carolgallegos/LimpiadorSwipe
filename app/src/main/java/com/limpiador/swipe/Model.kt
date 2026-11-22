package com.limpiador.swipe

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import java.text.DateFormat
import java.util.Date

enum class Category(val title: String, val emoji: String) {
    PHOTOS("Fotos", "📷"),
    VIDEOS("Videos", "🎬"),
    SCREENSHOTS("Capturas de pantalla", "📱"),
    WHATSAPP("WhatsApp", "💬"),
    FILES("Descargas y documentos", "📁")
}

enum class SortOrder(val label: String) {
    NEWEST("Recientes"),
    OLDEST("Antiguas"),
    BIGGEST("Más pesadas")
}

enum class Screen { HOME, SWIPE, REVIEW }

data class CategoryStats(val count: Int, val bytes: Long)

data class Item(
    val key: String,          // identificador único
    val uri: Uri,             // content:// para galería, file:// para archivos
    val name: String,
    val size: Long,
    val dateMillis: Long,
    val isVideo: Boolean,
    val isImage: Boolean,
    val folder: String,
    val filePath: String?,    // solo para "Descargas y documentos"
    val durationMs: Long = 0L
) {
    val extension: String get() = name.substringAfterLast('.', "").uppercase()
}

fun Context.formatSize(bytes: Long): String = Formatter.formatShortFileSize(this, bytes)

fun formatDate(millis: Long): String =
    if (millis <= 0) "" else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
