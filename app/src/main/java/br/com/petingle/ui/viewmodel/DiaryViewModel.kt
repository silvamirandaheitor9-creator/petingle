package br.com.petingle.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.petingle.data.db.dao.DiaryDao
import br.com.petingle.data.db.dao.PetDao
import br.com.petingle.data.db.entity.DiaryEntry
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
class DiaryViewModel @Inject constructor(
    private val diaryDao: DiaryDao,
    private val petDao: PetDao,
) : ViewModel() {

    /** Todas as entradas do diário, mais recentes primeiro.
     *  ImmutableList → compilador do Compose reconhece como estável e
     *  evita recomposição de toda a lista quando apenas um campo muda. */
    val entries: StateFlow<ImmutableList<DiaryEntry>> = diaryDao.getAllEntries()
        .map { it.toPersistentList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /** Pets cadastrados — usados para o filtro por pet e para exibir o nome na timeline. */
    val pets: StateFlow<ImmutableList<Pet>> = petDao.getAllPets()
        .map { it.toPersistentList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /** Exclui uma entrada do diário. */
    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch { diaryDao.deleteEntry(entry) }
    }

    /** Cria uma nova entrada a partir da foto. */
    fun addEntry(petId: Long, photoPath: String, caption: String) {
        viewModelScope.launch {
            diaryDao.insertEntry(
                DiaryEntry(petId = petId, photoPath = photoPath, caption = caption.take(140)),
            )
        }
    }

    /** Atualiza legenda e/ou pet de uma entrada existente. */
    fun updateEntry(entry: DiaryEntry) {
        viewModelScope.launch { diaryDao.updateEntry(entry) }
    }

    /** Emite a entrada com o [id] especificado, ou null se não encontrada. */
    fun getEntryById(id: Long) = diaryDao.getEntryById(id)
}
