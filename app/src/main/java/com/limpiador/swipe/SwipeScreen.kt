package com.limpiador.swipe

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun SwipeScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExitDialog by remember { mutableStateOf(false) }

    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val threshold = widthPx * 0.28f

    val item = vm.current
    val offset = remember(item?.key) { Animatable(0f) }
    var busy by remember(item?.key) { mutableStateOf(false) }

    fun fling(delete: Boolean) {
        if (busy || vm.current == null) return
        busy = true
        scope.launch {
            offset.animateTo(if (delete) -widthPx * 1.5f else widthPx * 1.5f, tween(220))
            vm.decide(delete)
        }
    }

    fun tryExit() {
        if (vm.toDelete.isNotEmpty()) showExitDialog = true else vm.goHome()
    }

    BackHandler { tryExit() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Barra superior
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { tryExit() }) { Text("← Inicio") }
            Spacer(Modifier.weight(1f))
            Text(
                "${vm.category.emoji} ${vm.remaining} por revisar",
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        if (item == null) {
            // Ya no quedan elementos
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 64.sp)
                Text(
                    "¡Ya revisaste todo en ${vm.category.title}!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.goHome() }) { Text("Volver al inicio") }
            }
        } else {
            // Tarjetas
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                vm.next?.let { next ->
                    ItemCard(
                        item = next,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 0.93f
                                scaleY = 0.93f
                                alpha = 0.5f
                            }
                    )
                }

                ItemCard(
                    item = item,
                    swipeProgress = (offset.value / threshold).coerceIn(-1f, 1f),
                    onOpen = {
                        if (item.uri.scheme == "content") {
                            val intent = Intent(Intent.ACTION_VIEW)
                                .setDataAndType(item.uri, if (item.isVideo) "video/*" else "image/*")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = offset.value
                            rotationZ = offset.value / widthPx * 12f
                        }
                        .pointerInput(item.key) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val v = offset.value
                                    when {
                                        v > threshold -> fling(delete = false)
                                        v < -threshold -> fling(delete = true)
                                        else -> scope.launch { offset.animateTo(0f, spring()) }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { offset.animateTo(0f, spring()) }
                                },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    scope.launch { offset.snapTo(offset.value + amount) }
                                }
                            )
                        }
                )
            }

            // Botones
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundButton("✕", Red, 72) { fling(delete = true) }
                RoundButton("↶", Color(0xFF555555), 52, enabled = vm.undoAvailable) { vm.undo() }
                RoundButton("♥", Green, 72) { fling(delete = false) }
            }
        }

        if (vm.toDelete.isNotEmpty()) {
            Button(
                onClick = { vm.openReview() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) {
                Text("Revisar y borrar ${vm.toDelete.size} · ${vm.size(vm.bytesToFree)}")
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Tienes ${vm.toDelete.size} marcadas") },
            text = { Text("Todavía no se han borrado. ¿Qué quieres hacer?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    vm.openReview()
                }) { Text("Revisar y borrar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    vm.goHome()
                }) { Text("Salir sin borrar") }
            }
        )
    }
}

@Composable
private fun RoundButton(
    label: String,
    color: Color,
    sizeDp: Int,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(sizeDp.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = color,
            contentColor = Color.White
        )
    ) {
        Text(label, fontSize = (sizeDp / 2.4).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ItemCard(
    item: Item,
    modifier: Modifier = Modifier,
    swipeProgress: Float = 0f,
    onOpen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .then(if (onOpen != null) Modifier.clickable { onOpen() } else Modifier)
        ) {
            if (item.isImage || item.isVideo) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📄", fontSize = 80.sp)
                        Text(
                            item.extension.ifEmpty { "ARCHIVO" },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            if (item.isVideo) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .background(Color(0x88000000), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("▶", fontSize = 32.sp, color = Color.White) }
            }

            // Información abajo
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000))))
                    .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp)
            ) {
                Text(
                    item.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val details = buildList {
                    add(context.formatSize(item.size))
                    formatDate(item.dateMillis).takeIf { it.isNotEmpty() }?.let { add(it) }
                    if (item.isVideo && item.durationMs > 0) add(formatDuration(item.durationMs))
                }.joinToString(" · ")
                Text(details, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                if (item.folder.isNotEmpty()) {
                    Text(
                        item.folder,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Sellos de CONSERVAR / BORRAR
            if (swipeProgress > 0.05f) {
                Stamp("CONSERVAR", Green, -15f, Modifier.align(Alignment.TopStart).alpha(swipeProgress))
            }
            if (swipeProgress < -0.05f) {
                Stamp("BORRAR", Red, 15f, Modifier.align(Alignment.TopEnd).alpha(-swipeProgress))
            }
        }
    }
}

@Composable
private fun Stamp(text: String, color: Color, angle: Float, modifier: Modifier) {
    Text(
        text,
        color = color,
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
            .padding(24.dp)
            .rotate(angle)
            .border(4.dp, color, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
