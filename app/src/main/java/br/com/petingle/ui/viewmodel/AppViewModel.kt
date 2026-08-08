package br.com.petingle.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.petingle.data.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    val isOnboardingDone: StateFlow<Boolean> = prefs.isOnboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    init {
        viewModelScope.launch {
            // Aguarda DataStore estar pronto (seção 4: navegação só ocorre quando
            // animação mínima terminar E carregamento real concluir)
            prefs.isOnboardingDone.collect {
                _isReady.value = true
            }
        }
    }
}
