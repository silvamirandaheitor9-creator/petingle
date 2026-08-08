package br.com.petingle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.petingle.R

/**
 * FontFamily Nunito a partir de arquivos TTF locais em res/font/.
 *
 * Antes: GoogleFont.Provider + Font(googleFont = ...) × 5 pesos.
 *   → Fazia IPC para o Play Services (GMS font provider) no cold start,
 *     responsável por ~1300ms de atraso entre setContent e a composição
 *     do NavGraph.
 *
 * Depois: Font(R.font.*) × 5 — leitura de asset empacotado no APK.
 *   → Zero IPC, zero rede, inicialização praticamente instantânea.
 */
private val NunitoFontFamily: FontFamily by lazy {
    FontFamily(
        Font(R.font.nunito_regular,   FontWeight.Normal),
        Font(R.font.nunito_medium,    FontWeight.Medium),
        Font(R.font.nunito_semibold,  FontWeight.SemiBold),
        Font(R.font.nunito_bold,      FontWeight.Bold),
        Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    )
}

val PetIngleTypography: Typography by lazy {
    Typography(
        displayLarge   = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 57.sp),
        displayMedium  = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,      fontSize = 45.sp),
        displaySmall   = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,      fontSize = 36.sp),
        headlineLarge  = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,      fontSize = 32.sp),
        headlineMedium = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,      fontSize = 28.sp),
        headlineSmall  = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.SemiBold,  fontSize = 24.sp),
        titleLarge     = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,      fontSize = 22.sp),
        titleMedium    = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.SemiBold,  fontSize = 16.sp),
        titleSmall     = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.SemiBold,  fontSize = 14.sp),
        bodyLarge      = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Normal,    fontSize = 16.sp),
        bodyMedium     = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Normal,    fontSize = 14.sp),
        bodySmall      = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Normal,    fontSize = 12.sp),
        labelLarge     = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.SemiBold,  fontSize = 14.sp),
        labelMedium    = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Medium,    fontSize = 12.sp),
        labelSmall     = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Medium,    fontSize = 11.sp),
    )
}
