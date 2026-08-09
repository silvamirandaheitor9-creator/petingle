package br.com.petingle.ui.screen.main.pets

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.Male
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import br.com.petingle.R
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.ui.theme.OrangePrimary
import br.com.petingle.ui.theme.OrangeGradStart
import br.com.petingle.ui.viewmodel.NewPetViewModel
import br.com.petingle.util.DateUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ─── Espécies (SPEC §11) ──────────────────────────────────────────────────────

private data class SpeciesOption(
    val storageValue: String,
    val label: String,
    @DrawableRes val iconRes: Int,
)

private val SPECIES_OPTIONS = listOf(
    SpeciesOption("cachorro", "Cachorro", R.drawable.icone_especie_cachorro),
    SpeciesOption("gato",     "Gato",     R.drawable.icone_especie_gato),
    SpeciesOption("pássaro",  "Pássaro",  R.drawable.icone_especie_passaro),
    SpeciesOption("peixe",    "Peixe",    R.drawable.icone_especie_peixe),
    SpeciesOption("réptil",   "Réptil",   R.drawable.icone_especie_reptil),
    SpeciesOption("roedor",   "Roedor",   R.drawable.icone_especie_roedor),
    SpeciesOption("outro",    "Outro",    R.drawable.icone_especie_outro),
)

private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

// ─── Tela unificada de Novo Pet / Editar Pet (SPEC §11 simplificado) ─────────

/**
 * Formulário único e scrollável — apenas informações básicas.
 * [petId] == -1L → modo criação; > 0 → modo edição.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPetScreen(
    viewModel: NewPetViewModel = hiltViewModel(),
    petId: Long = -1L,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onNavigateToPhotoEditor: (Uri) -> Unit = {},
) {
    val isEditMode  = petId > 0L
    val photoPath   by viewModel.photoPath.collectAsState()
    val editingPet  by viewModel.editingPet.collectAsState()
    val isSaving    by viewModel.isSaving.collectAsState()

    LaunchedEffect(petId) {
        if (isEditMode) viewModel.loadPetForEditing(petId)
    }

    // ── Estados do formulário ────────────────────────────────────────────────
    var name         by rememberSaveable { mutableStateOf("") }
    var species      by rememberSaveable { mutableStateOf<String?>(null) }
    var sex          by rememberSaveable { mutableStateOf<String?>(null) }
    var breed        by rememberSaveable { mutableStateOf("") }
    var birthDateIso by rememberSaveable { mutableStateOf("") }
    var weightText   by rememberSaveable { mutableStateOf("") }
    var formPreFilled by rememberSaveable { mutableStateOf(false) }

    // Pré-preenche ao editar
    LaunchedEffect(editingPet) {
        val p = editingPet ?: return@LaunchedEffect
        if (!formPreFilled) {
            name         = p.name
            species      = p.species.takeIf { it.isNotBlank() }
            sex          = p.sex.takeIf { it.isNotBlank() }
            breed        = p.breed
            birthDateIso = p.birthDate
            weightText   = if (p.weightKg > 0.0) p.weightKg.toString() else ""
            formPreFilled = true
        }
    }

    // ── Validações ────────────────────────────────────────────────────────────
    var attemptedSave  by remember { mutableStateOf(false) }
    var showPetLimitMessage by remember { mutableStateOf(false) }
    val nameError      = attemptedSave && name.isBlank()
    val weightValue    = weightText.trim().replace(',', '.').toDoubleOrNull()
    val weightError    = attemptedSave && weightText.isNotBlank() &&
        (weightValue == null || weightValue <= 0.0)
    val birthDateError = attemptedSave && birthDateIso.isNotBlank() &&
        isIsoDateInFuture(birthDateIso)

    var showDatePicker by remember { mutableStateOf(false) }

    // ── Seletor de foto ───────────────────────────────────────────────────────
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) onNavigateToPhotoEditor(uri) }

    BackHandler(enabled = !isSaving) { onDismiss() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {

            // ── TopAppBar ────────────────────────────────────────────────────
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Editar Pet" else "Novo Pet",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(Icons.Rounded.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OrangeGradStart),
            )

            // ── Formulário ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // ── Avatar / Foto ─────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, OrangePrimary.copy(alpha = 0.35f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    pickPhotoLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                                        ),
                                    )
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (photoPath.isNullOrBlank()) {
                            val speciesIconRes = SPECIES_OPTIONS
                                .find { it.storageValue == species }?.iconRes
                            if (speciesIconRes != null) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(OrangePrimary.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(speciesIconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp),
                                    )
                                }
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.avatar_pet_padrao),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(File(photoPath!!))
                                    .build(),
                                contentDescription = "Foto do pet",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // Ícone câmera no canto
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(OrangePrimary)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.PhotoCamera, null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Text(
                        text = if (photoPath.isNullOrBlank()) "Toque para adicionar uma foto"
                               else "Toque para trocar a foto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                        textAlign = TextAlign.Center,
                    )
                }

                // ── Nome ───────────────────────────────────────────────────────
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do pet*") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("O nome é obrigatório") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Espécie ────────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Espécie",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SPECIES_OPTIONS, key = { it.storageValue }) { option ->
                            SpeciesChip(
                                option = option,
                                selected = species == option.storageValue,
                                onClick = { species = option.storageValue },
                            )
                        }
                    }
                }

                // ── Sexo ──────────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Sexo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SexOptionChip(
                            label = "Macho", icon = Icons.Rounded.Male,
                            selected = sex == "Macho",
                            onClick = { sex = "Macho" },
                            modifier = Modifier.weight(1f),
                        )
                        SexOptionChip(
                            label = "Fêmea", icon = Icons.Rounded.Female,
                            selected = sex == "Fêmea",
                            onClick = { sex = "Fêmea" },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ── Raça ───────────────────────────────────────────────────────
                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Raça (opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Data de nascimento ─────────────────────────────────────────
                val dateInteractionSource = remember { MutableInteractionSource() }
                val isDatePressed by dateInteractionSource.collectIsPressedAsState()
                LaunchedEffect(isDatePressed) { if (isDatePressed) showDatePicker = true }
                OutlinedTextField(
                    value = if (birthDateIso.isBlank()) "" else
                        DateUtils.isoDateToUtcMidnightMillis(birthDateIso)
                            ?.let { DateUtils.utcMillisToDisplayDate(it) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data de nascimento (opcional)") },
                    isError = birthDateError,
                    supportingText = {
                        if (birthDateError) Text("A data não pode ser no futuro")
                    },
                    trailingIcon = {
                        Icon(Icons.Rounded.CalendarMonth, "Selecionar data")
                    },
                    interactionSource = dateInteractionSource,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Peso ───────────────────────────────────────────────────────
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Peso atual (kg, opcional)") },
                    isError = weightError,
                    supportingText = {
                        if (weightError) Text("Informe um peso numérico maior que zero")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))
            }

            // ── Botão Salvar fixo ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = {
                        attemptedSave = true
                        val hasError = name.isBlank() ||
                            (weightText.isNotBlank() &&
                                (weightValue == null || weightValue <= 0.0)) ||
                            (birthDateIso.isNotBlank() && isIsoDateInFuture(birthDateIso))
                        if (hasError) return@Button

                        val pet = Pet(
                            name          = name.trim(),
                            species       = species ?: "outro",
                            breed         = breed.trim(),
                            sex           = sex ?: "",
                            isCastrated   = editingPet?.isCastrated ?: false,
                            birthDate     = birthDateIso,
                            weightKg      = weightValue ?: 0.0,
                            photoPath     = photoPath ?: "",
                            // campos não coletados nesta tela — ficam em branco
                            bloodType         = editingPet?.bloodType ?: "",
                            allergies         = editingPet?.allergies ?: "",
                            chronicConditions = editingPet?.chronicConditions ?: "",
                            microchip         = editingPet?.microchip ?: "",
                            notes             = editingPet?.notes ?: "",
                            vetName           = editingPet?.vetName ?: "",
                            vetPhone          = editingPet?.vetPhone ?: "",
                        )

                        if (isEditMode && editingPet != null) {
                            viewModel.updatePet(editingPet!!, pet, onSaved)
                        } else {
                            viewModel.savePet(
                                pet = pet,
                                onSaved = onSaved,
                                onLimitReached = { showPetLimitMessage = true },
                            )
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                        )
                    } else {
                        Text(
                            text = if (isEditMode) "Salvar alterações" else "Adicionar Pet",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

    if (showPetLimitMessage) {
        AlertDialog(
            onDismissRequest = { showPetLimitMessage = false },
            title = { Text("Limite de perfis atingido") },
            text = {
                Text("Assista a um anúncio na aba Meus Pets para desbloquear mais perfis.")
            },
            confirmButton = {
                TextButton(onClick = { showPetLimitMessage = false }) {
                    Text("Entendi")
                }
            },
        )
    }
            }
        }
    }

    // ── DatePicker ────────────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.isoDateToUtcMidnightMillis(birthDateIso),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        birthDateIso = DateUtils.utcMillisToIsoDate(millis)
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) { DatePicker(state = datePickerState) }
    }
}

// ─── Helpers internos ─────────────────────────────────────────────────────────

@Composable
private fun SpeciesChip(option: SpeciesOption, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) OrangePrimary.copy(alpha = 0.14f) else Color.Transparent,
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) OrangePrimary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Image(
            painter = painterResource(option.iconRes),
            contentDescription = option.label,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) OrangePrimary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SexOptionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) OrangePrimary.copy(alpha = 0.14f) else Color.Transparent,
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) OrangePrimary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) OrangePrimary
                   else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = if (selected) OrangePrimary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun isIsoDateInFuture(iso: String): Boolean {
    val parsed = runCatching { isoDateFormat.parse(iso) }.getOrNull() ?: return false
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }.time
    return parsed.after(today)
}
