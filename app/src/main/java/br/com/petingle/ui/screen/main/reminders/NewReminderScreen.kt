package br.com.petingle.ui.screen.main.reminders

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.petingle.R
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.ui.theme.OrangeGradEnd
import br.com.petingle.ui.theme.OrangeGradStart
import br.com.petingle.ui.theme.OrangePrimary
import br.com.petingle.ui.viewmodel.NewReminderViewModel
import kotlinx.collections.immutable.ImmutableList
import br.com.petingle.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

// ─── Tela "Novo Lembrete" / Editar Lembrete ───────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReminderScreen(
    reminderId: Long = -1L,
    viewModel: NewReminderViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {},
    onSaved: () -> Unit = {},
) {
    val context = LocalContext.current
    val pets by viewModel.pets.collectAsState()

    LaunchedEffect(reminderId) {
        if (reminderId > 0L) viewModel.loadReminder(reminderId)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAlarmPermissionDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(viewModel.dateTimeMillis)
    val timeStr = timeFormat.format(viewModel.dateTimeMillis)

    // Dias restantes para exibir no resumo
    val daysUntil = remember(viewModel.dateTimeMillis) {
        val diff = viewModel.dateTimeMillis - System.currentTimeMillis()
        TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // ── Cabeçalho ─────────────────────────────────────────────────────
            ReminderHeader(
                isEditing   = reminderId > 0L,
                category    = viewModel.category,
                onDismiss   = onDismiss,
            )

            // ── Formulário ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {

                // ── Título ────────────────────────────────────────────────────
                SectionLabel("Título do lembrete")
                OutlinedTextField(
                    value         = viewModel.title,
                    onValueChange = { viewModel.title = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(
                            "Ex: Vacina anual do Rex",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
                        )
                    },
                    singleLine = true,
                    shape      = RoundedCornerShape(16.dp),
                    colors     = outlinedFieldColors(),
                )

                // ── Pet ───────────────────────────────────────────────────────
                if (pets.isNotEmpty()) {
                    SectionLabel("Para qual pet?")
                    PetChipRow(
                        pets           = pets,
                        selectedPetId  = viewModel.selectedPetId,
                        onPetSelected  = { viewModel.selectedPetId = it },
                    )
                }

                // ── Categoria ─────────────────────────────────────────────────
                SectionLabel("Categoria")
                CategoryGrid(
                    selected = viewModel.category,
                    onSelect = { viewModel.category = it },
                )

                // ── Data e Hora ───────────────────────────────────────────────
                SectionLabel("Data e horário")
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DateTimeButton(
                        modifier  = Modifier.weight(1f),
                        icon      = Icons.Rounded.CalendarMonth,
                        label     = dateStr,
                        onClick   = { showDatePicker = true },
                    )
                    DateTimeButton(
                        modifier  = Modifier.weight(1f),
                        icon      = Icons.Rounded.AccessTime,
                        label     = timeStr,
                        onClick   = { showTimePicker = true },
                    )
                }

                // Contagem regressiva
                if (daysUntil >= 0) {
                    CountdownBadge(daysUntil)
                }

                // ── Recorrência ───────────────────────────────────────────────
                SectionLabel("Repetição")
                RecurrenceSelector(
                    selected = viewModel.recurrence,
                    onSelect = { viewModel.recurrence = it },
                )

                // ── Observações ───────────────────────────────────────────────
                SectionLabel("Observações (opcional)")
                OutlinedTextField(
                    value         = viewModel.notes,
                    onValueChange = { viewModel.notes = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Notes,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Adicione detalhes relevantes...",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
                            )
                        }
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape    = RoundedCornerShape(16.dp),
                    colors   = outlinedFieldColors(),
                )

                Spacer(Modifier.height(4.dp))

                // ── Botão salvar ──────────────────────────────────────────────
                Button(
                    onClick = {
                        val alarmManager =
                            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            alarmManager.canScheduleExactAlarms()
                        } else true
                        if (!canExact) {
                            showAlarmPermissionDialog = true
                        } else {
                            viewModel.saveReminder(onSaved)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = viewModel.isValid && !viewModel.isSaving,
                    shape   = RoundedCornerShape(24.dp),
                    colors  = ButtonDefaults.buttonColors(
                        containerColor         = OrangePrimary,
                        contentColor           = Color.White,
                        disabledContainerColor = OrangePrimary.copy(alpha = 0.35f),
                        disabledContentColor   = Color.White,
                    ),
                ) {
                    if (viewModel.isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text       = if (reminderId > 0L) "Salvar alterações" else "Criar lembrete",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // ── Diálogo: permissão de alarme exato ────────────────────────────────────
    if (showAlarmPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmPermissionDialog = false },
            icon = {
                Icon(Icons.Rounded.AlarmOn, contentDescription = null, tint = OrangePrimary)
            },
            title = { Text("Permissão de alarme necessária") },
            text = {
                Text(
                    "Para o lembrete disparar exatamente no horário escolhido, o PetIngle " +
                    "precisa da permissão de Alarmes e lembretes.\n\n" +
                    "Toque em \"Abrir configurações\", ative o PetIngle na lista e " +
                    "volte para criar o lembrete."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAlarmPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape  = RoundedCornerShape(24.dp),
                ) { Text("Abrir configurações", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmPermissionDialog = false }) { Text("Agora não") }
            },
        )
    }

    // ── Diálogo de erro ───────────────────────────────────────────────────────
    val saveError = viewModel.saveError
    if (saveError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title            = { Text("Não foi possível salvar") },
            text             = { Text(saveError) },
            confirmButton    = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            },
        )
    }

    // ── DatePicker ────────────────────────────────────────────────────────────
    if (showDatePicker) {
        val utcMidnightInitial = remember(viewModel.dateTimeMillis) {
            DateUtils.localMillisToUtcMidnight(viewModel.dateTimeMillis)
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = utcMidnightInitial,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utc ->
                        viewModel.dateTimeMillis = DateUtils.utcMillisToLocalPreservingTime(
                            utc, viewModel.dateTimeMillis
                        )
                    }
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

    // ── TimePicker ────────────────────────────────────────────────────────────
    if (showTimePicker) {
        val timeCal = remember(viewModel.dateTimeMillis) {
            Calendar.getInstance().apply { timeInMillis = viewModel.dateTimeMillis }
        }
        val timePickerState = rememberTimePickerState(
            initialHour   = timeCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = timeCal.get(Calendar.MINUTE),
            is24Hour      = true,
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape          = RoundedCornerShape(28.dp),
                color          = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(
                        "Selecionar horário",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = viewModel.dateTimeMillis
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE,      timePickerState.minute)
                                set(Calendar.SECOND,      0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            viewModel.dateTimeMillis = newCal.timeInMillis
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

// ─── Cabeçalho com gradiente + ícone dinâmico da categoria ───────────────────

@Composable
private fun ReminderHeader(
    isEditing: Boolean,
    category: String,
    onDismiss: () -> Unit,
) {
    val catItem = CATEGORIES.find { it.key == category } ?: CATEGORIES.first()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(OrangeGradStart, OrangeGradEnd)))
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        // Botão voltar
        IconButton(
            onClick  = onDismiss,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar", tint = Color.White)
        }

        // Título + ícone da categoria
        Column(
            modifier            = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Ícone da categoria selecionada num badge branco translúcido
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter            = painterResource(catItem.icon),
                    contentDescription = catItem.label,
                    modifier           = Modifier.size(24.dp),
                    contentScale       = ContentScale.Fit,
                )
            }
            Text(
                text       = if (isEditing) "Editar Lembrete" else "Novo Lembrete",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
            Text(
                text  = catItem.label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.80f),
            )
        }
    }
}

// ─── Chips de seleção de pet ──────────────────────────────────────────────────

@Composable
private fun PetChipRow(
    pets: ImmutableList<Pet>,
    selectedPetId: Long,
    onPetSelected: (Long) -> Unit,
) {
    LazyRow(
        contentPadding       = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(pets, key = { it.id }) { pet ->
            val isSelected = pet.id == selectedPetId
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) OrangePrimary else OrangePrimary.copy(alpha = 0.08f),
                animationSpec = tween(200), label = "petChipBg",
            )
            val borderWidth by animateDpAsState(
                targetValue   = if (isSelected) 0.dp else 1.dp,
                animationSpec = tween(200), label = "petChipBorder",
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .border(
                        borderWidth,
                        OrangePrimary.copy(alpha = 0.25f),
                        RoundedCornerShape(50),
                    )
                    .clickable { onPetSelected(pet.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(14.dp),
                    )
                } else {
                    Icon(
                        Icons.Rounded.Pets,
                        contentDescription = null,
                        tint               = OrangePrimary.copy(alpha = 0.70f),
                        modifier           = Modifier.size(14.dp),
                    )
                }
                Text(
                    text       = pet.name,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color      = if (isSelected) Color.White else OrangePrimary,
                )
            }
        }
    }
}

// ─── Grade de categorias ──────────────────────────────────────────────────────

private data class CategoryItem(val key: String, val label: String, @DrawableRes val icon: Int)

private val CATEGORIES = listOf(
    CategoryItem("vacina",        "Vacina",        R.drawable.icone_vacina),
    CategoryItem("consulta",      "Consulta",      R.drawable.icone_consulta),
    CategoryItem("banho",         "Banho",         R.drawable.icone_banho),
    CategoryItem("medicacao",     "Medicação",     R.drawable.icone_medicacao),
    CategoryItem("alimentacao",   "Alimentação",   R.drawable.icone_alimentacao),
    CategoryItem("vermifugo",     "Vermífugo",     R.drawable.icone_vermifugo),
    CategoryItem("personalizado", "Personalizado", R.drawable.icone_personalizado),
)

@Composable
private fun CategoryGrid(selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(CATEGORIES) { cat ->
            val isSelected = selected == cat.key
            val bgColor by animateColorAsState(
                targetValue   = if (isSelected) OrangePrimary.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                animationSpec = tween(180), label = "catBg",
            )
            val borderColor by animateColorAsState(
                targetValue   = if (isSelected) OrangePrimary
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                animationSpec = tween(180), label = "catBorder",
            )
            val borderWidth by animateDpAsState(
                targetValue   = if (isSelected) 2.dp else 1.dp,
                animationSpec = tween(180), label = "catBorderW",
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(bgColor)
                    .border(borderWidth, borderColor, RoundedCornerShape(24.dp))
                    .clickable { onSelect(cat.key) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Image(
                    painter            = painterResource(cat.icon),
                    contentDescription = cat.label,
                    modifier           = Modifier.size(20.dp),
                    contentScale       = ContentScale.Fit,
                )
                Text(
                    text       = cat.label,
                    style      = MaterialTheme.typography.labelMedium,
                    color      = if (isSelected) OrangePrimary
                                 else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// ─── Botão de data / hora ─────────────────────────────────────────────────────

@Composable
private fun DateTimeButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f),
        ),
        onClick   = onClick,
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier         = Modifier
                    .size(34.dp)
                    .background(OrangePrimary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = OrangePrimary,
                    modifier           = Modifier.size(18.dp),
                )
            }
            Text(
                text       = label,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

// ─── Badge de contagem regressiva ─────────────────────────────────────────────

@Composable
private fun CountdownBadge(daysUntil: Int) {
    val text = when {
        daysUntil == 0 -> "Lembrete para hoje"
        daysUntil == 1 -> "Lembrete para amanhã"
        else           -> "Faltam $daysUntil dias para o lembrete"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(OrangePrimary.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Rounded.Repeat,
            contentDescription = null,
            tint               = OrangePrimary,
            modifier           = Modifier.size(18.dp),
        )
        Text(
            text       = text,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = OrangePrimary,
        )
    }
}

// ─── Seletor de recorrência ───────────────────────────────────────────────────

private val RECURRENCES = listOf(
    "none"    to "Não repete",
    "daily"   to "Diário",
    "weekly"  to "Semanal",
    "monthly" to "Mensal",
)

@Composable
private fun RecurrenceSelector(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RECURRENCES.forEach { (key, label) ->
            val isSelected = selected == key
            val bg by animateColorAsState(
                targetValue   = if (isSelected) OrangePrimary else OrangePrimary.copy(alpha = 0.07f),
                animationSpec = tween(180), label = "recBg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(bg)
                    .clickable { onSelect(key) }
                    .padding(vertical = 11.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isSelected) Color.White else OrangePrimary,
                    textAlign  = TextAlign.Center,
                    maxLines   = 2,
                )
            }
        }
    }
}

// ─── Label de seção ───────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
    )
}

// ─── Cores padrão dos campos de texto ────────────────────────────────────────

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = OrangePrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
    focusedLabelColor    = OrangePrimary,
)
