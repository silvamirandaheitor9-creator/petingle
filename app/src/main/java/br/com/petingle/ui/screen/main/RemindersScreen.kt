package br.com.petingle.ui.screen.main

import kotlinx.collections.immutable.ImmutableList
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.petingle.R
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.data.db.entity.Reminder
import br.com.petingle.ui.theme.OrangePrimary
import br.com.petingle.ui.viewmodel.ReminderGroup
import br.com.petingle.ui.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Ponto de entrada da aba Lembretes (SPEC §10 — Parte 1) ──────────────────

@Composable
fun RemindersScreen(
    viewModel: ReminderViewModel = hiltViewModel(),
    onNavigateToNewReminder: (reminderId: Long) -> Unit = {},
) {
    val grouped by viewModel.groupedReminders.collectAsState()
    val pets by viewModel.pets.collectAsState()
    val selectedPetId by viewModel.selectedPetId.collectAsState()
    val historicoExpanded by viewModel.historicoExpanded.collectAsState()

    val isEmpty = grouped.isEmpty()
    val selectedPetName = pets.firstOrNull { it.id == selectedPetId }?.name

    if (isEmpty) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (pets.size > 1) {
                PetFilterChips(
                    pets = pets,
                    selectedPetId = selectedPetId,
                    onSelectPet = { viewModel.selectPet(it) },
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                ReminderEmptyState(
                    selectedPetName = selectedPetName,
                    onClearFilter = if (selectedPetId != null) {
                        { viewModel.selectPet(null) }
                    } else {
                        null
                    },
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            // ── Chips de filtro por pet ────────────────────────────────────────
            if (pets.size > 1) {
                item(key = "pet_filter") {
                    PetFilterChips(
                        pets = pets,
                        selectedPetId = selectedPetId,
                        onSelectPet = { viewModel.selectPet(it) },
                    )
                }
            }

            // ── Seções: Hoje / Amanhã / Esta semana ───────────────────────────
            for (group in listOf(
                ReminderGroup.HOJE,
                ReminderGroup.AMANHA,
                ReminderGroup.ESTA_SEMANA,
            )) {
                val items = grouped[group] ?: continue

                item(key = "header_${group.name}") {
                    SectionHeader(label = group.label())
                }

                // Cards de lembrete com stagger escalonado por grupo
                itemsIndexed(items, key = { _, r -> "reminder_${r.id}" }) { index, reminder ->
                    var shown by remember(reminder.id) { mutableStateOf(false) }
                    LaunchedEffect(reminder.id) {
                        delay((index * 60L).coerceAtMost(360L))
                        shown = true
                    }
                    AnimatedVisibility(
                        visible = shown,
                        enter   = fadeIn(tween(280)) + slideInVertically(
                            animationSpec  = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness    = Spring.StiffnessMediumLow,
                            ),
                            initialOffsetY = { it / 4 },
                        ),
                    ) {
                        ReminderCard(
                            reminder          = reminder,
                            pets              = pets,
                            onEdit            = { onNavigateToNewReminder(reminder.id) },
                            onDelete          = { viewModel.deleteReminder(reminder) },
                            onToggleCompleted = { viewModel.toggleCompleted(reminder) },
                        )
                    }
                }
            }

            // ── Histórico (recolhível) ─────────────────────────────────────────
            val historico = grouped[ReminderGroup.HISTORICO]
            if (!historico.isNullOrEmpty()) {
                item(key = "header_historico") {
                    HistoricoHeader(
                        count = historico.size,
                        expanded = historicoExpanded,
                        onToggle = { viewModel.toggleHistorico() },
                    )
                }

                item(key = "historico_content") {
                    AnimatedVisibility(
                        visible = historicoExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column {
                            historico.forEach { reminder ->
                                ReminderCard(
                                    reminder          = reminder,
                                    pets              = pets,
                                    onEdit            = { onNavigateToNewReminder(reminder.id) },
                                    onDelete          = { viewModel.deleteReminder(reminder) },
                                    onToggleCompleted = { viewModel.toggleCompleted(reminder) },
                                    isHistorico       = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Estado vazio ─────────────────────────────────────────────────────────────

@Composable
private fun ReminderEmptyState(
    selectedPetName: String?,
    onClearFilter: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.vazio_lembretes),
                contentDescription = "Nenhum lembrete",
                modifier = Modifier.size(220.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = if (selectedPetName != null) {
                "Nenhum lembrete para $selectedPetName"
            } else {
                "Nenhum lembrete por aqui ainda"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (selectedPetName != null) {
                "Os lembretes dos outros pets continuam salvos. Volte para “Todos” para visualizá-los."
            } else {
                "Toque no + para criar o primeiro lembrete para seus pets."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
        if (onClearFilter != null) {
            TextButton(onClick = onClearFilter) {
                Text("Ver todos os lembretes")
            }
        }
    }
}

// ─── Chips de filtro por pet ──────────────────────────────────────────────────

@Composable
private fun PetFilterChips(
    pets: ImmutableList<Pet>,
    selectedPetId: Long?,
    onSelectPet: (Long?) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedPetId == null,
                onClick = { onSelectPet(null) },
                label = { Text("Todos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary,
                    selectedLabelColor = Color.White,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedPetId == null,
                    selectedBorderColor = OrangePrimary,
                ),
            )
        }
        items(pets, key = { it.id }) { pet ->
            FilterChip(
                selected = selectedPetId == pet.id,
                onClick = { onSelectPet(pet.id) },
                label = { Text(pet.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary,
                    selectedLabelColor = Color.White,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedPetId == pet.id,
                    selectedBorderColor = OrangePrimary,
                ),
            )
        }
    }
}

// ─── Cabeçalho de seção ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = OrangePrimary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

// ─── Cabeçalho do Histórico (recolhível) ─────────────────────────────────────

@Composable
private fun HistoricoHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Histórico ($count)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Recolher histórico" else "Expandir histórico",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
    }
}

// ─── Card de lembrete ─────────────────────────────────────────────────────────

@Composable
private fun ReminderCard(
    reminder: Reminder,
    pets: ImmutableList<Pet>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleCompleted: () -> Unit,
    isHistorico: Boolean = false,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val petName = pets.find { it.id == reminder.petId }?.name ?: "Pet"
    // Formata a data uma única vez por mudança de timestamp — sem alocar SimpleDateFormat por recomposição.
    val dateStr = remember(reminder.dateTimeMillis) {
        SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
            .format(Date(reminder.dateTimeMillis))
    }

    // ── Animação: check desenhado (10.17) ─────────────────────────────────────
    val checkProgress = remember { Animatable(if (reminder.isCompleted) 1f else 0f) }

    // Pré-computa path, comprimento e intervalos fora do lambda de desenho do Canvas.
    // Sem isso: Path + PathMeasure + floatArrayOf são alocados 60 × por segundo durante a animação.
    val density = LocalDensity.current
    val checkPath = remember(density) {
        val s = with(density) { 26.dp.toPx() }
        androidx.compose.ui.graphics.Path().apply {
            moveTo(s * 0.18f, s * 0.52f)
            lineTo(s * 0.42f, s * 0.74f)
            lineTo(s * 0.82f, s * 0.26f)
        }
    }
    val pathLength = remember(checkPath) {
        android.graphics.PathMeasure(checkPath.asAndroidPath(), false).length
    }
    val dashIntervals = remember(pathLength) { floatArrayOf(pathLength, pathLength) }
    LaunchedEffect(reminder.isCompleted) {
        checkProgress.animateTo(
            targetValue    = if (reminder.isCompleted) 1f else 0f,
            animationSpec  = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ícone da categoria + check animado (10.17)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (isHistorico || reminder.isCompleted)
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                        else
                            OrangePrimary.copy(alpha = 0.12f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(reminder.category.toCategoryDrawable()),
                    contentDescription = reminder.category,
                    modifier = Modifier
                        .size(26.dp)
                        .alpha(1f - checkProgress.value),
                    contentScale = ContentScale.Fit,
                )
                // Traço do checkmark sendo desenhado conforme progress 0 → 1.
                // checkPath, pathLength e dashIntervals são pré-computados fora do lambda
                // de desenho — só a phase muda a cada frame (zero alocações por frame).
                Canvas(modifier = Modifier.size(26.dp)) {
                    if (checkProgress.value > 0f) {
                        drawPath(
                            path  = checkPath,
                            color = Color(0xFF4CAF50),
                            style = Stroke(
                                width      = 3.dp.toPx(),
                                cap        = StrokeCap.Round,
                                join       = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = dashIntervals,
                                    phase     = pathLength * (1f - checkProgress.value),
                                ),
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Conteúdo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (reminder.isCompleted)
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                    else
                        MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "🐾 $petName  •  $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
                if (reminder.recurrence != "none") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = OrangePrimary.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = reminder.recurrence.toRecurrenceLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = OrangePrimary.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Ações
            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = onToggleCompleted,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = if (reminder.isCompleted) "Marcar como pendente" else "Marcar como concluído",
                        modifier = Modifier.size(20.dp),
                        tint = if (reminder.isCompleted)
                            Color(0xFF4CAF50)
                        else
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Editar",
                        modifier = Modifier.size(18.dp),
                        tint = OrangePrimary.copy(alpha = 0.75f),
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Excluir",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir lembrete?") },
            text = { Text("O lembrete \"${reminder.title}\" será removido permanentemente.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun ReminderGroup.label(): String = when (this) {
    ReminderGroup.HOJE -> "Hoje"
    ReminderGroup.AMANHA -> "Amanhã"
    ReminderGroup.ESTA_SEMANA -> "Próximos"
    ReminderGroup.HISTORICO -> "Histórico"
}

@DrawableRes
fun String.toCategoryDrawable(): Int = when (this) {
    "vacina"       -> R.drawable.icone_vacina
    "consulta"     -> R.drawable.icone_consulta
    "banho"        -> R.drawable.icone_banho
    "medicacao"    -> R.drawable.icone_medicacao
    "alimentacao"  -> R.drawable.icone_alimentacao
    "vermifugo"    -> R.drawable.icone_vermifugo
    else           -> R.drawable.icone_personalizado
}

fun String.toRecurrenceLabel(): String = when (this) {
    "daily"   -> "Repete diariamente"
    "weekly"  -> "Repete semanalmente"
    "monthly" -> "Repete mensalmente"
    else      -> ""
}

