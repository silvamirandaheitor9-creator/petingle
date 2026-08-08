package br.com.petingle.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.petingle.data.db.dao.HealthRecordDao
import br.com.petingle.data.db.dao.PetDao
import br.com.petingle.data.db.entity.HealthRecord
import br.com.petingle.data.db.entity.Pet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel do detalhe do pet (SPEC §12 — Partes 1 e 2) ──────────────────
// Carrega o pet por ID e expõe as listas de registros de saúde por tipo.
// Cada Flow é convertido em StateFlow<ImmutableList> para consumo eficiente
// no Compose (compilador reconhece como estável e evita recomposições extras).

@HiltViewModel
class PetDetailViewModel @Inject constructor(
    private val petDao: PetDao,
    private val healthRecordDao: HealthRecordDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** ID do pet passado como argumento de navegação. */
    val petId: Long = checkNotNull(savedStateHandle["petId"])

    /** Dados completos do pet — null enquanto o banco carrega. */
    val pet: StateFlow<Pet?> = petDao.getPetById(petId)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Vacinas do pet ordenadas por data (mais recente primeiro). */
    val vaccines: StateFlow<ImmutableList<HealthRecord>> =
        healthRecordDao.getRecordsByPetAndType(petId, "vaccine")
            .map { it.toPersistentList() }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /** Medicamentos do pet ordenados por data (mais recente primeiro). */
    val medications: StateFlow<ImmutableList<HealthRecord>> =
        healthRecordDao.getRecordsByPetAndType(petId, "medication")
            .map { it.toPersistentList() }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /** Consultas do pet ordenadas por data (mais recente primeiro). */
    val consultations: StateFlow<ImmutableList<HealthRecord>> =
        healthRecordDao.getRecordsByPetAndType(petId, "consultation")
            .map { it.toPersistentList() }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /** Pesagens do pet — usadas para o gráfico e a lista da sub-aba Peso. */
    val weights: StateFlow<ImmutableList<HealthRecord>> =
        healthRecordDao.getRecordsByPetAndType(petId, "weight")
            .map { it.toPersistentList() }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /** Registros de alimentação do pet — sub-aba Alimentação (tipo, porção, horários). */
    val feedings: StateFlow<ImmutableList<HealthRecord>> =
        healthRecordDao.getRecordsByPetAndType(petId, "feeding")
            .map { it.toPersistentList() }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /**
     * Flag para evitar duplo-toque nos botões Salvar dos 5 formulários de saúde.
     * Setado/resetado na main thread antes/depois da coroutine, então é thread-safe
     * para o contexto de Compose (todos os cliques chegam na main thread).
     */
    private var savingInProgress = false

    /** Insere um novo registro de saúde no banco. */
    fun insertRecord(record: HealthRecord) {
        if (savingInProgress) return   // guard contra duplo-toque
        savingInProgress = true
        viewModelScope.launch {
            healthRecordDao.insertRecord(record)
            savingInProgress = false
        }
    }

    /** Remove um registro de saúde do banco. */
    fun deleteRecord(record: HealthRecord) {
        viewModelScope.launch { healthRecordDao.deleteRecord(record) }
    }

    /** Remove o pet e todos os seus dados do banco (Seção 13). */
    fun deletePet(pet: Pet) {
        viewModelScope.launch { petDao.deletePet(pet) }
    }
}
