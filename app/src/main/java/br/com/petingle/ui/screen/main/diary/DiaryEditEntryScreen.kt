package br.com.petingle.ui.screen.main.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import br.com.petingle.data.db.entity.DiaryEntry
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.ui.theme.OrangeGradStart
import br.com.petingle.ui.theme.OrangePrimary
import kotlinx.collections.immutable.ImmutableList
import java.io.File

/**
 * Tela de edição de uma entrada existente no Diário.
 * Permite editar a legenda e o pet associado. A foto não é alterada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditEntryScreen(
    entry: DiaryEntry,
    pets: ImmutableList<Pet>,
    onDismiss: () -> Unit,
    onSave: (updatedEntry: DiaryEntry) -> Unit,
) {
    val context = LocalContext.current

    var caption       by rememberSaveable { mutableStateOf(entry.caption) }
    var selectedPetId by rememberSaveable { mutableStateOf(entry.petId) }
    var isSaving      by remember { mutableStateOf(false) }

    fun handleBack() { if (!isSaving) onDismiss() }
    BackHandler(onBack = ::handleBack)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // ── TopAppBar com gradiente laranja ───────────────────────────────
            TopAppBar(
                title = {
                    Text(
                        text       = "Editar momento",
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::handleBack, enabled = !isSaving) {
                        Icon(Icons.Rounded.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OrangeGradStart),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {

                // ── Foto existente (somente leitura) ──────────────────────────
                if (entry.photoPath.isNotEmpty() && File(entry.photoPath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(entry.photoPath))
                            .size(800)
                            .scale(Scale.FILL)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto do momento",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }

                // ── Campo de legenda editável ─────────────────────────────────
                OutlinedTextField(
                    value          = caption,
                    onValueChange  = { if (it.length <= 140) caption = it },
                    label          = { Text("Legenda") },
                    supportingText = { Text("${caption.length}/140") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier       = Modifier.fillMaxWidth(),
                    shape          = RoundedCornerShape(12.dp),
                    minLines       = 2,
                )

                // ── Seletor de pet (apenas se houver múltiplos) ───────────────
                if (pets.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text       = "Pet",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onBackground,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pets.forEach { pet ->
                                FilterChip(
                                    selected = pet.id == selectedPetId,
                                    onClick  = { selectedPetId = pet.id },
                                    label    = { Text(pet.name) },
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OrangePrimary.copy(alpha = 0.16f),
                                        selectedLabelColor     = OrangePrimary,
                                    ),
                                )
                            }
                        }
                    }
                }

                // ── Botão Salvar ──────────────────────────────────────────────
                Button(
                    onClick = {
                        isSaving = true
                        onSave(entry.copy(caption = caption.trim(), petId = selectedPetId))
                    },
                    enabled  = !isSaving,
                    colors   = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape    = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Salvar alterações", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
