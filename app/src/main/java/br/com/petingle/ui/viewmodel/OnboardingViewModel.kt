package br.com.petingle.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.petingle.data.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    // ── Seleção de tema ──────────────────────────────────────────────────────
    private val _selectedDark = MutableStateFlow(false)
    val selectedDark: StateFlow<Boolean> = _selectedDark.asStateFlow()

    fun selectTheme(dark: Boolean) {
        _selectedDark.value = dark
        viewModelScope.launch { prefs.setDarkTheme(dark) }
    }

    // ── Nome inserido no onboarding (página opcional) ─────────────────────────
    // Salvo em prefs apenas ao concluir o onboarding (completeOnboarding).
    private val _onboardingName = MutableStateFlow("")
    val onboardingName: StateFlow<String> = _onboardingName.asStateFlow()

    fun setOnboardingName(name: String) {
        _onboardingName.value = name
    }

    // ── Aceite dos termos ────────────────────────────────────────────────────
    private val _termsChecked = MutableStateFlow(false)
    val termsChecked: StateFlow<Boolean> = _termsChecked.asStateFlow()

    fun setTermsChecked(checked: Boolean) {
        _termsChecked.value = checked
    }

    // ── Conclusão do onboarding ──────────────────────────────────────────────
    fun completeOnboarding() {
        viewModelScope.launch {
            if (_onboardingName.value.isNotBlank()) {
                prefs.setUserName(_onboardingName.value.trim())
            }
            prefs.setOnboardingDone(true)
            prefs.setTermsAccepted(true)
        }
    }
}
