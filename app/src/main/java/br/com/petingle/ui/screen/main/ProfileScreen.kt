package br.com.petingle.ui.screen.main

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.petingle.R
import br.com.petingle.ui.theme.OrangeGradEnd
import br.com.petingle.ui.theme.OrangeGradStart
import br.com.petingle.ui.theme.OrangePrimary
import br.com.petingle.ui.viewmodel.ProfileUiEvent
import br.com.petingle.ui.viewmodel.ProfileViewModel
import br.com.petingle.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage

// ─────────────────────────────────────────────────────────────────────────────
// ProfileScreen — redesign completo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val context         = LocalContext.current
    val focusManager    = LocalFocusManager.current
    val scope           = rememberCoroutineScope()
    val snackbarState   = remember { SnackbarHostState() }

    val userName      by viewModel.userName.collectAsState()
    val isDark        by themeViewModel.isDarkTheme.collectAsState()
    val petCount      by viewModel.petCount.collectAsState()
    val diaryCount    by viewModel.diaryCount.collectAsState()
    val reminderCount by viewModel.reminderCount.collectAsState()

    var nameInput         by remember(userName) { mutableStateOf(userName) }
    var pendingImportUri  by remember { mutableStateOf<android.net.Uri?>(null) }
    var editingName       by remember { mutableStateOf(false) }

    val profilePhotoPath by viewModel.profilePhotoPath.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.saveProfilePhoto(uri)
    }

    var showImportDialog  by remember { mutableStateOf(false) }
    var showDeleteDialog1 by remember { mutableStateOf(false) }
    var showDeleteDialog2 by remember { mutableStateOf(false) }

    // ── Stagger: controla visibilidade de cada seção ──────────────────────────
    val sectionCount = 5
    val sectionVisible = remember { List(sectionCount) { mutableStateOf(false) } }
    LaunchedEffect(Unit) {
        sectionVisible.forEachIndexed { i, state ->
            delay(80L * i)
            state.value = true
        }
    }

    // ── Eventos do ViewModel ──────────────────────────────────────────────────
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileUiEvent.ExportSuccess ->
                    snackbarState.showSnackbar("Prontinho! Seus dados estão salvos com segurança 🐾")
                is ProfileUiEvent.ExportError ->
                    snackbarState.showSnackbar("Erro ao exportar: ${event.msg}")
                is ProfileUiEvent.ImportSuccess ->
                    snackbarState.showSnackbar("Backup importado com sucesso! 🐾")
                is ProfileUiEvent.ImportError ->
                    snackbarState.showSnackbar("Erro ao importar: ${event.msg}")
                is ProfileUiEvent.DeleteSuccess ->
                    snackbarState.showSnackbar("Todos os dados foram apagados.")
            }
        }
    }

    // ── SAF: exportar ─────────────────────────────────────────────────────────
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.exportBackup(context.contentResolver, treeUri)
        }
    }

    // ── SAF: importar ─────────────────────────────────────────────────────────
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { fileUri ->
        if (fileUri != null) {
            pendingImportUri = fileUri
            showImportDialog = true
        }
    }

    // ── Diálogos ──────────────────────────────────────────────────────────────
    if (showImportDialog && pendingImportUri != null) {
        PetIngleDialog(onDismiss = { showImportDialog = false }) {
            Text(
                "Importar Backup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Como você quer importar os dados?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    showImportDialog = false
                    viewModel.importBackup(context.contentResolver, pendingImportUri!!, merge = true)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary),
            ) { Text("Mesclar com dados atuais", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    showImportDialog = false
                    viewModel.importBackup(context.contentResolver, pendingImportUri!!, merge = false)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            ) { Text("Substituir tudo", fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = { showImportDialog = false }, modifier = Modifier.align(Alignment.End)) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }

    if (showDeleteDialog1) {
        PetIngleDialog(onDismiss = { showDeleteDialog1 = false }) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Apagar todos os dados?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Todos os pets, lembretes, entradas do diário e registros de saúde serão removidos do aparelho.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDeleteDialog1 = false },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                ) { Text("Cancelar") }
                Button(
                    onClick = { showDeleteDialog1 = false; showDeleteDialog2 = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Continuar") }
            }
        }
    }

    if (showDeleteDialog2) {
        PetIngleDialog(onDismiss = { showDeleteDialog2 = false }) {
            Text(
                "Tem certeza absoluta?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Esta ação é irreversível e não pode ser desfeita. Considere exportar um backup antes de continuar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDeleteDialog2 = false },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                ) { Text("Cancelar") }
                Button(
                    onClick = { showDeleteDialog2 = false; viewModel.deleteAllData() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Apagar tudo") }
            }
        }
    }

    // ── Layout principal ──────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {

            // ── Hero header ───────────────────────────────────────────────────
            ProfileHeroHeader(
                userName      = userName,
                photoPath     = profilePhotoPath,
                petCount      = petCount,
                diaryCount    = diaryCount,
                reminderCount = reminderCount,
                isDark        = isDark,
                compact       = true,
                editingName   = editingName,
                nameInput     = nameInput,
                onNameInputChange = { nameInput = it },
                onEditToggle  = { editingName = !editingName },
                onNameSave    = {
                    viewModel.setUserName(nameInput)
                    focusManager.clearFocus()
                    editingName = false
                    scope.launch { snackbarState.showSnackbar("Nome salvo! 🐾") }
                },
                onPhotoClick  = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            )

            // ── Seções compactas, sem rolagem ────────────────────────────────
            Box {
                StaggerSection(visible = sectionVisible[0].value, index = 0) {
                    AppearanceCard(isDark = isDark, onToggle = { themeViewModel.setDarkTheme(it) })
                }
            }
            Box {
                StaggerSection(visible = sectionVisible[1].value, index = 1) {
                    BackupCard(
                        onExport = { exportLauncher.launch(null) },
                        onImport = { importLauncher.launch(arrayOf("*/*")) },
                    )
                }
            }
            Box {
                StaggerSection(visible = sectionVisible[2].value, index = 2) {
                    LegalExpandableCard(
                        title   = "Política de Privacidade",
                        icon    = Icons.Rounded.Policy,
                        content = PRIVACY_POLICY_TEXT,
                    )
                }
            }
            Box {
                StaggerSection(visible = sectionVisible[3].value, index = 3) {
                    LegalExpandableCard(
                        title   = "Termos de Uso",
                        icon    = Icons.Rounded.Gavel,
                        content = TERMS_OF_USE_TEXT,
                    )
                }
            }
            Box {
                StaggerSection(visible = sectionVisible[4].value, index = 4) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegalExpandableCard(
                            title   = "Sobre o PetIngle",
                            icon    = Icons.Rounded.Info,
                            content = ABOUT_TEXT,
                        )
                        // Apagar dados — botão destrutivo isolado no fundo
                        DangerCard(onDeleteClick = { showDeleteDialog1 = true })
                    }
                }
            }
        }

        // ── Snackbar flutuante ────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
            snackbar  = { data ->
                Snackbar(
                    snackbarData   = data,
                    shape          = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor   = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
    }
}

// ─── Hero header ──────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeroHeader(
    userName         : String,
    photoPath        : String,
    petCount         : Int,
    diaryCount       : Int,
    reminderCount    : Int,
    isDark           : Boolean,
    compact          : Boolean = false,
    editingName      : Boolean,
    nameInput        : String,
    onNameInputChange: (String) -> Unit,
    onEditToggle     : () -> Unit,
    onNameSave       : () -> Unit,
    onPhotoClick     : () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // ── Zona do gradiente com informações do usuário ──────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(OrangeGradStart, OrangeGradEnd),
                    )
                )
        ) {
            // Círculo decorativo de fundo
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-30).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (compact) 16.dp else 24.dp,
                        vertical = if (compact) 12.dp else 28.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar com foto, inicial do nome, ou mascote padrão
                Box(
                    modifier         = Modifier
                        .size(if (compact) 52.dp else 72.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onPhotoClick),
                    contentAlignment = Alignment.Center,
                ) {
                    if (photoPath.isNotBlank()) {
                        AsyncImage(
                            model              = java.io.File(photoPath),
                            contentDescription = "Foto de perfil",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop,
                        )
                    } else if (userName.isNotBlank()) {
                        Text(
                            text       = userName.trim().first().uppercase(),
                            style      = if (compact) MaterialTheme.typography.titleLarge
                                         else MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color      = OrangePrimary,
                        )
                    } else {
                        Image(
                            painter            = painterResource(R.drawable.mel_avatar_pequeno),
                            contentDescription = "Mascote",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop,
                        )
                    }
                    // Ícone de edição sobreposto na borda inferior
                    Box(
                        modifier         = Modifier
                            .align(Alignment.BottomEnd)
                            .size(if (compact) 18.dp else 22.dp)
                            .background(OrangePrimary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.Edit,
                            contentDescription = "Alterar foto",
                            tint               = Color.White,
                            modifier           = Modifier.size(if (compact) 10.dp else 12.dp),
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (editingName) {
                        OutlinedTextField(
                            value           = nameInput,
                            onValueChange   = onNameInputChange,
                            singleLine      = true,
                            placeholder     = { Text("Seu apelido", color = Color.White.copy(alpha = 0.7f)) },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction      = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { onNameSave() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedTextColor     = Color.White,
                                unfocusedTextColor   = Color.White,
                                cursorColor          = Color.White,
                            ),
                            shape    = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onEditToggle) {
                                Text("Cancelar", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                            Button(
                                onClick = onNameSave,
                                colors  = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape   = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            ) {
                                Text("Salvar", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Text(
                            text       = if (userName.isNotBlank()) "Olá, ${userName.trim()}! 👋"
                                         else "Como podemos te chamar?",
                        style      = if (compact) MaterialTheme.typography.titleMedium
                                     else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White,
                        )
                        Text(
                            text  = "Tutor PetIngle",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable(onClick = onEditToggle)
                                .padding(
                                    horizontal = if (compact) 10.dp else 12.dp,
                                    vertical = if (compact) 3.dp else 5.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text("Editar nome", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }
        }

        // ── Faixa de estatísticas (fora do Box de gradiente, sem overlap) ─────
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 12.dp else 20.dp),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (compact) 8.dp else 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatChip(icon = Icons.Rounded.Pets,          label = "Pets",      value = petCount.toString(), compact = compact)
                StatDivider()
                StatChip(icon = Icons.Rounded.AutoStories,   label = "Memórias",  value = diaryCount.toString(), compact = compact)
                StatDivider()
                StatChip(icon = Icons.Rounded.Notifications, label = "Lembretes", value = reminderCount.toString(), compact = compact)
            }
        }


    }
}

@Composable
private fun StatChip(icon: ImageVector, label: String, value: String, compact: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (compact) 28.dp else 40.dp)
                .clip(CircleShape)
                .background(OrangePrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(if (compact) 15.dp else 20.dp))
        }
        Spacer(Modifier.height(if (compact) 1.dp else 4.dp))
        Text(
            value,
            style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(48.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}

// ─── Wrapper de stagger ───────────────────────────────────────────────────────

@Composable
private fun StaggerSection(visible: Boolean, index: Int, content: @Composable () -> Unit) {
    val alpha   = remember { Animatable(0f) }
    val offsetY = remember { Animatable(24f) }

    LaunchedEffect(visible) {
        if (visible) {
            delay(index * 60L)
            launch { alpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
            launch { offsetY.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha.value; translationY = offsetY.value }
            .padding(horizontal = 12.dp)
            .padding(top = 3.dp),
    ) {
        content()
    }
}

// ─── Card de aparência / tema ─────────────────────────────────────────────────

@Composable
private fun AppearanceCard(isDark: Boolean, onToggle: (Boolean) -> Unit) {
    val trackColor by animateColorAsState(
        targetValue   = if (isDark) OrangePrimary else Color.LightGray,
        animationSpec = tween(300),
        label         = "theme_track",
    )
    ProfileSectionCard(title = "Aparência", icon = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    if (isDark) "Tema Escuro" else "Tema Claro",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (isDark) "Toque para usar o tema claro" else "Toque para usar o tema escuro",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                )
            }
            Switch(
                checked        = isDark,
                onCheckedChange= onToggle,
                thumbContent   = {
                    Icon(
                        imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isDark) OrangePrimary else Color.Gray,
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor   = OrangePrimary.copy(alpha = 0.4f),
                    checkedThumbColor   = OrangePrimary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

// ─── Card de backup ───────────────────────────────────────────────────────────

@Composable
private fun BackupCard(onExport: () -> Unit, onImport: () -> Unit) {
    ProfileSectionCard(title = "Backup e Restauração", icon = Icons.Rounded.Backup) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Exportar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, OrangePrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onExport)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Upload, null, tint = OrangePrimary, modifier = Modifier.size(15.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Exportar backup", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                     Text("Salva dados, nome e foto de perfil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
            }
            // Importar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onImport)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Download, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(15.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Importar backup", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                     Text("Restaura dados, nome e foto de perfil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── Card de dados perigosos ──────────────────────────────────────────────────

@Composable
private fun DangerCard(onDeleteClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.06f),
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDeleteClick)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Apagar todos os dados",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    "Remove pets, lembretes, diário e saúde",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

// ─── Card expandível para textos legais ───────────────────────────────────────

@Composable
private fun LegalExpandableCard(title: String, icon: ImageVector, content: String) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label         = "arrow_$title",
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = OrangePrimary, modifier = Modifier.size(15.dp))
                    }
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = arrowRotation },
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(tween(250)) + fadeIn(tween(250)),
                exit    = shrinkVertically(tween(200)) + fadeOut(tween(200)),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text       = content,
                        style      = MaterialTheme.typography.bodySmall,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

// ─── Componentes reutilizáveis ────────────────────────────────────────────────

@Composable
private fun ProfileSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = OrangePrimary, modifier = Modifier.size(15.dp))
                }
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            content()
        }
    }
}

@Composable
private fun PetIngleDialog(onDismiss: () -> Unit, content: @Composable (androidx.compose.foundation.layout.ColumnScope.() -> Unit)) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content             = content,
            )
        }
    }
}

// ─── Textos legais (SPEC §19) ─────────────────────────────────────────────────

private const val PRIVACY_POLICY_TEXT = """Política de Privacidade do PetIngle
Última atualização: Julho de 2026

1. SOBRE ESTE DOCUMENTO
Esta Política de Privacidade descreve como o aplicativo PetIngle trata as informações dos seus usuários. Ao usar o app, você concorda com as práticas descritas aqui.

2. DADOS QUE FICAM NO SEU APARELHO
O PetIngle não exige criação de conta nem coleta dados pessoais em servidores próprios. Todas as informações que você cadastrar — nomes dos pets, fotos, datas, registros de saúde (vacinas, consultas, peso, medicamentos, alimentação), entradas do Diário e lembretes — ficam armazenadas exclusivamente no seu dispositivo.

3. BACKUP E EXPORTAÇÃO
O app oferece função de backup manual. O arquivo gerado é salvo na pasta que você escolher no próprio dispositivo. Você é responsável pela guarda e segurança desse arquivo.

4. PERMISSÕES UTILIZADAS
• Câmera: usada apenas quando você decide fotografar um pet diretamente pelo app.
• Galeria / Armazenamento: usada para selecionar fotos existentes no dispositivo.
• Notificações: usadas para enviar lembretes de vacinas, consultas e outros cuidados que você cadastrar. Você pode desativar notificações a qualquer momento nas configurações do sistema.
Nenhuma permissão é solicitada antes do momento em que você realmente precisa dela.

5. PUBLICIDADE E SERVIÇOS DE TERCEIROS
O PetIngle exibe anúncios por meio da plataforma Start.io. Para disponibilizar, personalizar, limitar a frequência e medir o desempenho dos anúncios, a Start.io e seus parceiros podem tratar dados técnicos do aparelho e da utilização da publicidade, conforme as políticas próprias desses serviços. O PetIngle não envia para a Start.io os nomes, fotos, registros de saúde ou lembretes cadastrados no app.

Algumas áreas do app podem mostrar anúncios na parte inferior da tela para manter a experiência gratuita. Quando o usuário atingir o limite inicial de 10 perfis de pets, poderá assistir voluntariamente a um anúncio recompensado para desbloquear mais 5 perfis. Cada desbloqueio depende da disponibilidade e da conclusão válida do anúncio, e o procedimento pode ser repetido enquanto essa opção estiver disponível.

6. DADOS E RASTREAMENTO
O PetIngle não utiliza ferramentas próprias de análise de comportamento ou rastreamento de usuário. A publicidade pode utilizar identificadores e dados técnicos tratados pela Start.io, de acordo com as permissões, configurações do aparelho e políticas aplicáveis.

7. CRIANÇAS
O PetIngle não é destinado a crianças menores de 13 anos. Não coletamos intencionalmente informações de menores.

8. SEUS DIREITOS (LGPD — LEI 13.709/2018)
Em conformidade com a Lei Geral de Proteção de Dados, você tem direito a:
• Confirmar a existência de tratamento de dados;
• Acessar, corrigir ou excluir seus dados (feito diretamente no app);
• Solicitar a portabilidade ou eliminação dos dados;
• Revogar consentimentos a qualquer momento.
Como todos os dados ficam no seu dispositivo, você exerce esses direitos diretamente pelo app ou desinstalando-o.

9. ALTERAÇÕES NESTA POLÍTICA
Podemos atualizar esta política periodicamente. Alterações relevantes serão comunicadas dentro do próprio app. A data de "última atualização" no topo sempre reflete a versão vigente."""

private const val TERMS_OF_USE_TEXT = """Termos de Uso do PetIngle
Última atualização: Julho de 2026

1. ACEITAÇÃO DOS TERMOS
Ao instalar ou usar o PetIngle, você concorda com estes Termos de Uso. Se não concordar, não utilize o aplicativo.

2. DESCRIÇÃO DO SERVIÇO
O PetIngle é um aplicativo de organização pessoal para tutores de animais de estimação. Permite cadastrar pets, registrar histórico de saúde, criar lembretes e manter um diário fotográfico — tudo armazenado localmente no seu dispositivo.

3. PUBLICIDADE E DESBLOQUEIO DE PERFIS
O PetIngle é disponibilizado com anúncios exibidos pela plataforma Start.io. Os anúncios podem aparecer em espaços reservados na parte inferior das abas, sem impedir o uso das funções principais. Ao atingir o limite inicial de 10 perfis de pets, o usuário poderá assistir voluntariamente a um anúncio recompensado para liberar mais 5 perfis. Esse desbloqueio pode ser repetido, conforme a disponibilidade do anúncio.

4. NÃO SUBSTITUI VETERINÁRIO
As funcionalidades do PetIngle — incluindo campos de saúde, lembretes e registros — têm finalidade exclusivamente organizacional. O app não oferece diagnósticos, prescrições ou orientações médico-veterinárias. Consulte sempre um médico-veterinário habilitado para decisões sobre a saúde dos seus pets.

5. RESPONSABILIDADES DO USUÁRIO
• Você é responsável pela veracidade das informações cadastradas.
• Você é responsável por realizar backups regulares dos seus dados.
• Você concorda em usar o app somente para fins lícitos e pessoais.
• Não é permitido usar o app para fins comerciais sem autorização expressa.

6. PROPRIEDADE INTELECTUAL
O nome "PetIngle", o mascote, o design, os ícones, os textos e demais elementos visuais são propriedade exclusiva dos criadores do app. É vedada a reprodução, cópia ou uso comercial sem autorização prévia por escrito.

7. LIMITAÇÃO DE RESPONSABILIDADE
O PetIngle é fornecido "como está", sem garantias de disponibilidade ininterrupta ou ausência de erros. Não nos responsabilizamos por perdas de dados decorrentes de falhas no dispositivo, desinstalação do app ou ausência de backup.

8. MODIFICAÇÕES
Podemos alterar estes Termos a qualquer momento. O uso continuado do app após a publicação das alterações implica aceitação das novas condições. A data de "última atualização" no topo indica a versão vigente.

9. LEI APLICÁVEL
Estes Termos são regidos pelas leis da República Federativa do Brasil. Qualquer controvérsia será submetida ao foro da comarca do usuário, conforme o Código de Defesa do Consumidor."""

private const val ABOUT_TEXT = """Sobre o PetIngle

PetIngle é um aplicativo criado para ajudar tutores a cuidarem melhor dos seus pets — de forma simples, organizada e com carinho. O app é gratuito e pode exibir anúncios do Start.io; anúncios recompensados podem liberar perfis adicionais de pets.

Versão: 1.0.0"""
