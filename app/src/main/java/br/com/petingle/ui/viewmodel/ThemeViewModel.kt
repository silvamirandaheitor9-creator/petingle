package br.com.petingle.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.petingle.data.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    val isDarkTheme = prefs.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch { prefs.setDarkTheme(dark) }
    }
}
