package br.com.petingle.ui.screen.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.petingle.R
import br.com.petingle.ui.theme.OrangePrimary
import br.com.petingle.ui.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// ─── Dados das páginas ────────────────────────────────────────────────────────

private fun buildPages(): List<OnboardingPageData> = listOf(
    OnboardingPageData(
        imageRes = R.drawable.mascote_splash,
        title    = "Bem-vindo ao PetIngle!",
        subtitle = "Aqui começa uma nova forma de cuidar dos seus pets — com carinho, organização e muita alegria.",
    ),
    OnboardingPageData(
        imageRes = R.drawable.onboarding_1_boasvindas,
        title    = "Seus pets em um só lugar",
        subtitle = "Cadastre todos os seus companheiros, adicione fotos, registre a espécie, raça e data de nascimento. Organize tudo com carinho!",
    ),
    OnboardingPageData(
        imageRes     = R.drawable.onboarding_4_fotos,
        title        = "Guarde cada momento especial",
        subtitle     = "Fotos, memórias e histórias dos seus pets em um diário bonito, só para vocês.",
        clipBottomDp = 30,
    ),
    OnboardingPageData(
        imageRes = R.drawable.onboarding_3_lembretes,
        title    = "Nunca esqueça um cuidado",
        subtitle = "Vacinas, consultas e remédios — lembretes que chegam na hora certa, sem complicação.",
    ),
    OnboardingPageData(
        imageRes    = null,
        title       = "Escolha o seu estilo",
        subtitle    = "Você pode mudar quando quiser na aba Perfil.",
        isThemePage = true,
    ),
    OnboardingPageData(
        imageRes   = null,
        title      = "Como você se chama?",
        subtitle   = "Opcional — você pode preencher depois na aba Perfil.",
        isNamePage = true,
    ),
    OnboardingPageData(
        imageRes    = null,
        title       = "Antes de começar",
        subtitle    = "",
        isTermsPage = true,
    ),
    OnboardingPageData(
        imageRes          = null,
        title             = "Permissões",
        subtitle          = "",
        isPermissionsPage = true,
    ),
)

// ─── Tela principal ───────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pages            = remember { buildPages() }
    val totalPages       = pages.size
    val nameIndex        = pages.indexOfFirst { it.isNamePage }
    val termsIndex       = pages.indexOfFirst { it.isTermsPage }
    val permissionsIndex = pages.indexOfFirst { it.isPermissionsPage }

    val pagerState  = rememberPagerState(pageCount = { totalPages })
    val scope       = rememberCoroutineScope()
    val currentPage = pagerState.currentPage

    val selectedDark   by viewModel.selectedDark.collectAsState()
    val termsChecked   by viewModel.termsChecked.collectAsState()
    val onboardingName by viewModel.onboardingName.collectAsState()

    BackHandler(enabled = currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrangePrimary)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Pager ────────────────────────────────────────────────────────
            HorizontalPager(
                state                   = pagerState,
                modifier                = Modifier.weight(1f).fillMaxWidth(),
                beyondViewportPageCount = 1,
            ) { page ->
                val offset = ((pagerState.currentPage - page).toFloat() +
                    pagerState.currentPageOffsetFraction).absoluteValue
                val alpha = (1f - offset * 0.55f).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = alpha },
                ) {
                    when {
                        pages[page].isThemePage -> ThemeSelectionPage(
                            data         = pages[page],
                            selectedDark = selectedDark,
                            onSelect     = { viewModel.selectTheme(it) },
                        )
                        pages[page].isNamePage -> NameInputPage(
                            nameInput    = onboardingName,
                            onNameChange = { viewModel.setOnboardingName(it) },
                        )
                        pages[page].isTermsPage -> TermsPage(
                            isActive        = pagerState.currentPage == page,
                            checked         = termsChecked,
                            onCheckedChange = { viewModel.setTermsChecked(it) },
                        )
                        pages[page].isPermissionsPage -> PermissionsPage(
                            isActive = pagerState.currentPage == page,
                        )
                        else -> StandardOnboardingPage(data = pages[page])
                    }
                }
            }

            // ── Controles inferiores ─────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp, top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Pontinhos simples
                DotsIndicator(pageCount = totalPages, currentPage = currentPage)

                // Botão — rótulo e ação variam por página
                val isNamePage    = currentPage == nameIndex
                val isTerms       = currentPage == termsIndex
                val isPermissions = currentPage == permissionsIndex
                val btnLabel = when {
                    isTerms       -> "Aceitar e continuar"
                    isPermissions -> "Entrar no PetIngle!"
                    isNamePage    -> "Continuar"
                    else          -> "PRÓXIMO"
                }
                NextButton(
                    label   = btnLabel,
                    enabled = if (isTerms) termsChecked else true,
                    onClick = {
                        when {
                            isPermissions -> {
                                viewModel.completeOnboarding()
                                onFinished()
                            }
                            else -> {
                                scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                            }
                        }
                    },
                )

                // Botão "Pular" — só visível na página de nome
                if (isNamePage) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    }) {
                        Text(
                            text  = "Pular",
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

// ─── Página padrão — imagem no topo, textos brancos ──────────────────────────

@Composable
private fun StandardOnboardingPage(data: OnboardingPageData) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(0.5f))

        // Imagem / mascote
        data.imageRes?.let { res ->
            if (data.clipBottomDp > 0) {
                // Crop da borda inferior para ocultar artefatos na base da imagem
                Box(
                    modifier         = Modifier
                        .size(width = 220.dp, height = (220 - data.clipBottomDp).dp)
                        .clipToBounds(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Image(
                        painter            = painterResource(id = res),
                        contentDescription = data.title,
                        modifier           = Modifier.size(220.dp),
                        contentScale       = ContentScale.Fit,
                    )
                }
            } else {
                Image(
                    painter            = painterResource(id = res),
                    contentDescription = data.title,
                    modifier           = Modifier.size(220.dp),
                    contentScale       = ContentScale.Fit,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text       = data.title,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text       = data.subtitle,
            fontSize   = 14.sp,
            color      = Color.White.copy(alpha = 0.88f),
            textAlign  = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.weight(1f))
    }
}

// ─── Página de nome (opcional) ────────────────────────────────────────────────

@Composable
private fun NameInputPage(
    nameInput: String,
    onNameChange: (String) -> Unit,
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(0.4f))

        Box(
            modifier         = Modifier
                .size(80.dp)
                .background(Color.White.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Rounded.Person,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(40.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text       = "Como você se chama?",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text       = "Opcional — você pode preencher depois na aba Perfil.",
            fontSize   = 14.sp,
            color      = Color.White.copy(alpha = 0.80f),
            textAlign  = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value            = nameInput,
            onValueChange    = onNameChange,
            placeholder      = { Text("Seu nome", color = Color.White.copy(alpha = 0.55f)) },
            singleLine       = true,
            modifier         = Modifier.fillMaxWidth(),
            shape            = RoundedCornerShape(16.dp),
            colors           = OutlinedTextFieldDefaults.colors(
                focusedBorderColor        = Color.White,
                unfocusedBorderColor      = Color.White.copy(alpha = 0.45f),
                cursorColor               = Color.White,
                focusedTextColor          = Color.White,
                unfocusedTextColor        = Color.White,
                focusedPlaceholderColor   = Color.White.copy(alpha = 0.55f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.55f),
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction      = ImeAction.Done,
            ),
        )

        Spacer(Modifier.weight(1f))
    }
}

// ─── Tela de seleção de tema ──────────────────────────────────────────────────

private val LightBg     = Color(0xFFFFF8F3)
private val LightCard   = Color(0xFFFFFFFF)
private val LightOrange = Color(0xFFFF7A3D)

private val DarkBg      = Color(0xFF1E1A17)
private val DarkCard    = Color(0xFF2B2420)
private val DarkOrange  = Color(0xFFFF8C42)

@Composable
private fun ThemeSelectionPage(
    data: OnboardingPageData,
    selectedDark: Boolean,
    onSelect: (Boolean) -> Unit,
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text       = data.title,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            textAlign  = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = data.subtitle,
            fontSize  = 14.sp,
            color     = Color.White.copy(alpha = 0.88f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ThemePreviewCard(
                isDark     = false,
                isSelected = !selectedDark,
                onClick    = { onSelect(false) },
                modifier   = Modifier.weight(1f),
            )
            ThemePreviewCard(
                isDark     = true,
                isSelected = selectedDark,
                onClick    = { onSelect(true) },
                modifier   = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(
    isDark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.04f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "theme_card_scale_$isDark",
    )

    val bg           = if (isDark) DarkBg     else LightBg
    val card         = if (isDark) DarkCard   else LightCard
    val orange       = if (isDark) DarkOrange else LightOrange
    val txtPrimary   = if (isDark) Color.White else Color(0xFF1A120B)
    val txtSecondary = if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF6B4F3A)
    val icon         = if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode
    val label        = if (isDark) "Escuro" else "Claro"

    Column(
        modifier            = modifier
            .scale(scale)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.30f),
                shape = RoundedCornerShape(20.dp),
            )
            .background(bg, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    color = orange,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                ),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .height(32.dp)
                .background(card, RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.weight(0.55f).height(20.dp).background(card, RoundedCornerShape(6.dp)))
            Box(modifier = Modifier.weight(0.45f).height(20.dp).background(orange.copy(alpha = 0.35f), RoundedCornerShape(6.dp)))
        }
        Spacer(Modifier.height(12.dp))
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = if (isSelected) orange else txtSecondary,
            modifier           = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = label,
            fontSize   = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color      = if (isSelected) txtPrimary else txtSecondary,
        )
    }
}

// ─── Indicador de pontinhos ───────────────────────────────────────────────────

@Composable
private fun DotsIndicator(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { i ->
            val isActive = i == currentPage
            val size by animateFloatAsState(
                targetValue   = if (isActive) 10f else 7f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label         = "dot_size_$i",
            )
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .background(
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.38f),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

// ─── Botão PRÓXIMO ────────────────────────────────────────────────────────────

@Composable
private fun NextButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "next_btn_scale",
    )

    Button(
        onClick           = onClick,
        enabled           = enabled,
        interactionSource = interactionSource,
        modifier          = Modifier
            .scale(scale)
            .fillMaxWidth(0.75f)
            .height(52.dp),
        shape  = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor         = Color.White,
            contentColor           = OrangePrimary,
            disabledContainerColor = Color.White.copy(alpha = 0.35f),
            disabledContentColor   = Color.White.copy(alpha = 0.60f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation  = 0.dp,
            pressedElevation  = 0.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        Text(
            text          = label,
            fontWeight    = FontWeight.Bold,
            fontSize      = 15.sp,
            letterSpacing = 0.5.sp,
        )
    }
}
