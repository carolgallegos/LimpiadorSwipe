package com.limpiador.swipe

import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MediaRepository(app)
    private val prefs = app.getSharedPreferences("limpiador", Context.MODE_PRIVATE)

    // Lo que ya decidiste conservar no se vuelve a mostrar
    private val kept: MutableSet<String> =
        (prefs.getStringSet("kept", emptySet()) ?: emptySet()).toMutableSet()

    var screen by mutableStateOf(Screen.HOME)
        private set
    var category by mutableStateOf(Category.PHOTOS)
        private set
    var sortOrder by mutableStateOf(SortOrder.NEWEST)
    var loading by mutableStateOf(false)
        private set
    var queue by mutableStateOf<List<Item>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var undoAvailable by mutableStateOf(false)
        private set
    var freedTotal by mutableLongStateOf(prefs.getLong("freed", 0L))
        private set
    var message by mutableStateOf<String?>(null)

    val toDelete = mutableStateListOf<Item>()
    val stats = mutableStateMapOf<Category, CategoryStats>()

    private val history = ArrayDeque<Pair<Item, Boolean>>() // (elemento, ¿se marcó para borrar?)

    val current: Item? get() = queue.getOrNull(index)
    val next: Item? get() = queue.getOrNull(index + 1)
    val remaining: Int get() = (queue.size - index).coerceAtLeast(0)
    val bytesToFree: Long get() = toDelete.sumOf { it.size }

    fun size(bytes: Long): String = getApplication<Application>().formatSize(bytes)

    fun refreshStats() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                Category.entries.mapNotNull { cat ->
                    if (cat == Category.FILES && !Environment.isExternalStorageManager()) return@mapNotNull null
                    val items = runCatching { repo.load(cat) }.getOrDefault(emptyList())
                        .filter { it.key !in kept }
                    cat to CategoryStats(items.size, items.sumOf { it.size })
                }
            }
            stats.clear()
            result.forEach { (cat, s) -> stats[cat] = s }
        }
    }

    fun start(cat: Category) {
        category = cat
        loading = true
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                runCatching { repo.load(cat) }.getOrDefault(emptyList())
            }.filter { it.key !in kept }

            queue = when (sortOrder) {
                SortOrder.NEWEST -> items.sortedByDescending { it.dateMillis }
                SortOrder.OLDEST -> items.sortedBy { it.dateMillis }
                SortOrder.BIGGEST -> items.sortedByDescending { it.size }
            }
            index = 0
            toDelete.clear()
            history.clear()
            undoAvailable = false
            loading = false
            screen = Screen.SWIPE
        }
    }

    fun decide(delete: Boolean) {
        val item = current ?: return
        if (delete) {
            toDelete.add(item)
        } else {
            kept.add(item.key)
            saveKept()
        }
        history.addLast(item to delete)
        undoAvailable = true
        index++
        if (index >= queue.size && toDelete.isNotEmpty()) screen = Screen.REVIEW
    }

    fun undo() {
        val (item, deleted) = history.removeLastOrNull() ?: return
        if (deleted) {
            toDelete.remove(item)
        } else {
            kept.remove(item.key)
            saveKept()
        }
        index = (index - 1).coerceAtLeast(0)
        undoAvailable = history.isNotEmpty()
        screen = Screen.SWIPE
    }

    fun unmark(item: Item) {
        toDelete.remove(item)
    }

    fun openReview() {
        screen = Screen.REVIEW
    }

    fun backToSwipe() {
        screen = Screen.SWIPE
    }

    fun goHome() {
        toDelete.clear()
        history.clear()
        undoAvailable = false
        screen = Screen.HOME
        refreshStats()
    }

    fun resetKept() {
        kept.clear()
        saveKept()
        message = "Listo: se volverán a mostrar las que habías conservado"
        refreshStats()
    }

    /**
     * Archivos: se borran aquí mismo y devuelve null.
     * Fotos/videos: devuelve la petición del sistema para mandarlos a la papelera.
     */
    fun startDelete(): IntentSender? {
        val items = toDelete.toList()
        if (items.isEmpty()) return null
        return if (category == Category.FILES) {
            val deleted = repo.deleteFiles(items)
            finishDelete(deleted)
            if (deleted.size < items.size) {
                message = "No se pudieron borrar ${items.size - deleted.size} archivos"
            }
            null
        } else {
            runCatching { repo.trashRequest(items) }.getOrElse {
                message = "No se pudo borrar: ${it.message}"
                null
            }
        }
    }

    fun onTrashResult(ok: Boolean) {
        if (ok) finishDelete(toDelete.toList())
        else message = "No se borró nada. Puedes intentarlo de nuevo."
    }

    private fun finishDelete(deleted: List<Item>) {
        val bytes = deleted.sumOf { it.size }
        freedTotal += bytes
        prefs.edit().putLong("freed", freedTotal).apply()

        val keys = deleted.map { it.key }.toSet()
        toDelete.removeAll { it.key in keys }
        history.clear()
        undoAvailable = false
        message = "¡Listo! Liberaste ${size(bytes)} (${deleted.size} elementos)"

        if (index < queue.size) {
            screen = Screen.SWIPE
        } else {
            screen = Screen.HOME
            refreshStats()
        }
    }

    private fun saveKept() {
        prefs.edit().putStringSet("kept", HashSet(kept)).apply()
    }
}
