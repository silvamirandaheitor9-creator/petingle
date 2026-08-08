package br.com.petingle.ui.screen.main.diary

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.ui.theme.OrangeGradEnd
import br.com.petingle.ui.theme.OrangeGradStart
import br.com.petingle.ui.theme.OrangePrimary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Tags de atividade / humor ────────────────────────────────────────────────

private data class ActivityTag(val emoji: String, val label: String)

private val ACTIVITY_TAGS = listOf(
    ActivityTag("🐾", "Passeio"),
    ActivityTag("🎾", "Brincadeira"),
    ActivityTag("🛁", "Banho"),
    ActivityTag("🏥", "Consulta"),
    ActivityTag("💛", "Carinho"),
    ActivityTag("📸", "Especial"),
    ActivityTag("🌿", "Ao ar livre"),
    ActivityTag("🍖", "Petisco"),
)

/**
 * Tela de nova entrada no Diário — redesenhada para ser mais viva e expressiva.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiaryAddEntryScreen(
    imageUri: Uri,
    pets: ImmutableList<Pet>,
    onDismiss: () -> Unit,
    onSave: (petId: Long, photoPath: String, caption: String) -> Unit,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var caption        by rememberSaveable { mutableStateOf("") }
    var selectedPetId  by remember(pets) { mutableStateOf(pets.firstOrNull()?.id) }
    var selectedTagIdx by remember { mutableStateOf<Int?>(null) }
    var isSaving       by remember { mutableStateOf(false) }

    val captionProgress = caption.length / 140f

    fun handleBack() { if (!isSaving) onDismiss() }
    BackHandler(onBack = ::handleBack)

    fun onTagSelected(index: Int) {
        if (selectedTagIdx == index) {
            selectedTagIdx = null
        } else {
            selectedTagIdx = index
            if (caption.isBlank()) {
                val tag = ACTIVITY_TAGS[index]
                caption = "${tag.emoji} ${tag.label}"
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // ── Header com gradiente laranja ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(OrangeGradStart, OrangeGradEnd)),
                    ),
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text       = "Nova memória",
                                fontWeight = FontWeight.Bold,
                                color      = Color.White,
                                style      = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text  = "Registre este momento especial",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.80f),
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = ::handleBack, enabled = !isSaving) {
                            Icon(Icons.Rounded.ArrowBack, "Voltar", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {

                // ── Foto estilo polaroid ─────────────────────────────────────
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .shadow(12.dp, RoundedCornerShape(6.dp))
                            .background(Color(0xFFF5F0E8), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                            .padding(bottom = 36.dp),
                    ) {
                        Column {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUri)
                                    .size(900)
                                    .scale(Scale.FILL)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto selecionada",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(3.dp)),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text      = "📸  PetIngle",
                                style     = MaterialTheme.typography.labelSmall,
                                color     = Color(0xFF9E8D7A),
                                fontWeight = FontWeight.Medium,
                                modifier  = Modifier.align(Alignment.CenterHorizontally),
                            )
                        }
                    }
                }

                // ── Qual foi o momento? ───────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text       = "Qual foi o momento?",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text  = "(opcional)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp),
                    ) {
                        ACTIVITY_TAGS.forEachIndexed { index, tag ->
                            val isSelected = selectedTagIdx == index
                            val bgColor by animateColorAsState(
                                targetValue   = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                animationSpec = tween(200),
                                label         = "tag_bg_$index",
                            )
                            val textColor by animateColorAsState(
                                targetValue   = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(200),
                                label         = "tag_text_$index",
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(bgColor)
                                    .then(
                                        if (!isSelected) Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.30f),
                                            RoundedCornerShape(50),
                                        ) else Modifier
                                    )
                                    .clickable { onTagSelected(index) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector        = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint               = Color.White,
                                            modifier           = Modifier.size(13.dp),
                                        )
                                    } else {
                                        Text(
                                            text  = tag.emoji,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                    Text(
                                        text       = tag.label,
                                        style      = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color      = textColor,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Legenda ──────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value         = caption,
                        onValueChange = { if (it.length <= 140) caption = it },
                        label         = { Text("O que foi esse momento?") },
                        placeholder   = {
                            Text(
                                "Conta um pouco sobre esta memória…",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.38f),
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(16.dp),
                        minLines        = 2,
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            focusedLabelColor  = OrangePrimary,
                            cursorColor        = OrangePrimary,
                        ),
                    )
                    // Barra de progresso do contador de caracteres
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            progress   = captionProgress,
                            modifier   = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50)),
                            color      = when {
                                captionProgress > 0.85f -> MaterialTheme.colorScheme.error
                                captionProgress > 0.60f -> Color(0xFFFF9800)
                                else                    -> OrangePrimary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap  = StrokeCap.Round,
                        )
                        Text(
                            text  = "${caption.length}/140",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                captionProgress > 0.85f -> MaterialTheme.colorScheme.error
                                else                    -> MaterialTheme.colorScheme.onSurface.copy(0.50f)
                            },
                        )
                    }
                }

                // ── Seletor de pet (só quando > 1 pet) ───────────────────────
                when {
                    pets.size > 1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text       = "Qual pet é esse?",
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onBackground,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement   = Arrangement.spacedBy(8.dp),
                            ) {
                                pets.forEach { pet ->
                                    val isSelected = pet.id == selectedPetId
                                    val bgColor by animateColorAsState(
                                        targetValue   = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        animationSpec = tween(200),
                                        label         = "pet_bg_${pet.id}",
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(bgColor)
                                            .clickable { selectedPetId = pet.id }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    ) {
                                        Text(
                                            text       = pet.name,
                                            style      = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color      = if (isSelected) Color.White
                                                         else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    pets.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(12.dp),
                        ) {
                            Text(
                                text  = "⚠️ Cadastre um pet antes de adicionar uma entrada no Diário.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                    // pets.size == 1: selecionado automaticamente — não exibe chips
                }

                // ── Botão Salvar ──────────────────────────────────────────────
                val saveScale by animateFloatAsState(
                    targetValue   = if (isSaving) 0.96f else 1f,
                    animationSpec = tween(150),
                    label         = "save_scale",
                )
                Button(
                    onClick = {
                        val petId = selectedPetId ?: return@Button
                        isSaving = true
                        scope.launch {
                            try {
                                val photoPath = withContext(Dispatchers.IO) {
                                    val bitmap = loadBitmapRespectingExif(context, imageUri)
                                        ?: error("Não foi possível carregar a foto")
                                    saveDiaryPhotoJpeg(context, bitmap)
                                }
                                onSave(petId, photoPath, caption.trim())
                            } catch (_: Exception) {
                                isSaving = false
                                onDismiss()
                            }
                        }
                    },
                    enabled  = !isSaving && pets.isNotEmpty(),
                    colors   = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape    = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .graphicsLayer(scaleX = saveScale, scaleY = saveScale),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text       = "💾  Salvar memória",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
