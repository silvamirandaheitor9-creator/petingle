package br.com.petingle.ui.screen.onboarding

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import br.com.petingle.ui.theme.OrangePrimary
import kotlinx.coroutines.delay

// ─── Model interno ────────────────────────────────────────────────────────────

private data class PermItem(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val isGranted: Boolean,
    val onRequest: () -> Unit,
)

// ─── Página de permissões (última tela do onboarding) ─────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsPage(isActive: Boolean) {

    // ── Estados de permissão (Accompanist) ──────────────────────────────────
    val cameraState = rememberPermissionState(Manifest.permission.CAMERA)

    val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val galleryState = rememberPermissionState(galleryPermission)

    // POST_NOTIFICATIONS só existe a partir do Android 13 (API 33)
    val notifState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    // ── Lista de itens ───────────────────────────────────────────────────────
    val items = remember(
        cameraState.status.isGranted,
        galleryState.status.isGranted,
        notifState?.status?.isGranted,
    ) {
        listOf(
            PermItem(
                icon      = Icons.Rounded.CameraAlt,
                title     = "Câmera",
                desc      = "Para fotografar seus pets diretamente pelo app",
                isGranted = cameraState.status.isGranted,
                onRequest = { cameraState.launchPermissionRequest() },
            ),
            PermItem(
                icon      = Icons.Rounded.Photo,
                title     = "Galeria de fotos",
                desc      = "Para escolher fotos dos pets da sua galeria",
                isGranted = galleryState.status.isGranted,
                onRequest = { galleryState.launchPermissionRequest() },
            ),
            PermItem(
                icon      = Icons.Rounded.NotificationsActive,
                title     = "Notificações",
                desc      = "Para receber lembretes de vacinas e consultas",
                isGranted = notifState?.status?.isGranted ?: true, // <API 33 não precisa pedir
                onRequest = { notifState?.launchPermissionRequest() },
            ),
        )
    }

    // ── Animação de entrada escalonada ───────────────────────────────────────
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive) {
            delay(120)
            visible = true
        } else {
            visible = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        // Ícone central
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(400)) + slideInVertically(tween(400)) { -30 },
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(36.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Título
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(450, delayMillis = 80)),
        ) {
            Text(
                text       = "Precisamos da sua autorização",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                textAlign  = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Subtítulo
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(450, delayMillis = 140)),
        ) {
            Text(
                text      = "O PetIngle precisa dessas permissões para funcionar bem. Você pode alterar isso nas configurações do celular a qualquer momento.",
                fontSize  = 13.sp,
                color     = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(28.dp))

        // Cards de permissão (entrada escalonada)
        items.forEachIndexed { index, item ->
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(400, delayMillis = 200 + index * 80)) +
                          slideInVertically(tween(400, delayMillis = 200 + index * 80)) { 24 },
            ) {
                PermissionCard(
                    item     = item,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (index < items.size - 1) Spacer(Modifier.height(10.dp))
        }
    }
}

// ─── Card individual de permissão ─────────────────────────────────────────────

@Composable
private fun PermissionCard(
    item: PermItem,
    modifier: Modifier = Modifier,
) {
    val bgAlpha by animateFloatAsState(
        targetValue   = if (item.isGranted) 0.28f else 0.15f,
        animationSpec = tween(300),
        label         = "perm_bg_${item.title}",
    )
    val buttonBg by animateColorAsState(
        targetValue   = if (item.isGranted) Color(0xFF4CAF50) else Color.White,
        animationSpec = tween(300),
        label         = "perm_btn_bg_${item.title}",
    )
    val buttonContent by animateColorAsState(
        targetValue   = if (item.isGranted) Color.White else OrangePrimary,
        animationSpec = tween(300),
        label         = "perm_btn_text_${item.title}",
    )
    val btnScale by animateFloatAsState(
        targetValue   = if (item.isGranted) 1.05f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "perm_btn_scale_${item.title}",
    )

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = bgAlpha)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Ícone circular
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = item.icon,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(22.dp),
                )
            }

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = item.title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
                Text(
                    text     = item.desc,
                    fontSize = 12.sp,
                    color    = Color.White.copy(alpha = 0.80f),
                )
            }

            // Botão Permitir / Check verde
            Button(
                onClick          = { if (!item.isGranted) item.onRequest() },
                modifier         = Modifier.scale(btnScale).height(36.dp),
                shape            = RoundedCornerShape(50),
                colors           = ButtonDefaults.buttonColors(
                    containerColor = buttonBg,
                    contentColor   = buttonContent,
                ),
                elevation        = ButtonDefaults.buttonElevation(0.dp),
                contentPadding   = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            ) {
                if (item.isGranted) {
                    Icon(
                        imageVector        = Icons.Rounded.Check,
                        contentDescription = "Concedida",
                        modifier           = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text       = "Permitir",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
