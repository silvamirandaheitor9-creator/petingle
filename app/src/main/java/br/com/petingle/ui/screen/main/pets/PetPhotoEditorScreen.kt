package br.com.petingle.ui.screen.main.pets

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import br.com.petingle.ui.screen.main.diary.cropBitmapToSquareRegion
import br.com.petingle.ui.screen.main.diary.loadBitmapRespectingExif
import br.com.petingle.ui.screen.main.diary.rotateBitmap
import br.com.petingle.ui.theme.OrangePrimary
import br.com.petingle.ui.theme.spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Editor de foto de perfil do pet (SPEC §11 — parte 2): tela cheia normal do
 * NavGraph com etapa de cortar/girar — sem filtros, adesivos ou texto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetPhotoEditorScreen(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onSave: (photoPath: String) -> Unit,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var sourceBitmap     by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var isLoadingSource  by remember(imageUri) { mutableStateOf(true) }
    LaunchedEffect(imageUri) {
        isLoadingSource = true
        sourceBitmap = withContext(Dispatchers.IO) { loadBitmapRespectingExif(context, imageUri) }
        isLoadingSource = false
    }

    var isSaving by remember { mutableStateOf(false) }

    fun handleBack() { if (!isSaving) onDismiss() }
    BackHandler(onBack = ::handleBack)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            TopAppBar(
                title = { Text("Foto do pet") },
                navigationIcon = {
                    IconButton(onClick = ::handleBack, enabled = !isSaving) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )

            when {
                isLoadingSource || isSaving -> LoadingBox()
                sourceBitmap == null        -> ErrorBox(onDismiss = onDismiss)
                else -> CropRotateStep(
                    workingBitmap = sourceBitmap!!,
                    onRotate = {
                        scope.launch {
                            val rotated = withContext(Dispatchers.Default) {
                                rotateBitmap(sourceBitmap!!, 90f)
                            }
                            sourceBitmap = rotated
                        }
                    },
                    onCropApplied = { cropped ->
                        isSaving = true
                        scope.launch {
                            val photoPath = withContext(Dispatchers.Default) {
                                savePetPhotoJpeg(context, cropped)
                            }
                            isSaving = false
                            onSave(photoPath)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = OrangePrimary)
    }
}

@Composable
private fun ErrorBox(onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "Não foi possível abrir essa foto.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss) { Text("Voltar") }
        }
    }
}

// ─── Cortar e girar ───────────────────────────────────────────────────────────
// Reutilizado pelo fluxo de foto de perfil do pet. Mesma lógica que estava
// em DiaryPhotoEditorScreen antes da remoção do editor do Diário.

@Composable
internal fun CropRotateStep(
    workingBitmap: Bitmap,
    onRotate: () -> Unit,
    onCropApplied: (Bitmap) -> Unit,
) {
    var frameSizePx by remember { mutableStateOf(0f) }
    var panOffset   by remember(workingBitmap) { mutableStateOf(Offset.Zero) }
    var zoom        by remember(workingBitmap) { mutableStateOf(1f) }

    val imageBitmap = remember(workingBitmap) { workingBitmap.asImageBitmap() }

    val baseScale = remember(workingBitmap, frameSizePx) {
        if (frameSizePx <= 0f) 1f
        else maxOf(frameSizePx / workingBitmap.width, frameSizePx / workingBitmap.height)
    }
    val totalScale = baseScale * zoom

    fun clampPan(offset: Offset, scale: Float): Offset {
        val displayedWidth  = workingBitmap.width  * scale
        val displayedHeight = workingBitmap.height * scale
        val maxX = ((displayedWidth  - frameSizePx) / 2f).coerceAtLeast(0f)
        val maxY = ((displayedHeight - frameSizePx) / 2f).coerceAtLeast(0f)
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f)
                    .onSizeChanged { frameSizePx = it.width.toFloat() }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .pointerInput(workingBitmap, frameSizePx) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val newZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                            zoom = newZoom
                            panOffset = clampPan(panOffset + pan, baseScale * newZoom)
                        }
                    },
            ) {
                val dstWidth  = (workingBitmap.width  * totalScale).roundToInt()
                val dstHeight = (workingBitmap.height * totalScale).roundToInt()
                val dstOffset = IntOffset(
                    x = (size.width  / 2f - dstWidth  / 2f + panOffset.x).roundToInt(),
                    y = (size.height / 2f - dstHeight / 2f + panOffset.y).roundToInt(),
                )
                drawImage(
                    image     = imageBitmap,
                    dstOffset = dstOffset,
                    dstSize   = IntSize(dstWidth, dstHeight),
                )
            }
        }

        Text(
            text  = "Arraste para posicionar e use pinça para dar zoom",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.sm),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onRotate) {
                Icon(Icons.Rounded.RotateRight, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Girar")
            }
            Button(
                onClick = {
                    val cropSizePx = (frameSizePx / totalScale)
                    val leftPx = (workingBitmap.width  - cropSizePx) / 2f - panOffset.x / totalScale
                    val topPx  = (workingBitmap.height - cropSizePx) / 2f - panOffset.y / totalScale
                    val cropped = cropBitmapToSquareRegion(
                        bitmap = workingBitmap,
                        left   = leftPx.roundToInt(),
                        top    = topPx.roundToInt(),
                        size   = cropSizePx.roundToInt(),
                    )
                    onCropApplied(cropped)
                },
                modifier = Modifier.height(56.dp),
            ) {
                Text("Avançar")
            }
        }
    }
}
