package br.com.petingle.ui.viewmodel

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class ReminderGroup { HOJE, AMANHA, ESTA_SEMANA, HISTORICO }

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderDao: ReminderDao,
    private val petDao: PetDao,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    private val allReminders: StateFlow<ImmutableList<Reminder>> = reminderDao.getAllReminders()
        .map { it.toPersistentList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /** Lista de pets para o filtro de chips.
     *  ImmutableList → compilador do Compose reconhece como estável. */
    val pets: StateFlow<ImmutableList<Pet>> = petDao.getAllPets()
        .map { it.toPersistentList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    private val _selectedPetId = MutableStateFlow<Long?>(null)
    val selectedPetId: StateFlow<Long?> = _selectedPetId.asStateFlow()

    private val _historicoExpanded = MutableStateFlow(false)
    val historicoExpanded: StateFlow<Boolean> = _historicoExpanded.asStateFlow()

    /** Lembretes agrupados por período (Hoje / Amanhã / Esta semana / Histórico).
     *  Os valores de cada grupo são ImmutableList para evitar instabilidade no Compose. */
    val groupedReminders: StateFlow<Map<ReminderGroup, ImmutableList<Reminder>>> =
        combine(allReminders, _selectedPetId) { reminders, petId ->
            val filtered = if (petId == null) reminders else reminders.filter { it.petId == petId }
            groupByDate(filtered)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun selectPet(petId: Long?) { _selectedPetId.value = petId }

    fun toggleHistorico() { _historicoExpanded.value = !_historicoExpanded.value }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            // Cancela o alarme antes de excluir do banco
            scheduler.cancel(reminder)
            reminderDao.deleteReminder(reminder)
        }
    }

    fun toggleCompleted(reminder: Reminder) {
        val nowCompleted = !reminder.isCompleted
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.updateReminder(reminder.copy(isCompleted = nowCompleted))

            if (nowCompleted) {
                // Marcando como concluído → cancela o alarme futuro
                scheduler.cancel(reminder)
            } else {
                // Desmarcando → reagenda se ainda está no futuro
                val now = System.currentTimeMillis()
                if (reminder.dateTimeMillis > now) {
                    val pet: Pet? = petDao.getPetByIdOnce(reminder.petId)
                    scheduler.schedule(
                        reminder     = reminder.copy(isCompleted = false),
                        petName      = pet?.name ?: "",
                        petPhotoPath = pet?.photoPath ?: "",
                    )
                }
            }
        }
    }

    private fun groupByDate(reminders: List<Reminder>): Map<ReminderGroup, ImmutableList<Reminder>> {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfTomorrow        = startOfToday + 86_400_000L
        val startOfDayAfterTomorrow = startOfTomorrow + 86_400_000L

        val result = LinkedHashMap<ReminderGroup, MutableList<Reminder>>()
        result[ReminderGroup.HOJE]       = mutableListOf()
        result[ReminderGroup.AMANHA]     = mutableListOf()
        result[ReminderGroup.ESTA_SEMANA] = mutableListOf()
        result[ReminderGroup.HISTORICO]  = mutableListOf()

        for (r in reminders) {
            when {
                r.dateTimeMillis < startOfToday          -> result[ReminderGroup.HISTORICO]!!.add(r)
                r.dateTimeMillis < startOfTomorrow        -> result[ReminderGroup.HOJE]!!.add(r)
                r.dateTimeMillis < startOfDayAfterTomorrow -> result[ReminderGroup.AMANHA]!!.add(r)
                else                                      -> result[ReminderGroup.ESTA_SEMANA]!!.add(r)
            }
        }

        result[ReminderGroup.HISTORICO]!!.sortByDescending { it.dateTimeMillis }

        // Converte para ImmutableList antes de retornar — o Compose compiler trata como estável.
        return result
            .filter { it.value.isNotEmpty() }
            .mapValues { it.value.toPersistentList() }
    }
}
