package br.com.petingle.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.petingle.data.datastore.UserPreferencesRepository
import br.com.petingle.data.db.dao.PetDao
import br.com.petingle.data.db.entity.Pet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PetsViewModel @Inject constructor(
    private val petDao: PetDao,
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    companion object {
        const val INITIAL_PET_LIMIT = 10
        const val BONUS_PET_SLOTS = 5
    }

    /** Lista completa de pets, ordenada por data de criação (mais recente primeiro). */
    val pets: StateFlow<ImmutableList<Pet>> = petDao.getAllPets()
        .map { it.toPersistentList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /** Contagem total de pets — usada no badge do título. */
    val petCount: StateFlow<Int> = petDao.getPetCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Limite persistente: 10 perfis iniciais + blocos de 5 desbloqueados. */
    val petLimit: StateFlow<Int> = prefs.bonusPetSlots
        .map { INITIAL_PET_LIMIT + it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), INITIAL_PET_LIMIT)

    fun unlockMorePets() {
        viewModelScope.launch {
            prefs.addBonusPetSlots(BONUS_PET_SLOTS)
        }
    }

    /**
     * Exclui um pet diretamente da lista (Meus Pets).
     */
    fun deletePetFromList(pet: Pet) {
        viewModelScope.launch { petDao.deletePet(pet) }
    }
}
