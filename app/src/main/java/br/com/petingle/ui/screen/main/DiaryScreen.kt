package br.com.petingle.ui.screen.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import br.com.petingle.R
import br.com.petingle.data.db.entity.DiaryEntry
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.ui.theme.OrangePrimary
import br.com.petingle.ui.viewmodel.DiaryViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─── Ponto de entrada da aba Diário ──────────────────────────────────────────

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel = hiltViewModel(),
    showAddEntryPlaceholder: Boolean = false,
    onDismissAddEntryPlaceholder: () -> Unit = {},
    onNavigateToPhotoEditor: (Uri) -> Unit = {},
    onEditEntry: (Long) -> Unit = {},
) {
    val entriesState = viewModel.entries.collectAsState()
    val petsState    = viewModel.pets.collectAsState()
    val entries by entriesState
    val pets    by petsState

    // ── Fluxo do "+": galeria → editor de fotos ───────────────────────────────
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onNavigateToPhotoEditor(uri)
        onDismissAddEntryPlaceholder()
    }
    LaunchedEffect(showAddEntryPlaceholder) {
        if (showAddEntryPlaceholder) {
            pickImageLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                ),
            )
        }
    }

    var selectedPetId by rememberSaveable { mutableStateOf<Long?>(null) }

    val visibleEntries by remember {
        derivedStateOf {
            val e = entriesState.value
            if (selectedPetId == null) e else e.filter { it.petId == selectedPetId }
        }
    }

    var entryPendingDelete by remember { mutableStateOf<DiaryEntry?>(null) }

    val hasLoadedOnce  = remember { mutableStateOf(false) }
    val knownEntryIds  = remember { mutableSetOf<Long>() }
    LaunchedEffect(entries) {
        if (!hasLoadedOnce.value) {
            knownEntryIds += entries.map { it.id }
            hasLoadedOnce.value = true
        }
    }

    if (entries.isEmpty()) {
        EmptyDiarySection()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        ) {
            // Header com contador de memórias
            item(key = "memories_header") {
                MemoriesHeader(count = entries.size)
            }

            if (pets.size > 1) {
                item(key = "pet_filter") {
                    PetFilterRow(
                        pets          = pets,
                        selectedPetId = selectedPetId,
                        onSelect      = { selectedPetId = it },
                    )
                }
                item(key = "filter_spacer") { Spacer(Modifier.height(8.dp)) }
            }

            // Entradas do diário com stagger na carga inicial e PolaroidReveal para novas
            itemsIndexed(visibleEntries, key = { _, e -> e.id }) { index, entry ->
                val pet    = pets.firstOrNull { it.id == entry.petId }
                val isNew  = hasLoadedOnce.value && entry.id !in knownEntryIds
                // Entradas novas: visíveis imediatamente (PolaroidReveal cuida da animação)
                // Entradas da carga inicial: aparecem em stagger
                var shown by remember(entry.id) { mutableStateOf(isNew) }
                LaunchedEffect(entry.id) {
                    if (!shown) {
                        delay((index * 55L).coerceAtMost(380L))
                        shown = true
                    }
                }
                AnimatedVisibility(
                    visible = shown,
                    enter   = fadeIn(tween(320)) + slideInVertically(tween(340)) { it / 5 },
                ) {
                    PolaroidReveal(
                        entryId       = entry.id,
                        hasLoadedOnce = hasLoadedOnce.value,
                        knownEntryIds = knownEntryIds,
                    ) {
                        DiaryEntryCard(
                            entry           = entry,
                            petName         = pet?.name ?: "Pet",
                            onDeleteRequest = { entryPendingDelete = entry },
                            onEditRequest   = { onEditEntry(entry.id) },
                            modifier        = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            item(key = "bottom_spacer") { Spacer(Modifier.height(20.dp)) }
        }
    }

    // ── Confirmação de exclusão ───────────────────────────────────────────────
    entryPendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text       = "Remover esta memória?",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text("Esta foto e legenda serão removidas do diário para sempre. Essa ação não pode ser desfeita.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        entryPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                    shape = RoundedCornerShape(50),
                ) {
                    Text("Remover", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

// ─── Header de memórias ───────────────────────────────────────────────────────

@Composable
private fun MemoriesHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(OrangePrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Rounded.PhotoAlbum,
                contentDescription = null,
                tint               = OrangePrimary,
                modifier           = Modifier.size(18.dp),
            )
        }
        Text(
            text       = if (count == 1) "1 memória guardada" else "$count memórias guardadas",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ─── Filtro por pet ───────────────────────────────────────────────────────────

@Composable
private fun PetFilterRow(
    pets: ImmutableList<Pet>,
    selectedPetId: Long?,
    onSelect: (Long?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding        = PaddingValues(horizontal = 16.dp),
    ) {
        item {
            FilterChip(
                selected = selectedPetId == null,
                onClick  = { onSelect(null) },
                label    = { Text("Todos") },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary.copy(alpha = 0.16f),
                    selectedLabelColor     = OrangePrimary,
                ),
            )
        }
        items(pets, key = { it.id }) { pet ->
            FilterChip(
                selected = selectedPetId == pet.id,
                onClick  = { onSelect(pet.id) },
                label    = { Text(pet.name) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary.copy(alpha = 0.16f),
                    selectedLabelColor     = OrangePrimary,
                ),
            )
        }
    }
}

// ─── Card de entrada no diário (redesenhado) ──────────────────────────────────

@Composable
private fun DiaryEntryCard(
    entry: DiaryEntry,
    petName: String,
    onDeleteRequest: () -> Unit,
    onEditRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context     = LocalContext.current
    val friendlyDate = remember(entry.dateMillis) { entry.dateMillis.toFriendlyDate() }
    val fullDateStr  = remember(entry.dateMillis) { entry.dateMillis.toDiaryDate() }

    val imageRequest = remember(entry.photoPath) {
        ImageRequest.Builder(context)
            .data(if (entry.photoPath.isNotEmpty()) File(entry.photoPath) else null)
            .memoryCacheKey(entry.photoPath)
            .diskCacheKey(entry.photoPath)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .size(480) // 720 → 480: suficiente para 260dp, reduz ~40% de memória por entry
            .scale(Scale.FILL)
            .crossfade(true)
            .build()
    }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            // ── Foto com overlays (pet badge + data) ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            ) {
                AsyncImage(
                    model              = imageRequest,
                    contentDescription = "Foto de $petName",
                    contentScale       = ContentScale.Crop,
                    fallback           = painterResource(R.drawable.avatar_pet_padrao),
                    error              = painterResource(R.drawable.avatar_pet_padrao),
                    placeholder        = painterResource(R.drawable.avatar_pet_padrao),
                    modifier           = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                )

                // Gradiente inferior para legibilidade dos badges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                            ),
                        ),
                )

                // Badge do nome do pet (canto inferior esquerdo)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(OrangePrimary, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Pets,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(12.dp),
                    )
                    Text(
                        text       = petName,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                }

                // Badge da data amigável (canto superior direito)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text  = friendlyDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }

            // ── Legenda (se houver) ────────────────────────────────────────────
            if (entry.caption.isNotBlank()) {
                Text(
                    text     = "\"${entry.caption.take(140)}\"",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            // ── Linha separadora sutil ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
            )

            // ── Barra de ações ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Data completa (legenda secundária)
                Text(
                    text      = fullDateStr,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                    modifier  = Modifier.weight(1f),
                )

                // Editar
                IconButton(
                    onClick  = onEditRequest,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Edit,
                        contentDescription = "Editar legenda",
                        tint               = OrangePrimary.copy(alpha = 0.82f),
                        modifier           = Modifier.size(20.dp),
                    )
                }

                // Excluir
                IconButton(
                    onClick  = onDeleteRequest,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Delete,
                        contentDescription = "Excluir",
                        tint               = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                        modifier           = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ─── Animação polaroid ao adicionar entrada ───────────────────────────────────

@Composable
private fun PolaroidReveal(
    entryId: Long,
    hasLoadedOnce: Boolean,
    knownEntryIds: MutableSet<Long>,
    content: @Composable () -> Unit,
) {
    val isNewEntry = remember(entryId) {
        val isNew = hasLoadedOnce && entryId !in knownEntryIds
        knownEntryIds += entryId
        isNew
    }

    val rotation = remember(entryId) { Animatable(if (isNewEntry) -9f else 0f) }
    val scale    = remember(entryId) { Animatable(if (isNewEntry) 0.82f else 1f) }

    LaunchedEffect(entryId) {
        if (isNewEntry) {
            launch {
                rotation.animateTo(
                    0f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                )
            }
            launch {
                scale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                )
            }
        }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            rotationZ = rotation.value
            scaleX    = scale.value
            scaleY    = scale.value
        },
    ) {
        content()
    }
}

// ─── Estado vazio ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyDiarySection() {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter            = painterResource(R.drawable.vazio_diario),
                contentDescription = null,
                modifier           = Modifier.fillMaxWidth(0.62f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = "Seu diário de memórias está em branco",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
                textAlign  = TextAlign.Center,
            )
            Text(
                text      = "Toque no botão + para guardar o primeiro momento especial com seu pet.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Helpers privados ─────────────────────────────────────────────────────────

private val DIARY_DATE_SDF      = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
private val DIARY_WEEKDAY_SDF   = SimpleDateFormat("EEEE", Locale("pt", "BR"))

private fun Long.toDiaryDate(): String = DIARY_DATE_SDF.format(Date(this))

/**
 * Retorna label amigável:
 * - "Hoje" / "Ontem" para as duas últimas datas
 * - Nome do dia da semana para os últimos 7 dias
 * - dd/MM/yyyy para datas mais antigas
 */
private fun Long.toFriendlyDate(): String {
    val now   = Calendar.getInstance()
    val entry = Calendar.getInstance().apply { timeInMillis = this@toFriendlyDate }

    val sameYear  = now.get(Calendar.YEAR) == entry.get(Calendar.YEAR)
    val daysDiff  = ((now.timeInMillis - this) / (24L * 60 * 60 * 1000)).toInt()

    return when {
        sameYear && now.get(Calendar.DAY_OF_YEAR) == entry.get(Calendar.DAY_OF_YEAR) -> "Hoje"
        sameYear && daysDiff == 1 -> "Ontem"
        daysDiff < 7              -> DIARY_WEEKDAY_SDF.format(Date(this))
            .replaceFirstChar { it.uppercaseChar() }
        else                      -> toDiaryDate()
    }
}
