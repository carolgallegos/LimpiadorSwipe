package com.limpiador.swipe

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ReviewScreen(vm: AppViewModel) {
    var confirmFiles by remember { mutableStateOf(false) }
    val isFiles = vm.category == Category.FILES

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        vm.onTrashResult(result.resultCode == Activity.RESULT_OK)
    }

    fun doDelete() {
        val sender = vm.startDelete()
        if (sender != null) {
            trashLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    BackHandler { vm.backToSwipe() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TextButton(onClick = { vm.backToSwipe() }) { Text("← Seguir deslizando") }

        Text(
            "Para borrar: ${vm.toDelete.size} · ${vm.size(vm.bytesToFree)}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Toca una para quitarla de la lista.",
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(12.dp))

        if (vm.toDelete.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay nada marcado para borrar", textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(vm.toDelete, key = { it.key }) { item ->
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2A2A2A))
                            .clickable { vm.unmark(item) }
                    ) {
                        if (item.isImage || item.isVideo) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📄", fontSize = 28.sp)
                                Text(
                                    item.name,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        if (item.isVideo) {
                            Text(
                                "▶",
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            )
                        }
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(Red, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("✕", color = Color.White, fontSize = 12.sp) }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { if (isFiles) confirmFiles = true else doDelete() },
            enabled = vm.toDelete.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Red)
        ) {
            Text(
                "🗑  Borrar ${vm.toDelete.size} (${vm.size(vm.bytesToFree)})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            if (isFiles) "Los archivos se borran definitivamente."
            else "Van a la papelera de tu galería. Puedes recuperarlos durante unos 30 días.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }

    if (confirmFiles) {
        AlertDialog(
            onDismissRequest = { confirmFiles = false },
            title = { Text("¿Borrar ${vm.toDelete.size} archivos?") },
            text = { Text("Estos archivos no van a la papelera: se borran para siempre.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmFiles = false
                    doDelete()
                }) { Text("Borrar", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmFiles = false }) { Text("Cancelar") }
            }
        )
    }
}
