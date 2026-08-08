package br.com.petingle.data.db.dao

import androidx.room.*
import br.com.petingle.data.db.entity.HealthRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthRecordDao {
    @Query("SELECT * FROM health_records WHERE petId = :petId AND type = :type ORDER BY dateMillis DESC")
    fun getRecordsByPetAndType(petId: Long, type: String): Flow<List<HealthRecord>>

    @Query("SELECT * FROM health_records WHERE petId = :petId AND type = 'vaccine' ORDER BY dateMillis DESC LIMIT 1")
    fun getLatestVaccine(petId: Long): Flow<HealthRecord?>

    @Query("SELECT * FROM health_records WHERE petId = :petId AND type = 'consultation' ORDER BY dateMillis DESC LIMIT 1")
    fun getLatestConsultation(petId: Long): Flow<HealthRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HealthRecord): Long

    @Update
    suspend fun updateRecord(record: HealthRecord)

    @Delete
    suspend fun deleteRecord(record: HealthRecord)

    @Query("DELETE FROM health_records WHERE petId = :petId")
    suspend fun deleteAllByPet(petId: Long)

    /** Todos os registros de vacina que possuem nextDoseDate preenchido. */
    @Query("SELECT * FROM health_records WHERE type = 'vaccine' AND nextDoseDate != '' ORDER BY dateMillis ASC")
    fun getVaccinesWithNextDose(): Flow<List<HealthRecord>>

    /** Consultas agendadas no futuro (dateMillis > agora). */
    @Query("SELECT * FROM health_records WHERE type = 'consultation' AND dateMillis > :nowMillis ORDER BY dateMillis ASC")
    fun getUpcomingConsultations(nowMillis: Long): Flow<List<HealthRecord>>
}
