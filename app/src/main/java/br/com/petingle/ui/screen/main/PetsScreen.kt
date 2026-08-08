package br.com.petingle.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.Male
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import br.com.petingle.R
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.ui.theme.OrangePrimary
import br.com.petingle.ui.viewmodel.PetsViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ─── Ponto de entrada da aba Meus Pets (SPEC §8) ─────────────────────────────

@Composable
fun PetsScreen(
    viewModel: PetsViewModel = hiltViewModel(),
    onPetClick: (Long) -> Unit = {},
    onEditPet: (Long) -> Unit = {},
) {
    val pets by viewModel.pets.collectAsState()
    var petToDelete by remember { mutableStateOf<Pet?>(null) }

    if (pets.isEmpty()) {
        EmptyPetsGridState()
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 14.dp, end = 14.dp, top = 14.dp, bottom = 96.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(pets, key = { _, pet -> pet.id }) { index, pet ->
                StaggeredPetCard(
                    pet = pet,
                    index = index,
                    onPetClick = onPetClick,
                    onEdit = { onEditPet(pet.id) },
                    onDelete = { petToDelete = pet },
                )
            }
        }
    }

    // Modal customizado de exclusão (SPEC §13)
    petToDelete?.let { pet ->
        DeletePetConfirmModal(
            pet = pet,
            onConfirm = {
                viewModel.deletePetFromList(pet)
                petToDelete = null
            },
            onDismiss = { petToDelete = null },
        )
    }
}

// ─── Card com animação de entrada escalonada ──────────────────────────────────

@Composable
private fun StaggeredPetCard(
    pet: Pet,
    index: Int,
    onPetClick: (Long) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var visible by remember(pet.id) { mutableStateOf(false) }
    LaunchedEffect(pet.id) {
        delay((index * 60L).coerceAtMost(400L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(280)) + slideInVertically(
            tween(300), initialOffsetY = { it / 4 },
        ),
    ) {
        PetGridCard(
            pet = pet,
            onPetClick = onPetClick,
            onEdit = onEdit,
            onDelete = onDelete,
        )
    }
}

// ─── Card individual redesenhado ──────────────────────────────────────────────

@Composable
private fun PetGridCard(
    pet: Pet,
    onPetClick: (Long) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pet_card_press",
    )

    val hasRealPhoto = remember(pet.photoPath) {
        pet.photoPath.isNotEmpty() && File(pet.photoPath).exists()
    }
    val ageLabel    = remember(pet.birthDate, pet.approximateAge) { petAgeLabel(pet) }
    val speciesIcon = speciesIconRes(pet.species)

    var showActions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (showActions) showActions = false else onPetClick(pet.id)
                },
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Área de foto (168dp) com overlays ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
            ) {
                // Foto ou placeholder por espécie — preenchimento total
                if (hasRealPhoto) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(pet.photoPath))
                            .size(360) // 500 → 360: suficiente para 168dp de altura
                            .scale(Scale.FILL)
                            .crossfade(true)
                            .build(),
                        contentDescription = pet.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(OrangePrimary.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(speciesIcon),
                            contentDescription = pet.species,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(80.dp),
                        )
                    }
                }

                // Gradiente inferior para legibilidade — mais curto pois info foi para baixo da foto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                            ),
                        ),
                )

                // Apenas nome + ícone de sexo sobre o gradiente
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = pet.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    when (pet.sex.trim().lowercase()) {
                        "macho" -> Icon(
                            imageVector = Icons.Rounded.Male,
                            contentDescription = "Macho",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp),
                        )
                        "fêmea", "femea" -> Icon(
                            imageVector = Icons.Rounded.Female,
                            contentDescription = "Fêmea",
                            tint = Color(0xFFFFB3D9).copy(alpha = 0.90f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                // Botão de ações (3 pontos) — canto superior direito
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.40f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showActions = !showActions },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Opções",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // ── Espécie + idade abaixo da foto (sem tampar a foto) ────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Image(
                        painter = painterResource(speciesIcon),
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = pet.species.replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                    )
                }
                if (ageLabel.isNotBlank()) {
                    Text(
                        text = ageLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = OrangePrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // ── Raça (se preenchida) ─────────────────────────────────────────
            if (pet.breed.isNotBlank()) {
                Text(
                    text = pet.breed,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                )
            } else {
                Spacer(Modifier.height(4.dp))
            }

            // ── Menu deslizante animado: Editar / Remover ────────────────────
            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    expandFrom = Alignment.Top,
                ) + fadeIn(tween(160)),
                exit = shrinkVertically(
                    animationSpec = tween(140),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(tween(100)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { showActions = false; onEdit() },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Edit, null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Editar",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    OutlinedButton(
                        onClick = { showActions = false; onDelete() },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.55f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Delete, null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Remover",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

// ─── Badge de idade ───────────────────────────────────────────────────────────

@Composable
private fun AgeBadge(label: String) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Cake,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.80f),
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.90f),
        )
    }
}

// ─── Modal customizado de exclusão de pet (SPEC §13) ─────────────────────────

@Composable
private fun DeletePetConfirmModal(
    pet: Pet,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.feedback_erro),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                )
                Text(
                    text = "Remover ${pet.name}?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Tem certeza que quer remover ${pet.name} e todo o histórico dele? Essa ação não pode ser desfeita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                    ) { Text("Cancelar") }
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.weight(1f),
                    ) { Text("Remover", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ─── Estado vazio (SPEC §8.5) ─────────────────────────────────────────────────

@Composable
private fun EmptyPetsGridState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.mascote_splash),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.58f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Nenhum pet por aqui ainda",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Toque no + para cadastrar seu primeiro pet com muito carinho.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Helpers públicos (reutilizados em HomeScreen e PetDetailScreen) ──────────

fun speciesIconRes(species: String): Int {
    val s = species.trim().lowercase()
        .replace('ã', 'a').replace('á', 'a').replace('â', 'a').replace('à', 'a')
        .replace('é', 'e').replace('ê', 'e')
        .replace('í', 'i')
        .replace('õ', 'o').replace('ó', 'o').replace('ô', 'o')
        .replace('ú', 'u').replace('ü', 'u')
        .replace('ç', 'c')
    return when (s) {
        "cao", "cachorro" -> R.drawable.icone_especie_cachorro
        "gato"            -> R.drawable.icone_especie_gato
        "passaro"         -> R.drawable.icone_especie_passaro
        "peixe"           -> R.drawable.icone_especie_peixe
        "reptil"          -> R.drawable.icone_especie_reptil
        "roedor"          -> R.drawable.icone_especie_roedor
        else              -> R.drawable.icone_especie_outro
    }
}

private fun petAgeLabel(pet: Pet): String {
    if (pet.birthDate.isNotBlank()) {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val bd = sdf.parse(pet.birthDate) ?: return pet.approximateAge
            val now = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = bd }
            val totalMonths =
                (now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)) * 12 +
                    now.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
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
