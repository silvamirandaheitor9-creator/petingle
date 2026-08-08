package br.com.petingle.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.petingle.data.db.dao.PetDao
import br.com.petingle.data.db.dao.ReminderDao
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.data.db.entity.Reminder
import br.com.petingle.data.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class NewReminderViewModel @Inject constructor(
    private val reminderDao: ReminderDao,
    private val petDao: PetDao,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    /** Lista de pets para o dropdown do formulário.
     *  ImmutableList → compilador do Compose reconhece como estável. */
    val pets: StateFlow<ImmutableList<Pet>> = petDao.getAllPets()
        .map { it.toPersistentList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    var editingReminderId: Long by mutableLongStateOf(-1L)
        private set

    var title by mutableStateOf("")
    var selectedPetId by mutableLongStateOf(-1L)
    var category by mutableStateOf("vacina")
    var dateTimeMillis by mutableLongStateOf(defaultDateTimeMillis())
    var recurrence by mutableStateOf("none")
    var notes by mutableStateOf("")

    var isSaving by mutableStateOf(false)
        private set

    var saveError by mutableStateOf<String?>(null)
        private set

    // Auto-seleciona o primeiro pet assim que a lista carregar do banco,
    // sem depender de LaunchedEffect no composable (que é assíncrono e pode
    // deixar selectedPetId = -1 antes de o usuário tocar em Salvar).
    init {
        viewModelScope.launch {
            val petList = pets.first { it.isNotEmpty() }
            if (selectedPetId <= 0L) {
                selectedPetId = petList.first().id
            }
        }
    }

    val isValid: Boolean
        get() = title.isNotBlank() && selectedPetId > 0

    fun clearError() { saveError = null }

    fun loadReminder(reminderId: Long) {
        if (reminderId <= 0L || editingReminderId == reminderId) return
        editingReminderId = reminderId
        viewModelScope.launch {
            val r = reminderDao.getReminderById(reminderId) ?: return@launch
            title = r.title
            selectedPetId = r.petId
            category = r.category
            dateTimeMillis = r.dateTimeMillis
            recurrence = r.recurrence
            notes = r.notes
        }
    }

    fun saveReminder(onDone: () -> Unit) {
        if (!isValid || isSaving) return
        isSaving = true
        saveError = null

        viewModelScope.launch {
            try {
                val pet: Pet? = withContext(Dispatchers.IO) { petDao.getPetByIdOnce(selectedPetId) }
                val now = System.currentTimeMillis()

                if (editingReminderId > 0) {
                    // ── Edição ───────────────────────────────────────────────
                    val existing = reminderDao.getReminderById(editingReminderId) ?: return@launch
                    // Cancela alarme anterior antes de reagendar
                    scheduler.cancel(existing)

                    val updated = existing.copy(
                        title         = title.trim(),
                        petId         = selectedPetId,
                        category      = category,
                        dateTimeMillis = dateTimeMillis,
                        recurrence    = recurrence,
                        notes         = notes.trim(),
                    )
                    reminderDao.updateReminder(updated)

                    if (updated.dateTimeMillis > now && !updated.isCompleted) {
                        scheduler.schedule(
                            reminder      = updated,
                            petName       = pet?.name ?: "",
                            petPhotoPath  = pet?.photoPath ?: "",
                        )
                    }
                } else {
                    // ── Criação ──────────────────────────────────────────────
                    val newId = reminderDao.insertReminder(
                        Reminder(
                            title          = title.trim(),
                            petId          = selectedPetId,
                            category       = category,
                            dateTimeMillis = dateTimeMillis,
                            recurrence     = recurrence,
                            notes          = notes.trim(),
                            notificationId = 0,
                        )
                    )
                    // Atualiza o notificationId para corresponder ao ID gerado
                    val saved = reminderDao.getReminderById(newId)
                    if (saved != null) {
                        val withNotifId = saved.copy(notificationId = newId.toInt())
                        reminderDao.updateReminder(withNotifId)

                        if (withNotifId.dateTimeMillis > now) {
                            scheduler.schedule(
                                reminder     = withNotifId,
                                petName      = pet?.name ?: "",
                                petPhotoPath = pet?.photoPath ?: "",
                            )
                        }
                    }
                }

                onDone()
            } catch (e: Exception) {
                saveError = "Erro ao salvar: ${e.localizedMessage ?: "tente novamente"}"
            } finally {
                isSaving = false
            }
        }
    }

    private fun defaultDateTimeMillis(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, 1)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
