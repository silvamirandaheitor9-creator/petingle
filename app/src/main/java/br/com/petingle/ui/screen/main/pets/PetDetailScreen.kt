package br.com.petingle.ui.screen.main.pets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import br.com.petingle.ui.screen.main.speciesIconRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import br.com.petingle.R
import br.com.petingle.data.db.entity.HealthRecord
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.ui.theme.OrangeGradEnd
import br.com.petingle.ui.theme.OrangeGradStart
import br.com.petingle.ui.theme.OrangePrimary
import br.com.petingle.ui.viewmodel.PetDetailViewModel
import br.com.petingle.util.DateUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import java.io.File

// ─── Sub-abas de saúde do pet — Seção 12, Partes 1, 2 e 3 ───────────────────

private enum class HealthTab(val label: String, val iconRes: Int) {
    VACCINES("Vacinas", R.drawable.icone_vacina),
    MEDICATIONS("Medicamentos", R.drawable.icone_medicacao),
    CONSULTATIONS("Consultas", R.drawable.icone_consulta),
    WEIGHT("Peso", R.drawable.vazio_peso),
    FEEDING("Alimentação", R.drawable.icone_alimentacao),
}

// ─── Ponto de entrada da tela de detalhe do pet ──────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    viewModel: PetDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val pet by viewModel.pet.collectAsState()
    val vaccines by viewModel.vaccines.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val consultations by viewModel.consultations.collectAsState()
    val weights  by viewModel.weights.collectAsState()
    val feedings by viewModel.feedings.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val currentTab = HealthTab.entries[selectedTabIndex]

    // Controle do BottomSheet de adicionar registro
    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Scaffold(
        topBar = {
            PetDetailTopBar(
                pet    = pet,
                onBack = onBack,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = OrangePrimary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 1.dp,
                ),
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Adicionar registro",
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ── TabRow das sub-abas (scrollável + icons) ────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = OrangePrimary,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        height = 3.dp,
                        color = OrangePrimary,
                    )
                },
            ) {
                HealthTab.entries.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        modifier = Modifier.padding(horizontal = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Image(
                                painter = painterResource(tab.iconRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(15.dp)
                                    .alpha(if (isSelected) 1f else 0.45f),
                            )
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) OrangePrimary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                            )
                        }
                    }
                }
            }

            // ── Conteúdo da aba selecionada ──────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab) {
                    HealthTab.VACCINES ->
                        VacinasContent(
                            records = vaccines,
                            onDelete = { viewModel.deleteRecord(it) },
                        )
                    HealthTab.MEDICATIONS ->
                        MedicamentosContent(
                            records = medications,
                            onDelete = { viewModel.deleteRecord(it) },
                        )
                    HealthTab.CONSULTATIONS ->
                        ConsultasContent(
                            records = consultations,
                            onDelete = { viewModel.deleteRecord(it) },
                        )
                    HealthTab.WEIGHT ->
                        WeightContent(
                            records = weights,
                            onDelete = { viewModel.deleteRecord(it) },
                        )
                    HealthTab.FEEDING ->
                        FeedingContent(
                            records = feedings,
                            onDelete = { viewModel.deleteRecord(it) },
                        )
                }
            }
        }
    }

    // ── BottomSheet de novo registro ─────────────────────────────────────────
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            when (currentTab) {
                HealthTab.VACCINES ->
                    AddVaccineForm(
                        petId = viewModel.petId,
                        onSave = { record ->
                            viewModel.insertRecord(record)
                            showAddSheet = false
                        },
                        onDismiss = { showAddSheet = false },
                    )
                HealthTab.MEDICATIONS ->
                    AddMedicationForm(
                        petId = viewModel.petId,
                        onSave = { record ->
                            viewModel.insertRecord(record)
                            showAddSheet = false
                        },
                        onDismiss = { showAddSheet = false },
                    )
                HealthTab.CONSULTATIONS ->
                    AddConsultationForm(
                        petId = viewModel.petId,
                        onSave = { record ->
                            viewModel.insertRecord(record)
                            showAddSheet = false
                        },
                        onDismiss = { showAddSheet = false },
                    )
                HealthTab.WEIGHT ->
                    AddWeightForm(
                        petId = viewModel.petId,
                        onSave = { record ->
                            viewModel.insertRecord(record)
                            showAddSheet = false
                        },
                        onDismiss = { showAddSheet = false },
                    )
                HealthTab.FEEDING ->
                    AddFeedingForm(
                        petId = viewModel.petId,
                        onSave = { record ->
                            viewModel.insertRecord(record)
                            showAddSheet = false
                        },
                        onDismiss = { showAddSheet = false },
                    )
            }
        }
    }
}

// ─── Cabeçalho da tela de detalhe ────────────────────────────────────────────

@Composable
private fun PetDetailTopBar(
    pet: Pet?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val hasRealPhoto = remember(pet?.photoPath) {
        pet?.photoPath?.isNotEmpty() == true && File(pet.photoPath).exists()
    }
    val speciesIcon = remember(pet?.species) { speciesIconRes(pet?.species ?: "") }
    val ageLabel = remember(pet) {
        if (pet == null) return@remember ""
        petAgeLabel(pet)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(OrangeGradStart, OrangeGradEnd)))
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Linha do botão voltar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White,
                )
            }
        }

        // Foto circular grande
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            if (hasRealPhoto) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(pet!!.photoPath))
                        .size(160)
                        .scale(Scale.FILL)
                        .crossfade(true)
                        .build(),
                    contentDescription = pet.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            } else {
                Image(
                    painter = painterResource(speciesIcon),
                    contentDescription = pet?.species,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Nome do pet
        Text(
            text = pet?.name ?: "",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 48.dp),
        )

        Spacer(Modifier.height(6.dp))

        // Chips de informação: espécie, sexo, idade
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pet != null) {
                PetInfoChipDetail(pet.species.replaceFirstChar { it.uppercaseChar() })
                if (pet.sex.isNotBlank()) {
                    PetInfoChipDetail(pet.sex)
                }
                if (pet.isCastrated) {
                    PetInfoChipDetail("Castrado/a")
                }
                if (ageLabel.isNotBlank()) {
                    PetInfoChipDetail(ageLabel)
                }
            }
        }
    }
}

@Composable
private fun PetInfoChipDetail(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.92f),
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private fun petAgeLabel(pet: Pet): String {
    if (pet.birthDate.isNotBlank()) {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val bd = sdf.parse(pet.birthDate) ?: return pet.approximateAge
            val now = java.util.Calendar.getInstance()
            val birth = java.util.Calendar.getInstance().apply { time = bd }
            val totalMonths =
                (now.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)) * 12 +
                    now.get(java.util.Calendar.MONTH) - birth.get(java.util.Calendar.MONTH)
            when {
                totalMonths < 1  -> "Filhote"
                totalMonths < 12 -> "$totalMonths ${if (totalMonths == 1) "mês" else "meses"}"
                else -> {
                    val years = totalMonths / 12
                    val months = totalMonths % 12
                    buildString {
                        append("$years ${if (years == 1) "ano" else "anos"}")
                        if (months > 0) append(" e $months ${if (months == 1) "mês" else "meses"}")
                    }
                }
            }
        } catch (e: Exception) {
            pet.approximateAge
        }
    }
    return pet.approximateAge
}

// ════════════════════════════════════════════════════════════════════════════
// SUB-ABA: VACINAS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun VacinasContent(
    records: ImmutableList<HealthRecord>,
    onDelete: (HealthRecord) -> Unit,
) {
    if (records.isEmpty()) {
        HealthEmptyState(
            imageRes = R.drawable.vazio_vacinas,
            title = "Nenhuma vacina registrada",
            message = "Adicione a primeira vacina do seu pet para acompanhar o histórico de imunização.",
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(records, key = { _, r -> r.id }) { index, record ->
                StaggeredHealthItem(index = index) {
                    VaccineCard(record = record, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
private fun VaccineCard(record: HealthRecord, onDelete: (HealthRecord) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    HealthRecordCard(
        iconRes = R.drawable.icone_vacina,
        accentColor = Color(0xFF4CAF50),
        onDeleteClick = { showDeleteDialog = true },
    ) {
        Text(
            text = record.vaccineName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val dateAndLot = buildString {
            append(DateUtils.utcMillisToDisplayDate(record.dateMillis))
            if (record.vaccineLot.isNotBlank()) append(" · Lote: ${record.vaccineLot}")
        }
        Text(
            text = dateAndLot,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
        if (record.nextDoseDate.isNotBlank()) {
            Text(
                text = "Próxima dose: ${record.nextDoseDate}",
                style = MaterialTheme.typography.bodySmall,
                color = OrangePrimary,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            message = "Remover a vacina \"${record.vaccineName}\"? Essa ação não pode ser desfeita.",
            onConfirm = {
                onDelete(record)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SUB-ABA: MEDICAMENTOS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun MedicamentosContent(
    records: ImmutableList<HealthRecord>,
    onDelete: (HealthRecord) -> Unit,
) {
    if (records.isEmpty()) {
        HealthEmptyState(
            imageRes = R.drawable.vazio_medicamentos,
            title = "Nenhum medicamento registrado",
            message = "Registre os medicamentos do seu pet para acompanhar os tratamentos em andamento.",
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(records, key = { _, r -> r.id }) { index, record ->
                StaggeredHealthItem(index = index) {
                    MedicationCard(record = record, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
private fun MedicationCard(record: HealthRecord, onDelete: (HealthRecord) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    HealthRecordCard(
        iconRes = R.drawable.icone_medicacao,
        accentColor = Color(0xFF9C27B0),
        onDeleteClick = { showDeleteDialog = true },
    ) {
        Text(
            text = record.medicationName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val dosageFreq = buildString {
            if (record.medicationDosage.isNotBlank()) append(record.medicationDosage)
            if (record.medicationFrequency.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(record.medicationFrequency)
            }
        }
        if (dosageFreq.isNotBlank()) {
            Text(
                text = dosageFreq,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
        val durationAndDate = buildString {
            if (record.medicationDurationDays > 0) append("Duração: ${record.medicationDurationDays} dia(s)")
            append(if (isNotEmpty()) " · " else "")
            append(DateUtils.utcMillisToDisplayDate(record.dateMillis))
        }
        Text(
            text = durationAndDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            message = "Remover o medicamento \"${record.medicationName}\"? Essa ação não pode ser desfeita.",
            onConfirm = {
                onDelete(record)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SUB-ABA: CONSULTAS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun ConsultasContent(
    records: ImmutableList<HealthRecord>,
    onDelete: (HealthRecord) -> Unit,
) {
    if (records.isEmpty()) {
        HealthEmptyState(
            imageRes = R.drawable.vazio_consultas,
            title = "Nenhuma consulta registrada",
            message = "Registre as consultas veterinárias do seu pet para manter o histórico de saúde completo.",
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(records, key = { _, r -> r.id }) { index, record ->
                StaggeredHealthItem(index = index) {
                    ConsultationCard(record = record, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
private fun ConsultationCard(record: HealthRecord, onDelete: (HealthRecord) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    HealthRecordCard(
        iconRes = R.drawable.icone_consulta,
        accentColor = Color(0xFF2196F3),
        onDeleteClick = { showDeleteDialog = true },
    ) {
        Text(
            text = record.consultationReason.ifBlank { "Consulta" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = DateUtils.utcMillisToDisplayDate(record.dateMillis),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
        )
        if (record.diagnosis.isNotBlank()) {
            Text(
                text = "Diagnóstico: ${record.diagnosis}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (record.vetInstructions.isNotBlank()) {
            Text(
                text = "Orientações: ${record.vetInstructions}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            message = "Remover esta consulta de \"${DateUtils.utcMillisToDisplayDate(record.dateMillis)}\"? Essa ação não pode ser desfeita.",
            onConfirm = {
                onDelete(record)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// COMPONENTES COMPARTILHADOS
// ════════════════════════════════════════════════════════════════════════════

// ─── Card base de registro de saúde ─────────────────────────────────────────

@Composable
private fun HealthRecordCard(
    iconRes: Int,
    accentColor: Color,
    onDeleteClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top,
        ) {
            // Barra de acento colorida lateral esquerda
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        accentColor,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    ),
            )

            // Badge do ícone da categoria
            Box(
                modifier = Modifier
                    .padding(start = 12.dp, top = 14.dp, bottom = 14.dp)
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accentColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                )
            }

            // Conteúdo textual
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                content()
            }

            // Botão deletar
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .padding(end = 2.dp, top = 2.dp)
                    .size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Remover",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ─── Animação de entrada escalonada (mesmo padrão da aba Meus Pets) ──────────

@Composable
private fun StaggeredHealthItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember(index) { mutableStateOf(false) }

    LaunchedEffect(index) {
        delay((index * 55L).coerceAtMost(380L))
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(260)) +
            slideInVertically(
                animationSpec = tween(260),
                initialOffsetY = { it / 5 },
            ),
    ) {
        content()
    }
}

// ─── Estado vazio genérico das sub-abas ──────────────────────────────────────

@Composable
private fun HealthEmptyState(
    imageRes: Int,
    title: String,
    message: String,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.58f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Header estilizado para os formulários de novo registro ──────────────────

@Composable
private fun FormHeader(iconRes: Int, title: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(OrangeGradStart, OrangeGradEnd)))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
    Spacer(Modifier.height(4.dp))
}

// ─── Diálogo de confirmação de exclusão ──────────────────────────────────────

@Composable
private fun DeleteConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Confirmar exclusão", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text("Remover", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

// ════════════════════════════════════════════════════════════════════════════
// FORMULÁRIOS DE NOVO REGISTRO (BottomSheet)
// ════════════════════════════════════════════════════════════════════════════

// ─── Formulário: Nova Vacina ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVaccineForm(
    petId: Long,
    onSave: (HealthRecord) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var lot by remember { mutableStateOf("") }
    // Inicializado como meia-noite UTC do dia atual (DatePicker sempre retorna UTC midnight).
    var dateMillis by remember { mutableLongStateOf(DateUtils.localMillisToUtcMidnight(System.currentTimeMillis())) }
    var nextDoseText by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showNextDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateMillis,
    )
    val nextDatePickerState = rememberDatePickerState()

    // Lê Y/M/D no fuso UTC para evitar off-by-one em fusos negativos (ex: UTC-3).
    val dateStr = DateUtils.utcMillisToDisplayDate(dateMillis)
    val isValid = name.isNotBlank()
    // Guard contra duplo-toque: setado antes de chamar onSave; o sheet fecha logo
    // em seguida (onSave → showAddSheet = false), mas dois toques no mesmo frame
    // ainda poderiam disparar o clique duas vezes sem este flag.
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.78f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormHeader(iconRes = R.drawable.icone_vacina, title = "Nova Vacina", accentColor = Color(0xFF4CAF50))

        // Nome da vacina
        HealthTextField(
            value = name,
            onValueChange = { name = it },
            label = "Nome da vacina *",
            placeholder = "Ex: V10, Antirrábica",
        )

        // Data de aplicação
        HealthDateField(
            label = "Data de aplicação *",
            dateStr = dateStr,
            onClick = { showDatePicker = true },
        )

        // Lote (opcional)
        HealthTextField(
            value = lot,
            onValueChange = { lot = it },
            label = "Lote (opcional)",
            placeholder = "Ex: AB1234",
        )

        // Data da próxima dose (opcional)
        HealthDateField(
            label = "Data da próxima dose (opcional)",
            dateStr = nextDoseText,
            placeholder = "Selecionar data",
            onClick = { showNextDatePicker = true },
        )

        Spacer(Modifier.height(8.dp))

        // Botão Salvar
        Button(
            onClick = {
                // Guard síncrono contra duplo-toque: isSaving é setado antes de
                // qualquer coroutine; dois toques no mesmo frame são processados
                // sequencialmente na main thread, então o 2º encontra true e sai.
                if (isValid && !isSaving) {
                    isSaving = true
                    onSave(
                        HealthRecord(
                            petId = petId,
                            type = "vaccine",
                            vaccineName = name.trim(),
                            vaccineLot = lot.trim(),
                            nextDoseDate = nextDoseText,
                            dateMillis = dateMillis,
                        )
                    )
                }
            },
            enabled = isValid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
        ) {
            Text("Salvar vacina", fontWeight = FontWeight.Bold, color = Color.White)
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }

    // DatePicker — data de aplicação
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // DatePicker — próxima dose
    if (showNextDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showNextDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    nextDatePickerState.selectedDateMillis?.let {
                        nextDoseText = DateUtils.utcMillisToDisplayDate(it)
                    }
                    showNextDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showNextDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = nextDatePickerState)
        }
    }
}

// ─── Formulário: Novo Medicamento ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMedicationForm(
    petId: Long,
    onSave: (HealthRecord) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var dateMillis by remember { mutableLongStateOf(DateUtils.localMillisToUtcMidnight(System.currentTimeMillis())) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
    val dateStr = DateUtils.utcMillisToDisplayDate(dateMillis)
    val isValid = name.isNotBlank() && dosage.isNotBlank() && frequency.isNotBlank()
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.82f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormHeader(iconRes = R.drawable.icone_medicacao, title = "Novo Medicamento", accentColor = Color(0xFF9C27B0))

        HealthTextField(
            value = name,
            onValueChange = { name = it },
            label = "Nome do medicamento *",
            placeholder = "Ex: Bravecto, Frontline",
        )

        HealthTextField(
            value = dosage,
            onValueChange = { dosage = it },
            label = "Dosagem *",
            placeholder = "Ex: 5mg, 2 gotas, 1 comprimido",
        )

        HealthTextField(
            value = frequency,
            onValueChange = { frequency = it },
            label = "Frequência *",
            placeholder = "Ex: a cada 8h, 1x ao dia",
        )

        HealthTextField(
            value = durationText,
            onValueChange = { durationText = it.filter { c -> c.isDigit() } },
            label = "Duração em dias (opcional)",
            placeholder = "Ex: 7",
            keyboardType = KeyboardType.Number,
        )

        HealthDateField(
            label = "Data de início",
            dateStr = dateStr,
            onClick = { showDatePicker = true },
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (isValid && !isSaving) {
                    isSaving = true
                    onSave(
                        HealthRecord(
                            petId = petId,
                            type = "medication",
                            medicationName = name.trim(),
                            medicationDosage = dosage.trim(),
                            medicationFrequency = frequency.trim(),
                            medicationDurationDays = durationText.toIntOrNull() ?: 0,
                            dateMillis = dateMillis,
                        )
                    )
                }
            },
            enabled = isValid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
        ) {
            Text("Salvar medicamento", fontWeight = FontWeight.Bold, color = Color.White)
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─── Formulário: Nova Consulta ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddConsultationForm(
    petId: Long,
    onSave: (HealthRecord) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var dateMillis by remember { mutableLongStateOf(DateUtils.localMillisToUtcMidnight(System.currentTimeMillis())) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
    val dateStr = DateUtils.utcMillisToDisplayDate(dateMillis)
    val isValid = reason.isNotBlank()
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.82f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormHeader(iconRes = R.drawable.icone_consulta, title = "Nova Consulta", accentColor = Color(0xFF2196F3))

        HealthDateField(
            label = "Data da consulta",
            dateStr = dateStr,
            onClick = { showDatePicker = true },
        )

        HealthTextField(
            value = reason,
            onValueChange = { reason = it },
            label = "Motivo da visita *",
            placeholder = "Ex: Check-up, vacinação, mal-estar",
        )

        HealthTextField(
            value = diagnosis,
            onValueChange = { diagnosis = it },
            label = "Diagnóstico (opcional)",
            placeholder = "Ex: Saudável, Otite, Dermatite",
            singleLine = false,
            minLines = 2,
        )

        HealthTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = "Orientações do veterinário (opcional)",
            placeholder = "Ex: Retorno em 30 dias, evitar banho por 3 dias",
            singleLine = false,
            minLines = 2,
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (isValid && !isSaving) {
                    isSaving = true
                    onSave(
                        HealthRecord(
                            petId = petId,
                            type = "consultation",
                            consultationReason = reason.trim(),
                            diagnosis = diagnosis.trim(),
                            vetInstructions = instructions.trim(),
                            dateMillis = dateMillis,
                        )
                    )
                }
            },
            enabled = isValid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
        ) {
            Text("Salvar consulta", fontWeight = FontWeight.Bold, color = Color.White)
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─── TextField genérico com o estilo do app ───────────────────────────────────

@Composable
private fun HealthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangePrimary,
            focusedLabelColor = OrangePrimary,
            cursorColor = OrangePrimary,
        ),
    )
}

// ─── Campo de data clicável ───────────────────────────────────────────────────
// Usa readOnly=true + MutableInteractionSource para detectar o press e abrir
// o DatePicker. Não usar enabled=false pois bloqueia eventos de pointer.

@Composable
private fun HealthDateField(
    label: String,
    dateStr: String,
    placeholder: String = "",
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) onClick()
    }

    OutlinedTextField(
        value = dateStr,
        onValueChange = {},
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        placeholder = { Text(placeholder.ifBlank { "Selecionar data" }, style = MaterialTheme.typography.bodyMedium) },
        readOnly = true,
        interactionSource = interactionSource,
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = "Selecionar data",
                tint = OrangePrimary,
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangePrimary,
            focusedLabelColor = OrangePrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = OrangePrimary,
        ),
    )
}

// ════════════════════════════════════════════════════════════════════════════
// SUB-ABA: PESO — Seção 12, Parte 2
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun WeightContent(
    records: ImmutableList<HealthRecord>,
    onDelete: (HealthRecord) -> Unit,
) {
    if (records.isEmpty()) {
        HealthEmptyState(
            imageRes = R.drawable.vazio_peso,
            title = "Nenhuma pesagem registrada",
            message = "Registre as pesagens do seu pet para acompanhar a evolução do peso ao longo do tempo.",
        )
        return
    }

    // Ordenação separada para gráfico (ASC = mais antigo à esquerda) e lista (DESC = mais recente primeiro).
    val sortedAsc  = remember(records) { records.sortedBy { it.dateMillis } }
    val sortedDesc = remember(records) { records.sortedByDescending { it.dateMillis } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── Gráfico (≥2 pontos) ou dica de "adicione mais uma pesagem" ──────
        item {
            if (records.size >= 2) {
                WeightLineChart(sortedAsc = sortedAsc)
            } else {
                WeightChartHint()
            }
        }

        // ── Cabeçalho da lista ────────────────────────────────────────────────
        item {
            Text(
                text = "Histórico de pesagens",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }

        // ── Registros — mais recente primeiro ─────────────────────────────────
        itemsIndexed(sortedDesc, key = { _, r -> r.id }) { index, record ->
            StaggeredHealthItem(index = index) {
                WeightCard(record = record, onDelete = onDelete)
            }
        }
    }
}

// ─── Dica para quando há apenas 1 pesagem ────────────────────────────────────

@Composable
private fun WeightChartHint() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OrangePrimary.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Adicione mais uma pesagem para visualizar o gráfico de evolução.",
                style = MaterialTheme.typography.bodySmall,
                color = OrangePrimary,
            )
        }
    }
}

// ─── Card de pesagem ─────────────────────────────────────────────────────────

@Composable
private fun WeightCard(record: HealthRecord, onDelete: (HealthRecord) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Badge laranja com o valor do peso
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrangePrimary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatWeightKg(record.weightKg),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OrangePrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = "kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrangePrimary,
                    )
                }
            }

            // Data + observações
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = DateUtils.utcMillisToDisplayDate(record.dateMillis),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (record.notes.isNotBlank()) {
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Remover",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            message = "Remover a pesagem de ${DateUtils.utcMillisToDisplayDate(record.dateMillis)} " +
                "(${formatWeightKg(record.weightKg)} kg)? Essa ação não pode ser desfeita.",
            onConfirm = {
                onDelete(record)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

// ─── Gráfico de linha — Canvas Compose nativo ────────────────────────────────
// Sem biblioteca externa. Usa bezier cúbico com pontos de controle horizontais
// para suavizar a curva, gradiente de preenchimento abaixo da linha e anéis
// nas anotações de dados. Y-axis: 5 ticks; X-axis: até 4 rótulos de data.

// Recebe List<HealthRecord> porque sortedBy/sortedByDescending retorna List<T>.
// A estabilidade já é garantida pelo WeightContent (que recebe ImmutableList).
@Composable
private fun WeightLineChart(sortedAsc: List<HealthRecord>) {
    val minW = sortedAsc.minOf { it.weightKg }
    val maxW = sortedAsc.maxOf { it.weightKg }
    // Breathing room de 15% acima e abaixo; range mínimo de 0,5 kg
    val spread = (maxW - minW).coerceAtLeast(0.5)
    val visMin = (minW - spread * 0.15).coerceAtLeast(0.0)
    val visMax = maxW + spread * 0.15
    val range  = (visMax - visMin).coerceAtLeast(0.5)

    val orangeColor = OrangePrimary
    val gridColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val labelStyle  = MaterialTheme.typography.labelSmall
    val labelColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Evolução do peso",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                // ── Labels do eixo Y (5 ticks, topo → base = visMax → visMin) ──
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    for (i in 4 downTo 0) {
                        val v = visMin + range * i / 4.0
                        Text(
                            text = formatWeightKg(v),
                            style = labelStyle,
                            color = labelColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                // ── Área do gráfico ────────────────────────────────────────────
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    val w = size.width
                    val h = size.height
                    val n = sortedAsc.size
                    val xStep = if (n > 1) w / (n - 1).toFloat() else w / 2f

                    // Coordenadas em pixels de cada ponto
                    val pts = sortedAsc.mapIndexed { i, r ->
                        val x  = if (n > 1) i * xStep else w / 2f
                        val ny = ((r.weightKg - visMin) / range).toFloat().coerceIn(0f, 1f)
                        Offset(x, h - ny * h)
                    }

                    // Linhas de grade horizontais
                    repeat(5) { i ->
                        val y = h * i / 4f
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end   = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    if (pts.size >= 2) {
                        // Bezier cúbico suavizado (pontos de controle horizontais)
                        val linePath = Path()
                        linePath.moveTo(pts[0].x, pts[0].y)
                        for (i in 1 until pts.size) {
                            val prev = pts[i - 1]
                            val curr = pts[i]
                            val dx   = (curr.x - prev.x) / 3f
                            linePath.cubicTo(
                                prev.x + dx, prev.y,
                                curr.x - dx, curr.y,
                                curr.x,      curr.y,
                            )
                        }

                        // Área preenchida com gradiente abaixo da linha
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(pts.last().x,  h)
                            lineTo(pts.first().x, h)
                            close()
                        }
                        drawPath(
                            path  = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    orangeColor.copy(alpha = 0.20f),
                                    Color.Transparent,
                                ),
                                startY = 0f,
                                endY   = h,
                            ),
                        )

                        // Linha principal
                        drawPath(
                            path  = linePath,
                            color = orangeColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap   = StrokeCap.Round,
                                join  = StrokeJoin.Round,
                            ),
                        )
                    }

                    // Anéis nos pontos de dados (branco + laranja)
                    pts.forEach { pt ->
                        drawCircle(Color.White,  radius = 5.dp.toPx(),   center = pt)
                        drawCircle(orangeColor,  radius = 3.5.dp.toPx(), center = pt)
                    }
                }
            }

            // ── Labels do eixo X ──────────────────────────────────────────────
            Spacer(Modifier.height(6.dp))
            val xLabels = buildWeightXLabels(sortedAsc)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 50.dp),
                horizontalArrangement = if (xLabels.size == 1)
                    Arrangement.Center
                else
                    Arrangement.SpaceBetween,
            ) {
                xLabels.forEach { label ->
                    Text(text = label, style = labelStyle, color = labelColor)
                }
            }
        }
    }
}

// ─── Formulário: Nova Pesagem ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWeightForm(
    petId: Long,
    onSave: (HealthRecord) -> Unit,
    onDismiss: () -> Unit,
) {
    var weightText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dateMillis by remember {
        mutableLongStateOf(DateUtils.localMillisToUtcMidnight(System.currentTimeMillis()))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
    val dateStr  = DateUtils.utcMillisToDisplayDate(dateMillis)
    // Aceita vírgula como separador decimal (common on pt-BR keyboards)
    val isValid  = weightText.replace(',', '.').toDoubleOrNull()?.let { it > 0.0 } == true
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.72f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormHeader(iconRes = R.drawable.vazio_peso, title = "Registrar Pesagem", accentColor = OrangePrimary)

        HealthTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = "Peso (kg) *",
            placeholder = "Ex: 5.3",
            keyboardType = KeyboardType.Decimal,
        )

        HealthDateField(
            label = "Data da pesagem",
            dateStr = dateStr,
            onClick = { showDatePicker = true },
        )

        HealthTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Observações (opcional)",
            placeholder = "Ex: Após vacinação, em jejum",
            singleLine = false,
            minLines = 2,
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (isValid && !isSaving) {
                    isSaving = true
                    onSave(
                        HealthRecord(
                            petId     = petId,
                            type      = "weight",
                            weightKg  = weightText.replace(',', '.').toDouble(),
                            dateMillis = dateMillis,
                            notes     = notes.trim(),
                        )
                    )
                }
            },
            enabled = isValid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
        ) {
            Text("Salvar pesagem", fontWeight = FontWeight.Bold, color = Color.White)
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─── Utilitários do gráfico de peso ──────────────────────────────────────────

/**
 * Formata kg para exibição: remove zeros e ponto desnecessários.
 * Ex: 5.00 → "5", 5.30 → "5.3", 12.55 → "12.6" (1 casa decimal max).
 */
private fun formatWeightKg(kg: Double): String =
    String.format(java.util.Locale.US, "%.1f", kg).trimEnd('0').trimEnd('.')

/**
 * Rótulos do eixo X: todos se ≤4 pontos; caso contrário, 4 distribuídos
 * (primeiro, 1/3, 2/3 e último). Formato "dd/MM" (5 primeiros chars de dd/MM/yyyy).
 */
private fun buildWeightXLabels(sorted: List<HealthRecord>): List<String> {
    val n = sorted.size
    return if (n <= 4) {
        sorted.map { DateUtils.utcMillisToDisplayDate(it.dateMillis).take(5) }
    } else {
        listOf(sorted.first(), sorted[n / 3], sorted[2 * n / 3], sorted.last())
            .map { DateUtils.utcMillisToDisplayDate(it.dateMillis).take(5) }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SUB-ABA: ALIMENTAÇÃO — Seção 12, Parte 3
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun FeedingContent(
    records: ImmutableList<HealthRecord>,
    onDelete: (HealthRecord) -> Unit,
) {
    if (records.isEmpty()) {
        HealthEmptyState(
            imageRes = R.drawable.vazio_alimentacao,
            title = "Nenhuma dieta registrada",
            message = "Adicione as informações de alimentação do seu pet para acompanhar o tipo de ração, as porções e os horários das refeições.",
        )
        return
    }

    val sorted = remember(records) { records.sortedByDescending { it.dateMillis } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(sorted, key = { _, r -> r.id }) { index, record ->
            StaggeredHealthItem(index = index) {
                FeedingCard(record = record, onDelete = onDelete)
            }
        }
    }
}

// ─── Card de alimentação ──────────────────────────────────────────────────────

@Composable
private fun FeedingCard(record: HealthRecord, onDelete: (HealthRecord) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Divide horários por vírgula e descarta entradas em branco
    val scheduleChips = remember(record.feedingSchedule) {
        record.feedingSchedule
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── Linha superior: ícone + informações + botão deletar ────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Badge com ícone da alimentação
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrangePrimary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.icone_alimentacao),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit,
                    )
                }

                // Tipo, quantidade, data e observações
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = record.feedingType,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (record.feedingAmountGrams > 0.0) {
                        Text(
                            text = formatFeedingAmount(record.feedingAmountGrams),
                            style = MaterialTheme.typography.bodySmall,
                            color = OrangePrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "A partir de ${DateUtils.utcMillisToDisplayDate(record.dateMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                    if (record.notes.isNotBlank()) {
                        Text(
                            text = record.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Remover",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // ── Chips de horário (alinhados abaixo do ícone) ──────────────────
            if (scheduleChips.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(start = 60.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    scheduleChips.forEach { time ->
                        FeedingTimeChip(time = time)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            message = "Remover o registro de alimentação \"${record.feedingType}\"? Essa ação não pode ser desfeita.",
            onConfirm = {
                onDelete(record)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

// ─── Chip de horário ──────────────────────────────────────────────────────────

@Composable
private fun FeedingTimeChip(time: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(OrangePrimary.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = OrangePrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─── Formulário: Nova Alimentação ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFeedingForm(
    petId: Long,
    onSave: (HealthRecord) -> Unit,
    onDismiss: () -> Unit,
) {
    var feedingType by remember { mutableStateOf("") }
    var amountText  by remember { mutableStateOf("") }
    var schedule    by remember { mutableStateOf("") }
    var notes       by remember { mutableStateOf("") }
    var dateMillis  by remember {
        mutableLongStateOf(DateUtils.localMillisToUtcMidnight(System.currentTimeMillis()))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
    val dateStr = DateUtils.utcMillisToDisplayDate(dateMillis)
    // Tipo obrigatório + quantidade numérica positiva (aceita vírgula pt-BR)
    val isValid = feedingType.isNotBlank() &&
        amountText.replace(',', '.').toDoubleOrNull()?.let { it > 0.0 } == true
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormHeader(iconRes = R.drawable.icone_alimentacao, title = "Registrar Alimentação", accentColor = Color(0xFF00897B))

        // Tipo de alimento — obrigatório
        HealthTextField(
            value = feedingType,
            onValueChange = { feedingType = it },
            label = "Tipo de alimento *",
            placeholder = "Ex: Ração seca, Ração úmida, Natural",
        )

        // Quantidade por porção em gramas — obrigatório
        HealthTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = "Quantidade por porção (g) *",
            placeholder = "Ex: 150",
            keyboardType = KeyboardType.Decimal,
        )

        // Horários das refeições — opcional, separados por vírgula
        HealthTextField(
            value = schedule,
            onValueChange = { schedule = it },
            label = "Horários das refeições",
            placeholder = "Ex: 07:00, 12:00, 19:00",
        )

        // Data de início da dieta
        HealthDateField(
            label = "Data de início",
            dateStr = dateStr,
            onClick = { showDatePicker = true },
        )

        // Observações opcionais
        HealthTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Observações (opcional)",
            placeholder = "Ex: Trocar para ração sênior no próximo mês",
            singleLine = false,
            minLines = 2,
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (isValid && !isSaving) {
                    isSaving = true
                    onSave(
                        HealthRecord(
                            petId              = petId,
                            type               = "feeding",
                            feedingType        = feedingType.trim(),
                            feedingAmountGrams = amountText.replace(',', '.').toDouble(),
                            feedingSchedule    = schedule.trim(),
                            dateMillis         = dateMillis,
                            notes              = notes.trim(),
                        )
                    )
                }
            },
            enabled = isValid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
        ) {
            Text("Salvar alimentação", fontWeight = FontWeight.Bold, color = Color.White)
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─── Utilitário de formatação de gramagem ─────────────────────────────────────

/**
 * Remove zeros e ponto desnecessários e anexa sufixo de unidade.
 * Ex: 150.0 → "150g por porção", 75.5 → "75.5g por porção"
 */
private fun formatFeedingAmount(grams: Double): String {
    val formatted = String.format(java.util.Locale.US, "%.1f", grams).trimEnd('0').trimEnd('.')
    return "${formatted}g por porção"
}
