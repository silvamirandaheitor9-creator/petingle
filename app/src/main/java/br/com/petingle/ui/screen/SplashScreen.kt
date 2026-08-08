package br.com.petingle.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.petingle.R
import br.com.petingle.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    isReady: Boolean,
    onNavigate: () -> Unit,
) {
    // ── Animatables de entrada ──────────────────────────────────────
    val mascotScale  = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val dotsAlpha    = remember { Animatable(0f) }

    var animationDone by remember { mutableStateOf(false) }

    // ── Dots pulsando em cascata ────────────────────────────────────
    val dotsTransition = rememberInfiniteTransition(label = "dots")
    val dot1Alpha by dotsTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label         = "dot1",
    )
    val dot2Alpha by dotsTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse),
        label         = "dot2",
    )
    val dot3Alpha by dotsTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse),
        label         = "dot3",
    )

    // ── Sequência de entrada ────────────────────────────────────────
    LaunchedEffect(Unit) {
        // 1. Mascote com quique (spring)
        mascotScale.animateTo(
            targetValue   = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness    = Spring.StiffnessMedium,
            ),
        )

        // 2. Texto e dots em paralelo
        launch { contentAlpha.animateTo(1f, tween(400)) }
        dotsAlpha.animateTo(1f, tween(400))

        // Tempo mínimo de exibição
        kotlinx.coroutines.delay(1_200)
        animationDone = true
    }

    LaunchedEffect(animationDone, isReady) {
        if (animationDone && isReady) onNavigate()
    }

    // ── Layout ──────────────────────────────────────────────────────
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(OrangePrimary),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            // Mascote — apenas spring de entrada (sem float/sway)
            Image(
                painter            = painterResource(R.drawable.mascote_splash),
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        scaleX = mascotScale.value
                        scaleY = mascotScale.value
                    },
            )

            Spacer(Modifier.height(24.dp))

            // Nome do app
            Text(
                text       = "PetIngle",
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                modifier   = Modifier.graphicsLayer { alpha = contentAlpha.value },
            )

            Spacer(Modifier.height(8.dp))

            // Tagline
            Text(
                text      = "Cuidando dos seus pets com carinho",
                fontSize  = 13.sp,
                color     = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .padding(horizontal = 40.dp)
                    .graphicsLayer { alpha = contentAlpha.value },
            )

            Spacer(Modifier.height(40.dp))

            // 3 pontinhos em cascata
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.graphicsLayer { alpha = dotsAlpha.value },
            ) {
                listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { dotAlpha ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer { alpha = dotAlpha }
                            .background(Color.White, CircleShape),
                    )
                }
            }
        }
    }
}
