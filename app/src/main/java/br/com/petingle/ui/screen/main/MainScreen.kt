package br.com.petingle.ui.screen.main

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.petingle.ui.theme.OrangeGradEnd
import br.com.petingle.ui.theme.OrangeGradStart
import br.com.petingle.ui.theme.OrangePrimary
import br.com.petingle.ui.viewmodel.HomeViewModel
import br.com.petingle.ui.viewmodel.PetsViewModel
import br.com.petingle.ui.viewmodel.ReminderViewModel
import com.startapp.sdk.ads.banner.Banner
import java.util.Calendar

// ─── Definição das 5 abas ────────────────────────────────────────────────────

private enum class MainTab(
    val label: String,
    val icon: ImageVector,
    /** Quando true, exibe o FAB "+" de ação. */
    val hasAddFab: Boolean,
) {
    HOME     (label = "Início",    icon = Icons.Rounded.Home,        hasAddFab = false),
    PETS     (label = "Meus Pets", icon = Icons.Rounded.Pets,        hasAddFab = true),
    DIARY    (label = "Diário",    icon = Icons.Rounded.AutoStories,  hasAddFab = true),
    REMINDERS(label = "Lembretes", icon = Icons.Rounded.Alarm,       hasAddFab = true),
    PROFILE  (label = "Perfil",    icon = Icons.Rounded.Person,      hasAddFab = false),
}

// ─── Tela principal ───────────────────────────────────────────────────────────

@Composable
fun MainScreen(
    onNavigateToDiaryPhotoEditor: (Uri) -> Unit = {},
    onNavigateToNewPet: () -> Unit = {},
    onNavigateToEditPet: (petId: Long) -> Unit = {},
    onNavigateToNewReminder: (reminderId: Long) -> Unit = {},
    onNavigateToPetDetail: (petId: Long) -> Unit = {},
    onNavigateToEditDiaryEntry: (entryId: Long) -> Unit = {},
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentTab = MainTab.entries[selectedTabIndex]

    // ── Saudação contextual para a aba Início ────────────────────────────────
    val homeViewModel: HomeViewModel = hiltViewModel()
    val userName by homeViewModel.userName.collectAsState()

    // ── Badge da aba Meus Pets ───────────────────────────────────────────────
    val petsViewModel: PetsViewModel = hiltViewModel()
    val petCount by petsViewModel.petCount.collectAsState()
    val petLimit by petsViewModel.petLimit.collectAsState()
    val context = LocalContext.current
    var showPetLimitDialog by remember { mutableStateOf(false) }
    val activity = context.findActivity()
    val rewardedAdController = remember(activity) {
        activity?.let {
            RewardedAdController(
                activity = it,
                onReward = {
                    petsViewModel.unlockMorePets()
                    showPetLimitDialog = false
                },
            )
        }
    }
    val preparedRewardedAd = rewardedAdController?.preparedAd
    val isRewardedAdLoading = rewardedAdController?.isLoading ?: false
    val rewardedAdUnavailable = rewardedAdController?.unavailable ?: true
    val rewardedAdError = rewardedAdController?.lastErrorMessage

    // Deixa o anúncio pronto antes de o usuário chegar ao limite. Assim o
    // diálogo não fica preso em uma primeira tentativa de carregamento.
    LaunchedEffect(rewardedAdController) {
        rewardedAdController?.load()
    }
    DisposableEffect(rewardedAdController) {
        onDispose {
            rewardedAdController?.dispose()
        }
    }

    // ── ViewModel de Lembretes ────────────────────────────────────────────────
    val reminderViewModel: ReminderViewModel = hiltViewModel()

    // ── Trigger do seletor de galeria da aba Diário ───────────────────────────
    var showAddDiaryEntry by remember { mutableStateOf(false) }

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(userName, hour) {
        val name = userName.trim()
        when {
            hour < 12 -> if (name.isNotBlank()) "Bom dia, $name! ☀️" else "Bom dia! ☀️"
            hour < 18 -> if (name.isNotBlank()) "Boa tarde, $name! 🌤️" else "Boa tarde! 🌤️"
            else      -> if (name.isNotBlank()) "Boa noite, $name! 🌙" else "Boa noite! 🌙"
        }
    }
    val warmPhrase = remember(hour) {
        when {
            hour < 12 -> "Como estão os pets hoje?"
            hour < 18 -> "Tudo bem com os seus bichinhos?"
            else      -> "Como foi o dia dos seus pets?"
        }
    }

    Scaffold(
        topBar = {
            PetIngleTopBar(
                title    = if (selectedTabIndex == 0) greeting else currentTab.label,
                subtitle = if (selectedTabIndex == 0) warmPhrase else null,
                badge    = if (currentTab == MainTab.PETS) "$petCount de $petLimit" else null,
            )
        },
        bottomBar = {
            Column {
                StartIoBanner()
                PetIngleBottomBar(
                    tabs          = MainTab.entries,
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ── Conteúdo da aba com crossfade suave ao trocar ─────────────────
            AnimatedContent(
                targetState   = currentTab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(140))
                },
                label = "tab_content",
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
                when (tab) {
                    MainTab.HOME ->
                        HomeScreen(
                            viewModel = homeViewModel,
                            onAddPet  = { selectedTabIndex = MainTab.PETS.ordinal },
                        )
                    MainTab.PETS ->
                        PetsScreen(
                            viewModel  = petsViewModel,
                            onPetClick = onNavigateToPetDetail,
                            onEditPet  = onNavigateToEditPet,
                        )
                    MainTab.DIARY ->
                        DiaryScreen(
                            showAddEntryPlaceholder      = showAddDiaryEntry,
                            onDismissAddEntryPlaceholder = { showAddDiaryEntry = false },
                            onNavigateToPhotoEditor      = onNavigateToDiaryPhotoEditor,
                            onEditEntry                  = onNavigateToEditDiaryEntry,
                        )
                    MainTab.REMINDERS ->
                        RemindersScreen(
                            viewModel               = reminderViewModel,
                            onNavigateToNewReminder = { id -> onNavigateToNewReminder(id) },
                        )
                    MainTab.PROFILE ->
                        ProfileScreen()
                }
            }

            // ── FAB "+" de ação (apenas abas com ação de adicionar) ──────────
            if (currentTab.hasAddFab) {
                AddFab(
                    contentDescription = "Adicionar ${currentTab.label}",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                    onClick = {
                        when (currentTab) {
                            MainTab.PETS      -> {
                                if (petCount < petLimit) {
                                    onNavigateToNewPet()
                                } else {
                                    rewardedAdController?.clearUnavailable()
                                    showPetLimitDialog = true
                                }
                            }
                            MainTab.DIARY     -> showAddDiaryEntry = true
                            MainTab.REMINDERS -> onNavigateToNewReminder(-1L)
                            else -> {}
                        }
                    },
                )
            }
        }
    }

    if (showPetLimitDialog) {
        AlertDialog(
            onDismissRequest = { showPetLimitDialog = false },
            title = { Text("Limite de perfis atingido") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Você já cadastrou $petLimit pets. Assista a um anúncio curto para desbloquear mais ${PetsViewModel.BONUS_PET_SLOTS} perfis.",
                    )
                    if (isRewardedAdLoading) {
                        Text(
                            "Preparando anúncio...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        )
                    } else if (rewardedAdUnavailable) {
                        Text(
                            "Ainda não encontramos uma oferta agora. Toque em tentar novamente para fazer uma nova busca.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        rewardedAdError?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                            Text(
                                "Detalhe técnico: $errorMessage",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (preparedRewardedAd == null) {
                            rewardedAdController?.load()
                            return@Button
                        }
                        rewardedAdController?.show()
                    },
                    enabled = !isRewardedAdLoading,
                ) {
                    Text(
                        when {
                            isRewardedAdLoading -> "Aguarde..."
                            preparedRewardedAd == null && rewardedAdUnavailable -> "Tentar novamente"
                            preparedRewardedAd == null -> "Preparar anúncio"
                            else -> "Assistir anúncio"
                        },
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (!isRewardedAdLoading) {
                            showPetLimitDialog = false
                        }
                    },
                    enabled = !isRewardedAdLoading,
                ) {
                    Text("Agora não")
                }
            },
        )
    }
}

/**
 * Banner padrão do Start.io compartilhado pela navegação principal.
 *
 * Ele fica em uma faixa própria, antes da NavigationBar, para que o Scaffold
 * reserve o espaço automaticamente e nenhum conteúdo ou ação fique encoberto.
 * A altura fixa evita que o anúncio altere o layout quando carregar ou atualizar.
 */
@Composable
private fun StartIoBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            factory = { context -> Banner(context) },
            update = { banner ->
                // O SDK mantém o carregamento e a atualização do anúncio.
                // Não chamamos loadAd() aqui: esse bloco permanece montado ao
                // trocar de aba e não dispara requisições a cada recomposição.
                banner.contentDescription = "Anúncio"
            },
        )
    }
}

// ─── Cabeçalho: gradiente laranja + título + subtitle opcional ────────────────

@Composable
private fun PetIngleTopBar(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(OrangeGradStart, OrangeGradEnd)),
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                horizontal = 20.dp,
                vertical   = if (subtitle != null) 10.dp else 16.dp,
            ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment      = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
                if (!badge.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text       = badge,
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White,
                        )
                    }
                }
            }
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                )
            }
        }
    }
}

// ─── Barra de navegação inferior com animação "pulo" ─────────────────────────

@Composable
private fun PetIngleBottomBar(
    tabs: List<MainTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor   = OrangePrimary,
        tonalElevation = 2.dp,
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedIndex == index

            NavigationBarItem(
                selected = isSelected,
                onClick  = { onTabSelected(index) },
                icon     = {
                    Icon(
                        imageVector        = tab.icon,
                        contentDescription = tab.label,
                        modifier           = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(
                        text  = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = OrangePrimary,
                    selectedTextColor   = OrangePrimary,
                    indicatorColor      = OrangePrimary.copy(alpha = 0.10f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                ),
            )
        }
    }
}

// ─── FAB "+" de ação com pulso suave ─────────────────────────────────────────

@Composable
private fun AddFab(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val fabScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fab_scale",
    )

    SmallFloatingActionButton(
        onClick        = onClick,
        containerColor = OrangePrimary,
        contentColor   = Color.White,
        elevation      = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp,
            pressedElevation = 1.dp,
        ),
        modifier = modifier
            .size(48.dp)
            .scale(fabScale),
    ) {
        Icon(
            imageVector        = Icons.Rounded.Add,
            contentDescription = contentDescription,
            modifier           = Modifier.size(24.dp),
        )
    }
}
