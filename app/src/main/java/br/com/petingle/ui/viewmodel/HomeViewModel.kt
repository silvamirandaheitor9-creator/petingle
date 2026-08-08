package br.com.petingle.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.petingle.data.datastore.UserPreferencesRepository
import br.com.petingle.data.db.dao.HealthRecordDao
import br.com.petingle.data.db.dao.PetDao
import br.com.petingle.data.db.dao.ReminderDao
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.data.db.entity.Reminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val petDao: PetDao,
    private val reminderDao: ReminderDao,
    private val healthRecordDao: HealthRecordDao,
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    /** Lista completa de pets, mais recentes primeiro. */
    val pets: StateFlow<ImmutableList<Pet>> = petDao.getAllPets()
        .map { it.toPersistentList() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /** Contagem total de pets cadastrados. */
    val petCount: StateFlow<Int> = petDao.getPetCount()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Nome do usuário salvo no DataStore. */
    val userName: StateFlow<String> = prefs.userName
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /**
     * Data da próxima VACINA — combina:
     * 1. Lembretes com category == "vacina"  →  dateTimeMillis armazenado em fuso local
     * 2. nextDoseDate de HealthRecords tipo "vaccine"  →  string "dd/MM/yyyy" representando UTC midnight
     *
     * ⚠ Bug histórico: o DatePicker do Material3 sempre retorna UTC midnight. Datas de registros
     * de saúde ficam salvas como UTC midnight. Formatar esses millis no fuso local (UTC-3) resultaria
     * no dia anterior (21h do dia anterior = "24/07" quando deveria ser "25/07").
     * Correção: health records → format em UTC; lembretes → format em fuso local.
     */
    val nextVaccineDate: StateFlow<String?> = combine(
        reminderDao.getPendingReminders(),
        healthRecordDao.getVaccinesWithNextDose(),
    ) { reminders, vaccineRecords ->
        val now = System.currentTimeMillis()

        // Lembretes: dateTimeMillis é em fuso local (ex.: 25/07 às 10h BRT → correto)
        val bestReminder: Reminder? = reminders
            .filter { it.category == "vacina" && it.dateTimeMillis > now }
            .minByOrNull { it.dateTimeMillis }

        // Registros: nextDoseDate = "dd/MM/yyyy" representando uma data em UTC → parse em UTC
        val bestRecordMillis: Long? = vaccineRecords
            .mapNotNull { parseDisplayDateToUtcMillis(it.nextDoseDate) }
            .filter { it > now }
            .minOrNull()

        // Escolhe o mais próximo e usa o formatter correto para cada fonte
        when {
            bestReminder == null && bestRecordMillis == null -> null
            bestReminder == null -> bestRecordMillis!!.toShortDateUtc()
            bestRecordMillis == null -> bestReminder.dateTimeMillis.toShortDateLocal()
            bestReminder.dateTimeMillis <= bestRecordMillis -> bestReminder.dateTimeMillis.toShortDateLocal()
            else -> bestRecordMillis.toShortDateUtc()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Data da próxima CONSULTA — combina:
     * 1. Lembretes com category == "consulta"  →  dateTimeMillis em fuso local
     * 2. HealthRecords tipo "consultation" com data futura  →  dateMillis em UTC midnight
     *
     * Mesma correção de timezone aplicada: health records formatados em UTC.
     */
    val nextConsultDate: StateFlow<String?> = combine(
        reminderDao.getPendingReminders(),
        healthRecordDao.getUpcomingConsultations(utcMidnightToday()),
    ) { reminders, consultRecords ->
        val now = System.currentTimeMillis()

        // Lembretes: fuso local
        val bestReminder: Reminder? = reminders
            .filter { it.category == "consulta" && it.dateTimeMillis > now }
            .minByOrNull { it.dateTimeMillis }

        // Registros de consulta: dateMillis = UTC midnight → exibir em UTC
        val bestRecordMillis: Long? = consultRecords
            .minByOrNull { it.dateMillis }
            ?.dateMillis

        when {
            bestReminder == null && bestRecordMillis == null -> null
            bestReminder == null -> bestRecordMillis!!.toShortDateUtc()
            bestRecordMillis == null -> bestReminder.dateTimeMillis.toShortDateLocal()
            bestReminder.dateTimeMillis <= bestRecordMillis -> bestReminder.dateTimeMillis.toShortDateLocal()
            else -> bestRecordMillis.toShortDateUtc()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

// ─── Helpers privados ──────────────────────────────────────────────────────────

/**
 * Retorna o timestamp UTC da meia-noite de hoje no fuso local.
 * Usado como limiar para filtrar consultas futuras no DAO.
 * Assim uma consulta cadastrada "para hoje" ainda aparece durante o dia.
 */
private fun utcMidnightToday(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR,         local.get(Calendar.YEAR))
        set(Calendar.MONTH,        local.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY,  0)
        set(Calendar.MINUTE,       0)
        set(Calendar.SECOND,       0)
        set(Calendar.MILLISECOND,  0)
    }.timeInMillis
}

/**
 * Analisa uma string "dd/MM/yyyy" armazenada por [DateUtils.utcMillisToDisplayDate]
 * (que representa uma data em UTC) de volta para UTC midnight millis.
 *
 * NÃO use SimpleDateFormat sem timezone aqui: em fusos negativos a data ficaria errada.
 */
private fun parseDisplayDateToUtcMillis(dateStr: String): Long? {
    if (dateStr.isBlank()) return null
    val parts = dateStr.split("/")
    if (parts.size != 3) return null
    val day   = parts[0].toIntOrNull() ?: return null
    val month = (parts[1].toIntOrNull() ?: return null) - 1 // Calendar é 0-based
    val year  = parts[2].toIntOrNull() ?: return null
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR,         year)
        set(Calendar.MONTH,        month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY,  0)
        set(Calendar.MINUTE,       0)
        set(Calendar.SECOND,       0)
        set(Calendar.MILLISECOND,  0)
    }.timeInMillis
}

/**
 * Formata UTC midnight millis (health records) como "dd/MM" lendo os campos no fuso UTC.
 * NÃO use SimpleDateFormat sem timezone — em UTC-3 a meia-noite UTC seria interpretada
 * como 21h do dia anterior e mostraria a data errada.
 */
private fun Long.toShortDateUtc(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = this@toShortDateUtc }
    return String.format(
        Locale("pt", "BR"),
        "%02d/%02d",
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1,
    )
}

/**
 * Formata millis de lembrete (armazenados em fuso local via [DateUtils.utcMillisToLocalPreservingTime])
 * como "dd/MM" usando o fuso do dispositivo — correto para reminders.
 */
private fun Long.toShortDateLocal(): String =
    SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(Date(this))
