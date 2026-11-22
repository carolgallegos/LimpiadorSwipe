@file:OptIn(ExperimentalMaterial3Api::class)

package com.limpiador.swipe

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

fun mediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO
    )
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

fun hasMediaPermission(ctx: Context): Boolean =
    Environment.isExternalStorageManager() ||
        mediaPermissions().any {
            ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
        }

@Composable
fun HomeScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var hasMedia by remember { mutableStateOf(hasMediaPermission(context)) }
    var hasFiles by remember { mutableStateOf(Environment.isExternalStorageManager()) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasMedia = hasMediaPermission(context)
        if (hasMedia) vm.refreshStats()
    }

    // Al volver de Ajustes, revisa de nuevo los permisos
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasMedia = hasMediaPermission(context)
        hasFiles = Environment.isExternalStorageManager()
        if (hasMedia || hasFiles) vm.refreshStats()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Limpiador Swipe", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Desliza ← para borrar y → para conservar",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Green.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Espacio liberado en total", color = Color.White.copy(alpha = 0.8f))
                    Text(vm.size(vm.freedTotal), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Green)
                }
            }
        }

        if (!hasMedia) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Necesito permiso para ver tus fotos y videos.")
                        Text(
                            "Elige \"Permitir todo\" para revisar toda tu galería.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.padding(4.dp))
                        Button(onClick = { permLauncher.launch(mediaPermissions()) }) {
                            Text("Dar permiso")
                        }
                    }
                }
            }
        }

        item {
            Column {
                Text("Empezar por:", color = Color.White.copy(alpha = 0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortOrder.entries.forEach { order ->
                        FilterChip(
                            selected = vm.sortOrder == order,
                            onClick = { vm.sortOrder = order },
                            label = { Text(order.label) }
                        )
                    }
                }
            }
        }

        items(Category.entries.filter { it != Category.FILES }) { cat ->
            CategoryCard(
                cat = cat,
                stats = vm.stats[cat],
                enabled = hasMedia,
                sizeText = vm::size,
                onClick = { vm.start(cat) }
            )
        }

        item {
            if (hasFiles) {
                CategoryCard(
                    cat = Category.FILES,
                    stats = vm.stats[Category.FILES],
                    enabled = true,
                    sizeText = vm::size,
                    onClick = { vm.start(Category.FILES) }
                )
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("📁  Descargas y documentos", fontWeight = FontWeight.Bold)
                        Text(
                            "Para limpiar PDFs, descargas y otros archivos, activa \"Permitir acceso para administrar todos los archivos\".",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.padding(4.dp))
                        Button(onClick = { openAllFilesSettings(context) }) {
                            Text("Abrir ajustes")
                        }
                    }
                }
            }
        }

        item {
            TextButton(onClick = { vm.resetKept() }) {
                Text("Volver a mostrar lo que ya conservé")
            }
        }
    }
}

@Composable
private fun CategoryCard(
    cat: Category,
    stats: CategoryStats?,
    enabled: Boolean,
    sizeText: (Long) -> String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(cat.emoji, fontSize = 32.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(cat.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    when {
                        !enabled -> "Sin permiso"
                        stats == null -> "Contando…"
                        stats.count == 0 -> "Nada por revisar ✨"
                        else -> "${stats.count} por revisar · ${sizeText(stats.bytes)}"
                    },
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun openAllFilesSettings(context: Context) {
    try {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        )
    } catch (e: Exception) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }
}
