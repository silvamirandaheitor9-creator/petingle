package br.com.petingle.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.petingle.data.db.dao.PetDao
import br.com.petingle.data.db.entity.Pet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para criar OU editar um pet (SPEC §11).
 * Quando petId > 0, carrega o pet existente; quando -1, cria um novo.
 * [photoPath] é compartilhado com PetPhotoEditorScreen via a mesma instância
 * escopada à rota do NavGraph.
 */
@HiltViewModel
class NewPetViewModel @Inject constructor(
    private val petDao: PetDao,
) : ViewModel() {

    /** Caminho da foto de perfil (recebe o resultado do PetPhotoEditorScreen). */
    private val _photoPath = MutableStateFlow<String?>(null)
    val photoPath: StateFlow<String?> = _photoPath

    /** Pet carregado para edição (null quando no modo criação). */
    private val _editingPet = MutableStateFlow<Pet?>(null)
    val editingPet: StateFlow<Pet?> = _editingPet

    /** Trava de duplo-toque — nunca resetada: a tela é destruída após salvar. */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    fun setPhotoPath(path: String?) {
        _photoPath.value = path
    }

    /**
     * Carrega um pet existente para pré-preencher o formulário de edição.
     * Chamado com LaunchedEffect(petId) na tela quando petId > 0.
     */
    fun loadPetForEditing(petId: Long) {
        if (petId <= 0L) return
        viewModelScope.launch {
            val pet = petDao.getPetByIdOnce(petId) ?: return@launch
            _editingPet.value = pet
            // Pré-carrega a foto existente (editável pelo usuário)
            if (_photoPath.value == null && pet.photoPath.isNotBlank()) {
                _photoPath.value = pet.photoPath
            }
        }
    }

    /** Salva um pet novo. */
    fun savePet(pet: Pet, onSaved: () -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            petDao.insertPet(pet)
            onSaved()
        }
    }

    /**
     * Atualiza um pet existente preservando o id e createdAt.
     * [existingPet] é o pet carregado do banco, garantindo que nenhum campo
     * imutável seja sobrescrito acidentalmente.
     */
    fun updatePet(existing: Pet, updated: Pet, onSaved: () -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            val merged = updated.copy(
                id        = existing.id,
                createdAt = existing.createdAt,
            )
            petDao.updatePet(merged)
            onSaved()
        }
    }
}
