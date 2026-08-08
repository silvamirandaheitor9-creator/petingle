package br.com.petingle.ui.screen.onboarding

import androidx.annotation.DrawableRes

/**
 * Dados de uma página do onboarding.
 *
 * [imageRes] null apenas para as telas de tema, nome, termos e permissões.
 * [isThemePage] / [isNamePage] / [isTermsPage] / [isPermissionsPage] ativam layouts especiais.
 * [clipBottomDp] recorta o fundo da imagem em dp (útil para imagens com artefatos na borda inferior).
 */
data class OnboardingPageData(
    @DrawableRes val imageRes: Int?,
    val title: String,
    val subtitle: String,
    val isThemePage: Boolean = false,
    val isNamePage: Boolean = false,
    val isTermsPage: Boolean = false,
    val isPermissionsPage: Boolean = false,
    val clipBottomDp: Int = 0,
)
